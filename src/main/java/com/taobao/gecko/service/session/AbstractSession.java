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
package com.taobao.gecko.service.session;

import com.taobao.gecko.service.Dispatcher;
import com.taobao.gecko.core.future.FutureImpl;
import com.taobao.gecko.core.message.impl.PoisonWriteMessage;
import com.taobao.gecko.core.message.WriteMessage;
import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.codec.CodecFactory;
import com.taobao.gecko.service.config.SessionConfig;
import com.taobao.gecko.service.handler.Handler;
import com.taobao.gecko.service.statistics.Statistics;
import com.taobao.gecko.exception.NotifyRemotingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;


/**
 * 连接抽象基类
 *
 * @author boyan
 * @since 1.0, 2009-12-16 下午06:04:05
 */
public abstract class AbstractSession implements Session {

    protected static final Log log = LogFactory.getLog(AbstractSession.class);

    /** 用于存储接收到的消息的缓冲区 */
    protected IoBuffer readBuffer;
    /** 用于保存session上的属性 */
    protected final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();
    /** session闲置超时时间 */
    protected volatile long sessionIdleTimeout;
    /** session超时时间 */
    protected volatile long sessionTimeout;
    /** 消息编码器 */
    protected CodecFactory.Encoder encoder;
    /** 消息解码器 */
    protected CodecFactory.Decoder decoder;
    protected volatile boolean closed, innerClosed;
    protected Statistics statistics;
    protected Handler handler;
    /** 用于判断远端的IP地址是否为回环IP地址（即：127.0.0.1） */
    protected boolean loopback;
    /** 设置最后一次操作的时间搓 */
    public AtomicLong lastOperationTimeStamp = new AtomicLong(0);
    /** 表示当前待写入通道的所有消息字节大小 */
    protected AtomicLong scheduleWritenBytes = new AtomicLong(0);
    protected final Dispatcher dispatchMessageDispatcher;
    protected volatile boolean useBlockingWrite = false;
    protected volatile boolean useBlockingRead = true;
    protected volatile boolean handleReadWriteConcurrently = true;
    protected ReentrantLock writeLock = new ReentrantLock();
    /** 保存将被写入通道的消息 */
    protected Queue<WriteMessage> writeQueue;
    /** 表示当前要写入通道的消息 */
    protected AtomicReference<WriteMessage> currentMessage = new AtomicReference<WriteMessage>();



    public AbstractSession(final SessionConfig sessionConfig) {
        super();
        this.lastOperationTimeStamp.set(System.currentTimeMillis());
        this.statistics = sessionConfig.statistics;
        this.handler = sessionConfig.handler;
        this.writeQueue = sessionConfig.queue;
        this.encoder = sessionConfig.codecFactory.getEncoder();
        this.decoder = sessionConfig.codecFactory.getDecoder();
        this.dispatchMessageDispatcher = sessionConfig.dispatchMessageDispatcher;
        this.handleReadWriteConcurrently = sessionConfig.handleReadWriteConcurrently;
        this.sessionTimeout = sessionConfig.sessionTimeout;
        this.sessionIdleTimeout = sessionConfig.sessionIdelTimeout;
    }


    /**
     * 启动session：触发handler#onSessionStarted
     */
    public synchronized void start() {
        log.debug("session started");
        // 触发handler#onSessionStarted
        this.onStarted();
        this.start0();
    }

    /**
     * 关闭session：添加一个毒丸
     */
    public final void close() {
        this.setClosed(true);
        // 加入毒丸到队列
        this.addPoisonWriteMessage(new PoisonWriteMessage());
    }


    /**
     * 同步发送消息
     *
     * @param packet
     */
    public void write(final Object packet) {
        if (packet == null) {
            throw new NullPointerException("Null packet");
        }
        if (this.isClosed()) {
            return;
        }

        // 将消息包装为WriteMessage对象
        final WriteMessage message = this.wrapMessage(packet, null);
        this.scheduleWritenBytes.addAndGet(message.remaining());
        this.writeFromUserCode(message);
    }

    /**
     * 异步发送消息
     *
     * @param packet
     * @return
     */
    public Future<Boolean> asyncWrite(final Object packet) {
        if (this.isClosed()) {
            final FutureImpl<Boolean> writeFuture = new FutureImpl<Boolean>();
            writeFuture.failure(new IOException("连接已经被关闭"));
            return writeFuture;
        }
        if (packet == null) {
            throw new NullPointerException("Null packet");
        }

        final FutureImpl<Boolean> writeFuture = new FutureImpl<Boolean>();
        final WriteMessage message = this.wrapMessage(packet, writeFuture);
        this.scheduleWritenBytes.addAndGet(message.remaining());
        this.writeFromUserCode(message);
        return writeFuture;
    }





    // 子类扩展

    /**
     * 启动session
     */
    protected abstract void start0();

    /**
     * 将消息封装为WriteMessage对象
     *
     * @param msg
     * @param writeFuture
     * @return
     */
    protected abstract WriteMessage wrapMessage(Object msg, Future<Boolean> writeFuture);

