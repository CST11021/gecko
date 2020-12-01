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

/**
 * Copyright [2008-2009] [dennis zhuang]
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.EventType;
import com.taobao.gecko.core.core.WriteMessage;
import com.taobao.gecko.core.core.impl.ByteBufferCodecFactory;
import com.taobao.gecko.core.core.impl.ByteBufferWriteMessage;
import com.taobao.gecko.core.core.impl.FutureImpl;
import com.taobao.gecko.core.nio.NioSessionConfig;
import com.taobao.gecko.core.nio.input.ChannelInputStream;
import com.taobao.gecko.core.nio.output.ChannelOutputStream;
import com.taobao.gecko.core.util.ByteBufferUtils;
import com.taobao.gecko.core.util.SelectorFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.*;
import java.util.concurrent.Future;

/**
 * Nio tcp连接
 *
 *
 *
 * @author boyan
 *
 * @since 1.0, 2009-12-16 下午06:09:15
 */
public class NioTCPSession extends AbstractNioSession {

    /** 如果写入返回为0，强制循环多次，提高发送效率 */
    static final int WRITE_SPIN_COUNT = Integer.parseInt(System.getProperty("notify.remoting.write_spin_count", "16"));

    /** 表示远端的IP地址 */
    private InetSocketAddress remoteAddress;
    /** 表示readBuffer初始的缓冲区大小，及对应readBuffer#capacity */
    private final int initialReadBufferSize;
    /** 表示#selectableChannel的接收的消息的缓存区大小 */
    private int recvBufferSize = 16 * 1024;



    public NioTCPSession(final NioSessionConfig sessionConfig, final int readRecvBufferSize) {
        super(sessionConfig);
        if (this.selectableChannel != null && this.getRemoteSocketAddress() != null) {
            // 远端的IP地址是否为回环IP地址
            this.loopback = this.getRemoteSocketAddress().getAddress().isLoopbackAddress();
        }

        // 设置用于存储接收的消息的缓冲区
        this.setReadBuffer(IoBuffer.allocate(readRecvBufferSize));
        this.initialReadBufferSize = this.readBuffer.capacity();
        // 触发Handler#onSessionCreated
        this.onCreated();
        try {
            this.recvBufferSize = ((SocketChannel) this.selectableChannel).socket().getReceiveBufferSize();
        } catch (final Exception e) {
            log.error("Get socket receive buffer size failed", e);
        }
    }

    /**
     * 重写父类方法，判断session是否过期，父类方法默认消息不过期
     *
     * @return
     */
    @Override
    public final boolean isExpired() {
        if (log.isDebugEnabled()) {
            log.debug("sessionTimeout=" + this.sessionTimeout
                    + ",this.timestamp=" + this.lastOperationTimeStamp.get()
                    + ",current=" + System.currentTimeMillis());
        }
        return this.sessionTimeout <= 0 ? false
                : System.currentTimeMillis() - this.lastOperationTimeStamp.get() >= this.sessionTimeout;
    }

    /**
     * 调用WriteMessage#write方法将消息写入通道
     *
     * @param channel
     * @param message
     * @return
     * @throws IOException
     */
    protected final long doRealWrite(final SelectableChannel channel, final WriteMessage message) throws IOException {
        return message.write((WritableByteChannel) channel);
    }

    /**
     * 将WriteMessage封装的消息对象写入通道，并返回原始的消息对象即WriteMessage#getMessage
     *
     * @param message
     * @return
     * @throws IOException
     */
    @Override
    protected Object writeToChannel0(final WriteMessage message) throws IOException {

        if (message.getWriteFuture() != null && !message.isWriting() && message.getWriteFuture().isCancelled()) {
            this.scheduleWritenBytes.addAndGet(0 - message.remaining());
            return message.getMessage();
        }

        // 判断如果消息字节缓冲区已经全部写入完成，则设置写入完成标识，并返回原始的消息对象
        if (!message.hasRemaining()) {
            if (message.getWriteFuture() != null) {
                message.getWriteFuture().setResult(Boolean.TRUE);
            }
            return message.getMessage();
        }

        // begin writing
        message.writing();
        if (this.useBlockingWrite) {
            return this.blockingWrite(this.selectableChannel, message, message);
        } else {
            for (int i = 0; i < WRITE_SPIN_COUNT; i++) {
                final long n = this.doRealWrite(this.selectableChannel, message);
                if (n > 0) {
                    this.statistics.statisticsWrite(n);
                    this.scheduleWritenBytes.addAndGet(0 - n);
                    break;
                }
            }

            // 如果缓冲区的字节都写入完成了，则将消息的WriteFuture标记设置为true，表示写入完成
            if (!message.hasRemaining()) {
                if (message.getWriteFuture() != null) {
                    message.getWriteFuture().setResult(Boolean.TRUE);
                }
                return message.getMessage();
            }

            // 有更多数据，但缓冲区已满，请等待下一次写入
            return null;
        }

    }

