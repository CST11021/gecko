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
package com.taobao.gecko.core.nio.impl;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.EventType;
import com.taobao.gecko.core.core.WriteMessage;
import com.taobao.gecko.core.core.impl.AbstractSession;
import com.taobao.gecko.core.core.impl.FileWriteMessage;
import com.taobao.gecko.core.core.impl.FutureImpl;
import com.taobao.gecko.core.core.impl.PoisonWriteMessage;
import com.taobao.gecko.core.nio.NioSession;
import com.taobao.gecko.core.nio.NioSessionConfig;
import com.taobao.gecko.core.util.SelectorFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.*;
import java.util.concurrent.Future;

/**
 * 基于Nio实现的会话
 *
 * @author boyan
 * @since 1.0, 2009-12-16 下午06:06:25
 */
public abstract class AbstractNioSession extends AbstractSession implements NioSession {

    protected SelectorManager selectorManager;
    protected SelectableChannel selectableChannel;


    public AbstractNioSession(final NioSessionConfig sessionConfig) {
        super(sessionConfig);
        this.selectorManager = sessionConfig.selectorManager;
        this.selectableChannel = sessionConfig.selectableChannel;
    }


    // 实现父类的抽象方法

    /**
     * 扩展父类方法，session启动的时候通过selectorManager注册
     */
    @Override
    protected void start0() {
        this.registerSession();
    }

    /**
     * 为了让NioController可见
     */
    @Override
    protected final void close0() {
        super.close0();
    }

    /**
     * 添加一个毒丸消息到发送消息的队列中
     *
     * @param poisonWriteMessage
     */
    @Override
    protected void addPoisonWriteMessage(final PoisonWriteMessage poisonWriteMessage) {
        this.writeQueue.offer(poisonWriteMessage);
        this.selectorManager.registerSession(this, EventType.ENABLE_WRITE);
    }

    /**
     * 将消息写入通道
     *
     * @param message
     */
    @Override
    protected final void writeFromUserCode(final WriteMessage message) {
        if (this.schduleWriteMessage(message)) {
            return;
        }
        // 到这里，当前线程一定是IO线程
        this.onWrite(null);
    }



    // ----- 实现NioSession接口 -----


    /**
     * 获得连接对应的channel
     *
     * @return
     */
    public SelectableChannel channel() {
        return this.selectableChannel;
    }

    /**
     * 派发IO事件
     *
     * @param event     对应session的事件类型
     * @param selector  当前连接对应的选择器，用于注册相关事件
     */
    public final void onEvent(final EventType event, final Selector selector) {

        final SelectionKey key = this.selectableChannel.keyFor(selector);

        switch (event) {
            case EXPIRED:
                this.onExpired();
                break;
            case WRITEABLE:
                this.onWrite(key);
                break;
            case READABLE:
                this.onRead(key);
                break;
            case ENABLE_WRITE:
                this.enableWrite(selector);
                break;
            case ENABLE_READ:
                this.enableRead(selector);
                break;
            case IDLE:
                this.onIdle();
                break;
            case CONNECTED:
                this.onConnected();
                break;
            default:
                log.error("Unknown event:" + event.name());
                break;
        }
    }

    /**
     * 注册OP_READ
     *
     * @param selector
     */
    public final void enableRead(final Selector selector) {
        final SelectionKey key = this.selectableChannel.keyFor(selector);
        if (key != null && key.isValid()) {
            this.interestRead(key);
        } else {
            try {
                this.selectableChannel.register(selector, SelectionKey.OP_READ, this);
            } catch (final ClosedChannelException e) {
                // ignore
            } catch (final CancelledKeyException e) {
                // ignore
            }
        }
    }

    /**
     * 注册OP_WRITE
     *
     * @param selector
     */
    public final void enableWrite(final Selector selector) {
        final SelectionKey key = this.selectableChannel.keyFor(selector);
        if (key != null && key.isValid()) {
            this.interestWrite(key);
        } else {
            try {
                this.selectableChannel.register(selector, SelectionKey.OP_WRITE, this);
            } catch (final ClosedChannelException e) {
                // ignore
            } catch (final CancelledKeyException e) {
                // ignore
            }
        }
    }