    /**
     * 将消息写入通道，留给子类去实现
     *
     * @param message
     */
    protected abstract void writeFromUserCode(WriteMessage message);

    /**
     * 将从网络读取字节解码（反序列化）为对象，并通过消息派发器通知Session（读取的字节保存在#readBuffer对象中）
     */
    public abstract void decode();

    /**
     * 添加一个毒丸消息到发送消息的队列中
     *
     * @param poisonWriteMessage
     */
    protected abstract void addPoisonWriteMessage(PoisonWriteMessage poisonWriteMessage);

    /**
     * 关闭通道
     *
     * @throws IOException
     */
    protected abstract void closeChannel() throws IOException;




    protected void onIdle0() {
        // callback for sub class
    }

    /**
     * Pre-Process WriteMessage before writing to channel
     *
     * @param writeMessage
     * @return
     */
    protected WriteMessage preprocessWriteMessage(final WriteMessage writeMessage) {
        return writeMessage;
    }

    /**
     * 通知#handler#onMessageReceived处理接收到的消息，如果有消息派发器，则通过消息派发器进行通知
     *
     * @param message
     */
    protected void dispatchReceivedMessage(final Object message) {
        if (this.dispatchMessageDispatcher == null) {
            long start = -1;
            if (this.statistics != null && this.statistics.isStatistics()) {
                start = System.currentTimeMillis();
            }
            this.onMessage(message, this);
            if (start != -1) {
                this.statistics.statisticsProcess(System.currentTimeMillis() - start);
            }
        } else {
            this.dispatchMessageDispatcher.dispatch(new Runnable() {
                public void run() {
                    long start = -1;
                    if (AbstractSession.this.statistics != null && AbstractSession.this.statistics.isStatistics()) {
                        start = System.currentTimeMillis();
                    }
                    AbstractSession.this.onMessage(message, AbstractSession.this);
                    if (start != -1) {
                        AbstractSession.this.statistics.statisticsProcess(System.currentTimeMillis() - start);
                    }
                }

            });

        }

    }

    /**
     * 真正关闭session的方法
     */
    public void close0() {
        synchronized (this) {
            if (this.innerClosed) {
                return;
            }

            this.innerClosed = true;
            this.setClosed(true);
        }

        try {
            this.closeChannel();
            log.debug("session closed");
        } catch (final IOException e) {
            this.onException(e);
            log.error("Close session error", e);
        } finally {
            // 如果最后一个消息已经完全写入，告知用户
            final WriteMessage writeMessage = this.writeQueue.poll();
            if (writeMessage != null && !writeMessage.hasRemaining()) {
                this.onMessageSent(writeMessage);
            }

            for (final WriteMessage msg : this.writeQueue) {
                if (msg != null && msg.getWriteFuture() != null) {
                    msg.getWriteFuture().failure(new NotifyRemotingException("连接已经关闭"));
                }
            }
            this.onClosed();
            this.clearAttributes();
            this.writeQueue.clear();
        }
    }

    /**
     * 清空保存发送消息的队列
     */
    public void clearWriteQueue() {
        this.writeQueue.clear();
    }






    // 会话生命周期处理器的相关触发方法

    /**
     * 通知#handler#onMessageReceived处理接收到的消息
     *
     * @param message
     * @param session
     */
    private void onMessage(final Object message, final Session session) {
        try {
            this.handler.onMessageReceived(session, message);
        } catch (final Throwable e) {
            this.onException(e);
        }
    }
    /**
     * 触发handler#onExceptionCaught
     */
    public void onException(final Throwable e) {
        this.handler.onExceptionCaught(this, e);
    }
    /**
     * 触发handler#onSessionClosed
     */
    protected void onClosed() {
        try {
            this.handler.onSessionClosed(this);
        } catch (final Throwable e) {
            this.onException(e);
        }
    }
    /**
     * 触发handler#onSessionStarted
     */
    protected void onStarted() {
        try {
            this.handler.onSessionStarted(this);
        } catch (final Throwable e) {
            this.onException(e);
        }
    }
    /**
     * 触发handler#onSessionCreated
     */
    protected void onCreated() {
        try {
            this.handler.onSessionCreated(this);
        } catch (final Throwable e) {
            this.onException(e);
        }
    }
    /**
     * 触发handler#onMessageSent
     *
     * @param message
     */
    protected void onMessageSent(final WriteMessage message) {
        this.handler.onMessageSent(this, message.getMessage());
    }
    /**
     * 同步，防止多个reactor并发调用此方法；
     * 触发handler#onSessionIdle
     */
    protected synchronized void onIdle() {
        try {
            // 再次检测，防止重复调用
            if (this.isIdle()) {
                this.onIdle0();
                this.handler.onSessionIdle(this);
                this.updateTimeStamp();
            }
        } catch (final Throwable e) {
            this.onException(e);
        }
    }
    protected void onConnected() {
        try {
            this.handler.onSessionConnected(this, null);
        } catch (final Throwable throwable) {
            this.onException(throwable);
        }
    }
    public void onExpired() {
        try {
            if (this.isExpired() && !this.isClosed()) {
                this.handler.onSessionExpired(this);
                this.close();
            }
        } catch (final Throwable e) {
            this.onException(e);
        }
    }