    /**
     * 获取远端的IP地址
     *
     * @return
     */
    public InetSocketAddress getRemoteSocketAddress() {
        if (this.remoteAddress == null) {
            if (this.selectableChannel instanceof SocketChannel) {
                this.remoteAddress = (InetSocketAddress) ((SocketChannel) this.selectableChannel).socket().getRemoteSocketAddress();
            }
        }
        return this.remoteAddress;
    }

    /**
     * 阻塞写，采用temp selector强制写入
     *
     * @param channel
     * @param message
     * @param writeBuffer
     * @return
     * @throws IOException
     * @throws ClosedChannelException
     */
    protected final Object blockingWrite(final SelectableChannel channel, final WriteMessage message, final WriteMessage writeBuffer) throws IOException, ClosedChannelException {
        SelectionKey tmpKey = null;
        Selector writeSelector = null;
        int attempts = 0;
        int bytesProduced = 0;
        try {


            while (writeBuffer.hasRemaining()) {
                // 调用WriteMessage#write方法将消息写入通道
                final long len = this.doRealWrite(channel, writeBuffer);
                if (len > 0) {
                    attempts = 0;
                    bytesProduced += len;
                    // 统计写入的消息字节大小
                    this.statistics.statisticsWrite(len);
                } else {
                    // 走到这里说明写入的消息是个毒丸，当写到此消息的时候，连接将关闭，参考 PoisonWriteMessage 实现
                    attempts++;
                    if (writeSelector == null) {
                        // 借用一个选择器
                        writeSelector = SelectorFactory.getSelector();
                        if (writeSelector == null) {
                            continue;
                        }
                        tmpKey = channel.register(writeSelector, SelectionKey.OP_WRITE);
                    }

                    if (writeSelector.select(1000) == 0) {
                        if (attempts > 2) {
                            throw new IOException("Client disconnected");
                        }
                    }

                }
            }


            // 设置WriteMessage#writeFuture标记已经写入完成
            if (!writeBuffer.hasRemaining() && message.getWriteFuture() != null) {
                message.getWriteFuture().setResult(Boolean.TRUE);
            }

        } finally {
            if (tmpKey != null) {
                tmpKey.cancel();
                tmpKey = null;
            }

            // 归还选择器
            if (writeSelector != null) {
                writeSelector.selectNow();
                SelectorFactory.returnSelector(writeSelector);
            }
            this.scheduleWritenBytes.addAndGet(0 - bytesProduced);
        }
        return message.getMessage();
    }

    /**
     * 重写父类方法，
     *
     * @param msg
     * @param writeFuture
     * @return
     */
    @Override
    protected WriteMessage wrapMessage(final Object msg, final Future<Boolean> writeFuture) {
        final ByteBufferWriteMessage message = new ByteBufferWriteMessage(msg, (FutureImpl<Boolean>) writeFuture);
        if (message.getWriteBuffer() == null) {
            message.setWriteBuffer(this.encoder.encode(message.getMessage(), this));
        }
        return message;
    }

    /**
     * 将从网络读取字节解码（反序列化）为对象，并通过消息派发器通知Session（读取的字节保存在#readBuffer对象中），触发Handler.onMessageReceived
     */
    @Override
    protected void readFromBuffer() {
        if (!this.readBuffer.hasRemaining()) {
            this.readBuffer = IoBuffer.wrap(ByteBufferUtils.increaseBufferCapatity(this.readBuffer.buf(), this.recvBufferSize));
        }

        int n = -1;
        int readCount = 0;
        try {
            while ((n = ((ReadableByteChannel) this.selectableChannel).read(this.readBuffer.buf())) > 0) {
                readCount += n;
                // readBuffer没有空间，跳出循环
                if (!this.readBuffer.hasRemaining()) {
                    break;
                }
            }
            if (readCount > 0) {
                this.readBuffer.flip();
                // 将从网络读取字节解码（反序列化）为对象，并通过消息派发器通知Session（读取的字节保存在#readBuffer对象中），触发Handler.onMessageReceived
                this.decode();
                this.readBuffer.compact();
            } else if (readCount == 0 && this.useBlockingRead) {
                if (this.selectableChannel instanceof SocketChannel
                        && !((SocketChannel) this.selectableChannel).socket().isInputShutdown()) {
                    n = this.blockingRead();
                }
                if (n > 0) {
                    readCount += n;
                }
            }

            // Connection closed
            if (n < 0) {
                this.close0();
            } else {
                this.selectorManager.registerSession(this, EventType.ENABLE_READ);
            }

            if (log.isDebugEnabled()) {
                log.debug("read " + readCount + " bytes from channel");
            }
        } catch (final ClosedChannelException e) {
            // ignore，不需要用户知道
            this.close0();
        } catch (final Throwable e) {
            this.close0();
            this.onException(e);
        }
    }

