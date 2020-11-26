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
package com.taobao.gecko.service.callback;

import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.taobao.gecko.core.nio.impl.TimerRef;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.impl.DefaultConnection;
import com.taobao.gecko.service.impl.RequestCallBack;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 回调基类
 *
 * @author boyan
 * @since 1.0, 2009-12-18 下午04:09:25
 */
public abstract class AbstractRequestCallBack implements RequestCallBack {

    /** 服务调用的超时时间，单位毫秒 */
    private final long timeout;
    /** 回调创建的时间戳 */
    private final long timestamp;
    /** 定时器引用 */
    private TimerRef timerRef;
    /** 请求计数 */
    private final CountDownLatch countDownLatch;

    private final ConcurrentHashMap<Connection, Future<Boolean>> writeFutureMap = new ConcurrentHashMap<Connection, Future<Boolean>>();

    /** 防止重复响应的锁 */
    protected final Lock responseLock = new ReentrantLock();


    public AbstractRequestCallBack(final CountDownLatch countDownLatch, final long timeout, final long timestamp) {
        super();
        this.countDownLatch = countDownLatch;
        this.timeout = timeout;
        this.timestamp = timestamp;
    }


    /**
     * 判断回调是否过期，当当前时间 - 服务响应 > 请求调用的超时时间时，该回调无效
     *
     * @param now 当前时间
     * @return
     */
    public boolean isInvalid(final long now) {
        return this.timeout <= 0 || now - this.timestamp > this.timeout;
    }

    /**
     * 当响应到达时触发此方法
     *
     * @param group           应答的分组名
     * @param responseCommand 应答命令
     * @param connection      应答的连接
     */
    public void onResponse(final String group, final ResponseCommand responseCommand, final Connection connection) {
        if (responseCommand != null) {
            this.removeCallBackFromConnection(connection, responseCommand.getOpaque());
        }
        this.onResponse0(group, responseCommand, connection);
    }

    /**
     * 当响应到达时触发此方法，留给子类扩展
     *
     * @param group
     * @param responseCommand
     * @param connection
     */
    public abstract void onResponse0(String group, ResponseCommand responseCommand, Connection connection);

    /**
     * 设置异常
     *
     * @param e
     * @param conn
     * @param requestCommand
     */
    public void setException(final Exception e, final Connection conn, final RequestCommand requestCommand) {
        if (requestCommand != null) {
            this.removeCallBackFromConnection(conn, requestCommand.getOpaque());
        }
        this.setException0(e, conn, requestCommand);
    }


    /**
     * 当收到响应后，将连接从#writeFutureMap移除，并设置future不可中断
     *
     * @param conn
     */
    public void cancelWrite(final Connection conn) {
        if (conn == null) {
            return;
        }
        final Future<Boolean> future = this.writeFutureMap.remove(conn);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void addWriteFuture(final Connection conn, final Future<Boolean> future) {
        this.writeFutureMap.put(conn, future);
    }

    /**
     * countDownLatch - 1，当countDownLatch为0时，不再阻塞
     */
    public void countDownLatch() {
        this.responseLock.lock();
        try {
            this.countDownLatch.countDown();
        } finally {
            this.responseLock.unlock();
        }
    }

    /**
     * 如果请求一直没响应，countDownLatch将一直阻塞，直到收到响应或者异常或者超时
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    public boolean await(final long timeout, final TimeUnit unit) throws InterruptedException {
        return this.countDownLatch.await(timeout, unit);
    }

    /**
     * 取消定时器
     */
    public void cancelTimer() {
        if (this.timerRef != null) {
            this.timerRef.cancel();
        }
    }

    public void setTimerRef(final TimerRef timerRef) {
        this.timerRef = timerRef;
    }

    protected static final BooleanAckCommand createComunicationErrorResponseCommand(final Connection conn, final Exception e, final RequestCommand requestCommand, final InetSocketAddress address) {
        final StringBuilder sb = new StringBuilder(e.getMessage());
        if (e.getCause() != null) {
            sb.append("\r\nRroot cause by:\r\n").append(e.getCause().getMessage());
        }
        final BooleanAckCommand value =
                conn.getRemotingContext()
                        .getCommandFactory()
                        .createBooleanAckCommand(requestCommand.getRequestHeader(), ResponseStatus.ERROR_COMM,
                                sb.toString());
        value.setResponseStatus(ResponseStatus.ERROR_COMM);
        value.setResponseTime(System.currentTimeMillis());
        value.setResponseHost(address);
        return value;
    }



    public abstract void setException0(Exception e, Connection conn, RequestCommand requestCommand);


    protected void removeCallBackFromConnection(final Connection conn, final Integer opaque) {
        if (conn != null) {
            ((DefaultConnection) conn).removeRequestCallBack(opaque);
        }
    }

    /**
     * 请求是否完成
     *
     * @return
     */
    public abstract boolean isComplete();

    /**
     * 标记请求完成
     */
    public abstract void complete();

    /**
     * 尝试完成请求
     *
     * @return
     */
    public boolean tryComplete() {
        this.responseLock.lock();
        try {
            // 已经完成
            if (this.isComplete()) {
                return false;
            }
            // 条件满足，可以完成
            if (this.countDownLatch.getCount() == 0) {
                // 标记完成
                this.complete();
                // 取消定时器
                this.cancelTimer();
                return true;
            }
            return false;
        } finally {
            this.responseLock.unlock();
        }
    }

    public void dispose() {
        this.writeFutureMap.clear();
        if (this.timerRef != null) {
            this.timerRef.cancel();
        }
    }

    public long getTimestamp() {
        return this.timestamp;
    }

}