    // 缓冲区相关设置

    /**
     * 返回读缓冲区的字节顺序，例如：大头（BIG_ENDIAN）或者小头（LITTLE_ENDIAN）
     *
     * @return
     */
    public final ByteOrder getReadBufferByteOrder() {
        if (this.readBuffer == null) {
            throw new IllegalStateException();
        }

        return this.readBuffer.order();
    }
    /**
     * 设置读缓冲区的字节顺序
     *
     * @param readBufferByteOrder
     */
    public final void setReadBufferByteOrder(final ByteOrder readBufferByteOrder) {
        if (this.readBuffer == null) {
            throw new NullPointerException("Null ReadBuffer");
        }

        this.readBuffer.order(readBufferByteOrder);
    }



    // 会话属性相关操作

    public void setAttribute(final String key, final Object value) {
        this.attributes.put(key, value);
    }
    public Set<String> attributeKeySet() {
        return this.attributes.keySet();
    }
    public Object setAttributeIfAbsent(final String key, final Object value) {
        return this.attributes.putIfAbsent(key, value);
    }
    public void removeAttribute(final String key) {
        this.attributes.remove(key);
    }
    public Object getAttribute(final String key) {
        return this.attributes.get(key);
    }
    public void clearAttributes() {
        this.attributes.clear();
    }



    // getter and setter ...

    /**
     * 返回会话是否超时
     *
     * @return
     */
    public boolean isExpired() {
        return false;
    }

    /**
     * 判断会话是否超过了闲置时间
     *
     * @return
     */
    public boolean isIdle() {
        final long lastOpTimestamp = this.getLastOperationTimeStamp();
        return lastOpTimestamp > 0 && System.currentTimeMillis() - lastOpTimestamp > this.sessionIdleTimeout;
    }
    public long getScheduleWritenBytes() {
        return this.scheduleWritenBytes.get();
    }
    public void updateTimeStamp() {
        this.lastOperationTimeStamp.set(System.currentTimeMillis());
    }
    public long getLastOperationTimeStamp() {
        return this.lastOperationTimeStamp.get();
    }
    public final boolean isHandleReadWriteConcurrently() {
        return this.handleReadWriteConcurrently;
    }
    public final void setHandleReadWriteConcurrently(final boolean handleReadWriteConcurrently) {
        this.handleReadWriteConcurrently = handleReadWriteConcurrently;
    }
    public final boolean isClosed() {
        return this.closed;
    }
    public final void setClosed(final boolean closed) {
        this.closed = closed;
    }
    public final boolean isLoopbackConnection() {
        return this.loopback;
    }
    public boolean isUseBlockingWrite() {
        return this.useBlockingWrite;
    }
    public void setUseBlockingWrite(final boolean useBlockingWrite) {
        this.useBlockingWrite = useBlockingWrite;
    }
    public boolean isUseBlockingRead() {
        return this.useBlockingRead;
    }
    public void setUseBlockingRead(final boolean useBlockingRead) {
        this.useBlockingRead = useBlockingRead;
    }
    public Queue<WriteMessage> getWriteQueue() {
        return this.writeQueue;
    }
    public Statistics getStatistics() {
        return this.statistics;
    }
    public Handler getHandler() {
        return this.handler;
    }
    public Dispatcher getDispatchMessageDispatcher() {
        return this.dispatchMessageDispatcher;
    }
    public ReentrantLock getWriteLock() {
        return this.writeLock;
    }
    public long getSessionIdleTimeout() {
        return this.sessionIdleTimeout;
    }
    public void setSessionIdleTimeout(final long sessionIdleTimeout) {
        this.sessionIdleTimeout = sessionIdleTimeout;
    }
    public long getSessionTimeout() {
        return this.sessionTimeout;
    }
    public void setSessionTimeout(final long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
    public CodecFactory.Encoder getEncoder() {
        return this.encoder;
    }
    public void setEncoder(final CodecFactory.Encoder encoder) {
        this.encoder = encoder;
    }
    public CodecFactory.Decoder getDecoder() {
        return this.decoder;
    }
    public void setDecoder(final CodecFactory.Decoder decoder) {
        this.decoder = decoder;
    }
    public IoBuffer getReadBuffer() {
        return this.readBuffer;
    }
    public void setReadBuffer(final IoBuffer readBuffer) {
        this.readBuffer = readBuffer;
    }






    static final class FailFuture implements Future<Boolean> {

        public boolean cancel(final boolean mayInterruptIfRunning) {
            return Boolean.FALSE;
        }


        public Boolean get() throws InterruptedException, ExecutionException {
            return Boolean.FALSE;
        }


        public Boolean get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return Boolean.FALSE;
        }


        public boolean isCancelled() {
            return false;
        }


        public boolean isDone() {
            return true;
        }

    }


}