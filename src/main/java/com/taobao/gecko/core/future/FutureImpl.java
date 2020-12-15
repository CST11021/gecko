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
package com.taobao.gecko.core.future;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * 简单的{@link Future}实现，它使用同步{@link Object}在生命周期内进行同步。
 *
 * @author Alexey Stashok
 * @see Future
 */
public class FutureImpl<R> implements Future<R> {

    /** 用于阻塞线程，直到返回异步结果 */
    private final Object sync;
    /** 表示该future是否完成（即异步的收到了异步的响应结果） */
    private boolean isDone;
    /** 标识该future是否取消 */
    private boolean isCancelled;
    /**  */
    private Throwable failure;
    /** 表示异步的响应结果 */
    protected R result;

    protected Object[] args;

    public FutureImpl() {
        this(new Object());
    }
    public FutureImpl(Object... args) {
        this();
        this.args = args;
    }
    public FutureImpl(Object sync) {
        this.sync = sync;
    }


    public Object[] getArgs() {
        return this.args;
    }

    /**
     * 获取当前结果值而没有任何阻塞
     *
     * @return current result value without any blocking.
     */
    public R getResult() {
        synchronized (this.sync) {
            return this.result;
        }
    }

    /**
     * 设置结果值并通知操作完成
     *
     * @param result the result value
     */
    public void setResult(R result) {
        synchronized (this.sync) {
            this.result = result;
            this.notifyHaveResult();
        }
    }

    /**
     * 取消当前异步请求
     *
     * @param mayInterruptIfRunning
     * @return
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (this.sync) {
            this.isCancelled = true;
            this.notifyHaveResult();
            return true;
        }
    }

    /**
     * 返回当前异步请求是否被取消
     *
     * @return
     */
    public boolean isCancelled() {
        synchronized (this.sync) {
            return this.isCancelled;
        }
    }

    /**
     * 判断当前异步请求是否完成
     *
     * @return
     */
    public boolean isDone() {
        synchronized (this.sync) {
            return this.isDone;
        }
    }

    /**
     * 一直轮询，直到返回结果（session）
     *
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public R get() throws InterruptedException, ExecutionException {
        synchronized (this.sync) {
            for (; ; ) {
                if (this.isDone) {
                    if (this.isCancelled) {
                        throw new CancellationException();
                    } else if (this.failure != null) {
                        throw new ExecutionException(this.failure);
                    } else if (this.result != null) {
                        return this.result;
                    }
                }

                this.sync.wait();
            }
        }
    }

    /**
     * 一直轮询，直到返回结果（session）或者超时
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = TimeUnit.MILLISECONDS.convert(timeout, unit);
        synchronized (this.sync) {
            for (; ; ) {
                if (this.isDone) {
                    if (this.isCancelled) {
                        throw new CancellationException();
                    } else if (this.failure != null) {
                        throw new ExecutionException(this.failure);
                    } else if (this.result != null) {
                        return this.result;
                    }
                } else if (System.currentTimeMillis() - startTime > timeoutMillis) {
                    throw new TimeoutException();
                }

                this.sync.wait(timeoutMillis);
            }
        }
    }

    /**
     * Notify about the failure, occured during asynchronous operation execution.
     *
     * @param failure
     */
    public void failure(Throwable failure) {
        synchronized (this.sync) {
            this.failure = failure;
            this.notifyHaveResult();
        }
    }

    /**
     * Notify blocked listeners threads about operation completion.
     */
    protected void notifyHaveResult() {
        this.isDone = true;
        this.sync.notifyAll();
    }
}