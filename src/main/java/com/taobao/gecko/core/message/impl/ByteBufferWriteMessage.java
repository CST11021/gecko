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
package com.taobao.gecko.core.message.impl;

import com.taobao.gecko.core.message.WriteMessage;
import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.future.FutureImpl;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;


/**
 * 发送消息包装实现
 *
 * @author boyan
 * @since 1.0, 2009-12-16 下午06:06:01
 */
public class ByteBufferWriteMessage implements WriteMessage {

    /** 表示消息对象 */
    protected Object message;

    /** 对应#message对象编码后的字节，即后续要写入通道的字节缓存区 */
    protected IoBuffer buffer;

    protected FutureImpl<Boolean> writeFuture;

    /** 表示该消息对象是否正在被写入通道 */
    protected volatile boolean writing;

    public ByteBufferWriteMessage(final Object message, final FutureImpl<Boolean> writeFuture) {
        this.message = message;
        this.writeFuture = writeFuture;
    }

    /**
     * 标记消息是否开始正在写入通道
     */
    public final void writing() {
        this.writing = true;
    }

    /**
     * 返回该消息对象是否正在被写入通道
     *
     * @return
     */
    public final boolean isWriting() {
        return this.writing;
    }

    /**
     * 返回该消息还剩余多少字节未被写入通道
     *
     * @return
     */
    public long remaining() {
        return this.buffer == null ? 0 : this.buffer.remaining();
    }

    /**
     * 返回是否还有未被写入通道的字节
     *
     * @return
     */
    public boolean hasRemaining() {
        return this.buffer != null && this.buffer.hasRemaining();
    }

    /**
     * 获取要写入通道的消息的字节缓冲区
     *
     * @return
     */
    public synchronized final IoBuffer getWriteBuffer() {
        return this.buffer;
    }

    /**
     * 将保存消息字节的缓冲区写入通道
     *
     * @param channel
     * @return 返回写入的字节数
     * @throws IOException
     */
    public long write(final WritableByteChannel channel) throws IOException {
        return channel.write(this.buffer.buf());
    }

    /**
     * 设置要写入通道的消息缓冲区
     *
     * @param buffers
     */
    public synchronized final void setWriteBuffer(final IoBuffer buffers) {
        this.buffer = buffers;

    }

    /**
     * 返回标记消息是否写入完成的future
     *
     * @return
     */
    public final FutureImpl<Boolean> getWriteFuture() {
        return this.writeFuture;
    }

    /**
     * 获取原始消息对象，原始的消息对象会被封装为WriteMessage
     *
     * @return
     */
    public final Object getMessage() {
        return this.message;
    }

}