    /**
     * 通过selectorManager将异步的请求回调器注册到reactor
     *
     * @param timerRef
     */
    public void insertTimer(final TimerRef timerRef) {
        if (this.isClosed()) {
            return;
        }

        this.selectorManager.insertTimer(timerRef);
    }


    // 发送消息

    /**
     * 往连接写入消息，可被中断，中断可能引起连接断开，请慎重使用
     *
     * @param packet
     */
    public void writeInterruptibly(final Object packet) {
        if (packet == null) {
            throw new NullPointerException("Null packet");
        }
        if (this.isClosed()) {
            return;
        }
        final WriteMessage message = this.wrapMessage(packet, null);
        this.scheduleWritenBytes.addAndGet(message.remaining());
        this.write0(message);
    }

    /**
     * 往连接异步写入消息，可被中断，中断可能引起连接断开，请慎重使用
     *
     * @param packet
     * @return
     */
    public Future<Boolean> asyncWriteInterruptibly(final Object packet) {
        if (packet == null) {
            throw new NullPointerException("Null packet");
        }
        if (this.isClosed()) {
            final FutureImpl<Boolean> writeFuture = new FutureImpl<Boolean>();
            writeFuture.failure(new IOException("连接已经被关闭"));
            return writeFuture;
        }
        final FutureImpl<Boolean> writeFuture = new FutureImpl<Boolean>();
        final WriteMessage message = this.wrapMessage(packet, writeFuture);
        this.scheduleWritenBytes.addAndGet(message.remaining());
        this.write0(message);
        return writeFuture;
    }

    /**
     * 从指定FileChannel的position位置开始传输size个字节到socket，其中head和tail是在传输文件前后写入的数据，可以为null
     *
     * @param head
     * @param tail
     * @param src
     * @param position
     * @param size
     * @return
     */
    public Future<Boolean> transferFrom(final IoBuffer head, final IoBuffer tail, final FileChannel src, final long position, long size) {
        this.checkParams(src, position);
        size = this.normalSize(src, position, size);
        final FutureImpl<Boolean> future = new FutureImpl<Boolean>();
        final WriteMessage message = new FileWriteMessage(position, size, future, src, head, tail);
        this.scheduleWritenBytes.addAndGet(message.remaining());
        this.writeFromUserCode(message);
        return future;
    }

    /**
     * 从指定FileChannel的position位置开始传输size个字节到socket，返回future对象查询状态, 其中head和tail是在传输文件前后写入的数据，可以为null
     *
     * @param head
     * @param tail
     * @param src
     * @param position
     * @param size
     * @return
     */
    public Future<Boolean> asyncTransferFrom(final IoBuffer head, final IoBuffer tail, final FileChannel src, final long position, long size) {
        this.checkParams(src, position);
        size = this.normalSize(src, position, size);
        final FutureImpl<Boolean> future = new FutureImpl<Boolean>();
        final WriteMessage message = new FileWriteMessage(position, size, future, src, head, tail);
        this.scheduleWritenBytes.addAndGet(message.remaining());
        this.writeFromUserCode(message);
        return future;
    }

    // --------------------





    // 实现 Session 接口

    /**
     * 获取本地的IP地址
     *
     * @return
     */
    public InetAddress getLocalAddress() {
        return ((SocketChannel) this.selectableChannel).socket().getLocalAddress();
    }

    /**
     * 刷新写入队列，如果正在运行OP_WRITE，则此方法可能无效
     *
     */
    public void flush() {
        if (this.isClosed()) {
            return;
        }

        this.flush0();
    }





    // 子类扩展

    protected abstract Object writeToChannel0(WriteMessage msg) throws ClosedChannelException, IOException;

    /**
     * 将从网络读取字节解码（反序列化）为对象，并通过消息派发器通知Session（读取的字节保存在#readBuffer对象中），触发Handler.onMessageReceived
     */
    protected abstract void readFromBuffer();