    protected final int blockingRead() throws ClosedChannelException, IOException {
        int n = 0;
        final Selector readSelector = SelectorFactory.getSelector();
        SelectionKey tmpKey = null;
        try {
            if (this.selectableChannel.isOpen()) {
                tmpKey = this.selectableChannel.register(readSelector, 0);
                tmpKey.interestOps(tmpKey.interestOps() | SelectionKey.OP_READ);
                final int code = readSelector.select(500);
                tmpKey.interestOps(tmpKey.interestOps() & ~SelectionKey.OP_READ);
                if (code > 0) {
                    do {
                        n = ((ReadableByteChannel) this.selectableChannel).read(this.readBuffer.buf());
                        if (log.isDebugEnabled()) {
                            log.debug("use temp selector read " + n + " bytes");
                        }
                    } while (n > 0 && this.readBuffer.hasRemaining());
                    this.readBuffer.flip();
                    this.decode();
                    this.readBuffer.compact();
                }
            }
        } finally {
            if (tmpKey != null) {
                tmpKey.cancel();
                tmpKey = null;
            }
            if (readSelector != null) {
                // Cancel the key.
                readSelector.selectNow();
                SelectorFactory.returnSelector(readSelector);
            }
        }
        return n;
    }

    /**
     * 将从网络读取字节解码（反序列化）为对象，并通过消息派发器通知Session（读取的字节保存在#readBuffer对象中），触发Handler.onMessageReceived
     */
    @Override
    public void decode() {
        Object message;
        int size = this.readBuffer.remaining();
        while (this.readBuffer.hasRemaining()) {
            try {
                message = this.decoder.decode(this.readBuffer, this);
                if (message == null) {
                    break;
                } else {
                    if (this.statistics.isStatistics()) {
                        this.statistics.statisticsRead(size - this.readBuffer.remaining());
                        size = this.readBuffer.remaining();
                    }
                }

                // 通知#handler#onMessageReceived处理接收到的消息
                this.dispatchReceivedMessage(message);
            } catch (final Exception e) {
                this.onException(e);
                log.error("Decode error", e);
                super.close();
                break;
            }
        }
    }

    public Socket socket() {
        return ((SocketChannel) this.selectableChannel).socket();
    }

    public ChannelInputStream getInputStream(final Object msg) throws IOException {
        if (this.decoder instanceof ByteBufferCodecFactory.ByteBufferDecoder) {
            return new ChannelInputStream(((IoBuffer) msg).buf());
        } else {
            throw new IOException(
                    "If you want to use ChannelInputStream,please set CodecFactory to ByteBufferCodecFactory");
        }
    }

    public ChannelOutputStream getOutputStream() throws IOException {
        if (this.encoder instanceof ByteBufferCodecFactory.ByteBufferEncoder) {
            return new ChannelOutputStream(this, 0, false);
        } else {
            throw new IOException(
                    "If you want to use ChannelOutputStream,please set CodecFactory to ByteBufferCodecFactory");
        }
    }

    public ChannelOutputStream getOutputStream(final int capacity, final boolean direct) throws IOException {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity<0");
        }
        if (this.encoder instanceof ByteBufferCodecFactory.ByteBufferEncoder) {
            return new ChannelOutputStream(this, capacity, direct);
        } else {
            throw new IOException(
                    "If you want to use ChannelOutputStream,please set CodecFactory to ByteBufferCodecFactory");
        }
    }

    @Override
    protected final void closeChannel() throws IOException {
        // 优先关闭输出流
        try {
            if (this.selectableChannel instanceof SocketChannel) {
                final Socket socket = ((SocketChannel) this.selectableChannel).socket();
                try {
                    if (!socket.isClosed() && !socket.isOutputShutdown()) {
                        socket.shutdownOutput();
                    }
                    if (!socket.isClosed() && !socket.isInputShutdown()) {
                        socket.shutdownInput();
                    }
                } catch (final IOException e) {
                    // ignore
                }
                try {
                    socket.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        } finally {
            this.unregisterSession();
        }
    }

    @Override
    protected void onIdle0() {
        if (this.initialReadBufferSize > 0 && this.readBuffer.capacity() > this.initialReadBufferSize) {
            this.readBuffer =
                    IoBuffer.wrap(ByteBufferUtils.decreaseBufferCapatity(this.readBuffer.buf(), this.recvBufferSize,
                            this.initialReadBufferSize));

        }
    }

}