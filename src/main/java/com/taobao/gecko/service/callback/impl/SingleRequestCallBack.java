/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.taobao.gecko.service.callback.impl;

import com.taobao.gecko.service.callback.AbstractRequestCallBack;
import com.taobao.gecko.service.callback.SingleRequestCallBackListener;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.service.connection.Connection;
import com.taobao.gecko.exception.NotifyRemotingException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 针对单个连接或者单个分组的请求回调
 *
 * @author boyan
 * @since 1.0, 2009-12-15 下午04:04:2
 */
public final class SingleRequestCallBack extends AbstractRequestCallBack {

    /** 请求ID */
    private final CommandHeader requestCommandHeader;
    /** 服务端的响应对象 */
    private ResponseCommand responseCommand;
    /** 回调监听 */
    private SingleRequestCallBackListener requestCallBackListener;
    /** 服务调用异常时的异常堆栈 */
    private Exception exception;
    /** 表示服务端是否已响应 */
    private boolean responsed = false;


    public SingleRequestCallBack(final CommandHeader requestCommandHeader, final long timeout) {
        super(new CountDownLatch(1), timeout, System.currentTimeMillis());
        this.requestCommandHeader = requestCommandHeader;
    }
    public SingleRequestCallBack(final CommandHeader requestCommandHeader, final long timeout, final SingleRequestCallBackListener requestCallBackListener) {
        super(new CountDownLatch(1), timeout, System.currentTimeMillis());
        this.requestCommandHeader = requestCommandHeader;
        this.requestCallBackListener = requestCallBackListener;
    }





    public Exception getException() {
        return this.exception;
    }
    @Override
    public void setException0(final Exception exception, final Connection conn, final RequestCommand requestCommand) {
        this.exception = exception;
        this.countDownLatch();
        if (this.tryComplete()) {
            if (this.requestCallBackListener != null) {
                this.requestCallBackListener.onException(exception);
            }
        }
    }

    @Override
    public boolean isComplete() {
        return this.responsed;
    }
    @Override
    public void complete() {
        this.responsed = true;
    }

    /**
     * 当响应到达时触发此方法，留给子类扩展
     *
     * @param group
     * @param responseCommand
     * @param connection
     */
    @Override
    public void onResponse0(final String group, final ResponseCommand responseCommand, final Connection connection) {
        synchronized (this) {
            if (this.responseCommand == null) {
                this.responseCommand = responseCommand;
            } else {
                return;// 已经有应答了
            }
        }

        // countDownLatch - 1，当countDownLatch为0时，不再阻塞
        this.countDownLatch();
        if (this.tryComplete()) {
            if (this.requestCallBackListener != null) {
                if (this.requestCallBackListener.getExecutor() != null) {
                    this.requestCallBackListener.getExecutor().execute(new Runnable() {
                        public void run() {
                            SingleRequestCallBack.this.requestCallBackListener.onResponse(responseCommand, connection);
                        }
                    });
                } else {
                    this.requestCallBackListener.onResponse(responseCommand, connection);
                }
            }
        }
    }


    /**
     * 返回响应对象
     *
     * @param time
     * @param timeUnit
     * @param conn
     * @return
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws NotifyRemotingException
     */
    public ResponseCommand getResult(final long time, final TimeUnit timeUnit, final Connection conn) throws InterruptedException, TimeoutException, NotifyRemotingException {
        // 如果请求一直没响应，将一直阻塞，直到收到响应或者异常或者超时
        if (!this.await(time, timeUnit)) {
            // 当收到响应后，将连接从#writeFutureMap移除，并设置future不可中断
            this.cancelWrite(conn);
            // 切记移除回调
            this.removeCallBackFromConnection(conn, this.requestCommandHeader.getOpaque());
            throw new TimeoutException("Operation timeout");
        }

        if (this.exception != null) {
            // 当收到响应后，将连接从#writeFutureMap移除，并设置future不可中断
            this.cancelWrite(conn);
            // 切记移除回调
            this.removeCallBackFromConnection(conn, this.requestCommandHeader.getOpaque());
            throw new NotifyRemotingException("同步调用失败", this.exception);
        }

        synchronized (this) {
            return this.responseCommand;
        }
    }
    public ResponseCommand getResult() throws InterruptedException, TimeoutException, NotifyRemotingException {
        if (!this.await(1000, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("Operation timeout(1 second)");
        }
        if (this.exception != null) {
            throw new NotifyRemotingException("同步调用失败", this.exception);
        }

        synchronized (this) {
            return this.responseCommand;
        }
    }


    public CommandHeader getRequestCommandHeader() {
        return this.requestCommandHeader;
    }





}