    protected final void flush0() {
        SelectionKey tmpKey = null;
        Selector writeSelector = null;
        int attempts = 0;
        try {
            while (true) {
                if (writeSelector == null) {
                    writeSelector = SelectorFactory.getSelector();
                    if (writeSelector == null) {
                        return;
                    }
                    tmpKey = this.selectableChannel.register(writeSelector, SelectionKey.OP_WRITE);
                }

                if (writeSelector.select(1000) == 0) {
                    attempts++;
                    // 最多尝试3次
                    if (attempts > 2) {
                        return;
                    }
                } else {
                    break;
                }
            }
            this.onWrite(this.selectableChannel.keyFor(writeSelector));
        } catch (final ClosedChannelException cce) {
            // ignore
            this.close0();
        } catch (final Throwable t) {
            this.close0();
            this.onException(t);
            log.error("Flush error", t);
        } finally {
            if (tmpKey != null) {
                // Cancel the key.
                tmpKey.cancel();
                tmpKey = null;
            }
            if (writeSelector != null) {
                try {
                    writeSelector.selectNow();
                } catch (final IOException e) {
                    log.error("Temp selector selectNow error", e);
                }
                // return selector
                SelectorFactory.returnSelector(writeSelector);
            }
        }
    }

    protected void onWrite(final SelectionKey key) {
        boolean isLockedByMe = false;
        if (this.currentMessage.get() == null) {
            // 获取下一个待写消息
            final WriteMessage nextMessage = this.writeQueue.peek();
            if (nextMessage != null && this.writeLock.tryLock()) {
                if (!this.writeQueue.isEmpty() && this.currentMessage.compareAndSet(null, nextMessage)) {
                    this.writeQueue.remove();
                }
            } else {
                return;
            }
        } else if (!this.writeLock.tryLock()) {
            return;
        }

        this.updateTimeStamp();
        // 加锁成功
        isLockedByMe = true;
        WriteMessage currentMessage = null;
        // 经验值，写入的最大数量为readBufferSize*3/2能达到最佳性能，并且不会太影响读写的公平性
        final long maxWritten = this.readBuffer.capacity() + this.readBuffer.capacity() >>> 1;



        try {
            long written = 0;
            while (this.currentMessage.get() != null) {
                currentMessage = this.currentMessage.get();
                currentMessage = this.preprocessWriteMessage(currentMessage);
                this.currentMessage.set(currentMessage);
                final long before = this.currentMessage.get().remaining();
                // 表示原始的消息对象WriteMessage#getMessage
                Object writeResult = null;
                // 如果写入的数量小于最大值，继续写，否则中断write，继续注册OP_WRITE
                if (written < maxWritten) {
                    writeResult = this.writeToChannel(currentMessage);
                    written += before - this.currentMessage.get().remaining();
                } else {
                    // 不写入,继续注册OP_WRITE
                }
                // 发送成功
                if (writeResult != null) {
                    this.currentMessage.set(this.writeQueue.poll());
                    if (currentMessage.isWriting()) {
                        this.onMessageSent(currentMessage);
                    }
                    // 取下一个消息处理
                    if (this.currentMessage.get() == null) {
                        if (isLockedByMe) {
                            isLockedByMe = false;
                            this.writeLock.unlock();
                        }
                        // 再尝试一次
                        final WriteMessage nextMessage = this.writeQueue.peek();
                        if (nextMessage != null && this.writeLock.tryLock()) {
                            isLockedByMe = true;
                            if (!this.writeQueue.isEmpty() && this.currentMessage.compareAndSet(null, nextMessage)) {
                                this.writeQueue.remove();
                            }
                            continue;
                        } else {
                            break;
                        }
                    }
                } else { // 不完全写入
                    if (isLockedByMe) {
                        isLockedByMe = false;
                        this.writeLock.unlock();
                    }
                    // 继续注册OP_WRITE，等待写
                    this.selectorManager.registerSession(this, EventType.ENABLE_WRITE);
                    break;
                }
            }
        } catch (final ClosedChannelException e) {
            this.close0();
            // ignore，不通知用户
            if (currentMessage != null && currentMessage.getWriteFuture() != null) {
                currentMessage.getWriteFuture().failure(e);
            }
        } catch (final Throwable e) {
            this.close0();
            this.handler.onExceptionCaught(this, e);
            if (currentMessage != null && currentMessage.getWriteFuture() != null) {
                currentMessage.getWriteFuture().failure(e);
            }
        } finally {
            if (isLockedByMe) {
                this.writeLock.unlock();
            }
        }
    }

    protected boolean schduleWriteMessage(final WriteMessage writeMessage) {
        // 将消息放到队列中
        final boolean offered = this.writeQueue.offer(writeMessage);
        assert offered;
        final Reactor reactor = this.selectorManager.getReactorFromSession(this);
        if (Thread.currentThread() != reactor) {
            this.selectorManager.registerSession(this, EventType.ENABLE_WRITE);
            return true;
        }
        return false;
    }

