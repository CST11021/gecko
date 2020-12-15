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
package com.taobao.gecko.core.message;

import com.taobao.gecko.core.future.FutureImpl;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * 发送消息包装类，通过该类将消息对象转为字节写入channel
 *
 * @author boyan
 * @since 1.0, 2009-12-16 下午06:02:37
 */
public interface WriteMessage {

    void writing();

    boolean isWriting();

    /**
     * 获取原始消息对象，原始的消息对象会被封装为WriteMessage
     *
     * @return
     */
    Object getMessage();

    /**
     * 返回是否还有未被写入通道的字节
     *
     * @return
     */
    boolean hasRemaining();

    /**
     * 返回该消息还剩余多少字节未被写入通道
     *
     * @return
     */
    long remaining();

    /**
     * 将WriteMessage消息对象写入指定的通道
     *
     * @param channel
     * @return
     * @throws IOException
     */
    long write(WritableByteChannel channel) throws IOException;

    FutureImpl<Boolean> getWriteFuture();

}