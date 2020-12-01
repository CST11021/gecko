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
package com.taobao.gecko.example.rpc.transport;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.example.rpc.command.RpcCommand;
import com.taobao.gecko.example.rpc.command.RpcRequest;
import com.taobao.gecko.example.rpc.command.RpcResponse;

/**
 * @author boyan
 * @Date 2011-2-17
 */
public class RpcCodecFactory implements CodecFactory {

    static final byte REQ_MAGIC = (byte) 0x70;
    static final byte RESP_MAGIC = (byte) 0x71;

    static final class RpcDecoder implements Decoder {

        private static final String CURRENT_COMMAND = "CurrentCommand";

        /**
         * 对buffer进行解码（反序列化），并返回对应的对象
         *
         * @param buff
         * @param session
         * @return
         */
        public Object decode(IoBuffer buff, Session session) {
            // TODO whz 这个后面再看看
            if (!buff.hasRemaining()) {
                return null;
            }

            // 1、优先从会话属性获取当前的请求或者响应的对象进行解码
            RpcCommand command = (RpcCommand) session.getAttribute(CURRENT_COMMAND);
            if (command != null) {
                System.out.println("会话中居然有command命令？？？？？？");
                if (command.decode(buff)) {
                    session.removeAttribute(CURRENT_COMMAND);
                    return command;
                }

                return null;
            }

            // 2、如果会话属性没有标记当前的消息类型，则根据缓冲区中的magic判断是请求对象还是响应对象
            byte magic = buff.get();
            if (magic == REQ_MAGIC) {
                command = new RpcRequest();
            } else {
                command = new RpcResponse();
            }

            if (command.decode(buff)) {
                return command;
            }

            session.setAttribute(CURRENT_COMMAND, command);
            return null;
        }

    }

    static final class RpcEncoder implements Encoder {

        /**
         * 将消息对象进行编码（序列化），获取对应的字节缓冲区
         *
         * @param message
         * @param session
         * @return
         */
        public IoBuffer encode(Object message, Session session) {
            return ((RpcCommand) message).encode();
        }

    }


    /**
     * RPC解码器
     *
     * @return
     */
    public Decoder getDecoder() {
        return new RpcDecoder();
    }

    /**
     * RPC编码器
     *
     * @return
     */
    public Encoder getEncoder() {
        return new RpcEncoder();
    }

}