    protected void onRead(final SelectionKey key) {
        this.updateTimeStamp();
        this.readFromBuffer();
    }

    /**
     * 通过selectorManager将session从reactor注销
     */
    protected void unregisterSession() {
        this.selectorManager.registerSession(this, EventType.UNREGISTER);
    }

    /**
     * 通过selectorManager注册session（将session注册到reactor）
     */
    protected final void registerSession() {
        this.selectorManager.registerSession(this, EventType.REGISTER);
    }

    private void interestRead(final SelectionKey key) {
        if (key.attachment() == null) {
            key.attach(this);
        }
        key.interestOps(key.interestOps() | SelectionKey.OP_READ);
    }

    /**
     * 将消息写入通道，并返回原始的消息对象（WriteMessage#getMessage）
     *
     * @param msg
     * @return
     * @throws ClosedChannelException
     * @throws IOException
     */
    private Object writeToChannel(final WriteMessage msg) throws ClosedChannelException, IOException {
        if (msg instanceof PoisonWriteMessage) {
            this.close0();
            return msg;
        } else {
            return this.writeToChannel0(msg);
        }
    }

    private void interestWrite(final SelectionKey key) {
        if (key.attachment() == null) {
            key.attach(this);
        }
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
    }

    private void write0(WriteMessage message) {
        boolean isLockedByMe = false;
        Object writeResult = null;
        try {
            if (this.currentMessage.get() == null && this.writeLock.tryLock()) {
                isLockedByMe = true;
                if (this.currentMessage.compareAndSet(null, message)) {
                    message = this.preprocessWriteMessage(message);
                    this.currentMessage.set(message);
                    try {
                        writeResult = this.writeToChannel(message);
                    } catch (final ClosedChannelException e) {
                        this.close0();
                        if (message.getWriteFuture() != null) {
                            message.getWriteFuture().failure(e);
                        }
                    } catch (final Throwable e) {
                        this.close0();
                        if (message.getWriteFuture() != null) {
                            message.getWriteFuture().failure(e);
                        }
                        this.onException(e);
                    }
                } else {
                    isLockedByMe = false;
                    this.writeLock.unlock();
                }
            }
            // 写入成功
            if (isLockedByMe && writeResult != null) {
                if (message.isWriting()) {
                    this.onMessageSent(message);
                }
                // 获取下一个元素
                final WriteMessage nextElement = this.writeQueue.poll();
                if (nextElement != null) {
                    this.currentMessage.set(nextElement);
                    isLockedByMe = false;
                    this.writeLock.unlock();
                    // 注册OP_WRITE
                    this.selectorManager.registerSession(this, EventType.ENABLE_WRITE);
                } else {
                    this.currentMessage.set(null);
                    isLockedByMe = false;
                    this.writeLock.unlock();
                    // 再次check
                    if (this.writeQueue.peek() != null) {
                        this.selectorManager.registerSession(this, EventType.ENABLE_WRITE);
                    }
                }
            } else {
                // 写入失败
                boolean isRegisterForWriting = false;
                if (this.currentMessage.get() != message) {
                    // 加入队列
                    this.writeQueue.offer(message);
                    // 判断是否正在写，没有的话，需要注册OP_WRITE
                    if (!this.writeLock.isLocked()) {
                        isRegisterForWriting = true;
                    }
                } else {
                    isRegisterForWriting = true;
                    if (isLockedByMe) {
                        isLockedByMe = false;
                        this.writeLock.unlock();
                    }
                }
                if (isRegisterForWriting) {
                    this.selectorManager.registerSession(this, EventType.ENABLE_WRITE);
                }
            }
        } finally {
            // 确保释放锁
            if (isLockedByMe) {
                this.writeLock.unlock();
            }
        }
    }

    private void checkParams(final FileChannel src, final long position) {
        if (src == null) {
            throw new NullPointerException("Null FileChannel");
        }
        try {
            if (position < 0 || position > src.size()) {
                throw new ArrayIndexOutOfBoundsException("Could not write position out of bounds of file channel");
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long normalSize(final FileChannel src, final long position, long size) {
        try {
            size = Math.min(src.size() - position, size);
            return size;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}