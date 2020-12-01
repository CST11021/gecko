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

import com.taobao.gecko.core.command.CommandFactory;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.service.config.WireFormatType;


public class RpcWireFormatType extends WireFormatType {

    private static final String SCHEME = "rpc";

    private static final String NAME = "Notify Remoting rpc";

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public String name() {
        return NAME;
    }

    /**
     * 协议的编解码工厂
     *
     * @return
     */
    @Override
    public CodecFactory newCodecFactory() {
        return new RpcCodecFactory();
    }

    /**
     * 协议的命令工厂：用于创建心跳请求和正常ack请求的命令对象
     *
     * @return
     */
    @Override
    public CommandFactory newCommandFactory() {
        return new RpcCommandFactory();
    }

}