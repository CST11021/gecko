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
package com.taobao.gecko.example.rpc.command;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.util.OpaqueGenerator;
import com.taobao.gecko.example.rpc.transport.RpcCodecFactory;

import java.io.*;


public class RpcRequest implements RequestCommand, CommandHeader, RpcCommand {

    static final long serialVersionUID = -1L;

    private Integer opaque;

    /** 表示调用的服务端的目标bean */
    private String beanName;
    /** 表示调用的服务方法 */
    private String methodName;
    /** 方法入参 */
    private Object[] arguments;

    public RpcRequest() {
        super();
    }
    public RpcRequest(final String beanName, final String methodName, final Object[] arguments) {
        super();
        this.opaque = OpaqueGenerator.getNextOpaque();
        this.beanName = beanName;
        this.methodName = methodName;
        this.arguments = arguments;
    }


    // 实现：RpcCommand

    /**
     * 将请求对象进行编码（转为字节）以便后续的序列化操作
     *
     * @return
     */
    public IoBuffer encode() {
        byte[] argumentsData = null;
        if (this.arguments != null && this.arguments.length > 0) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                final ObjectOutputStream objOut = new ObjectOutputStream(out);
                for (final Object arg : this.arguments) {
                    objOut.writeObject(arg);
                }
                out.close();
                argumentsData = out.toByteArray();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }

        }
        final IoBuffer buffer =
                IoBuffer.allocate(1 + 4 + 4 + this.beanName.length() + 4 + this.methodName.length() + 4
                        + (argumentsData != null ? 4 : 0) + (argumentsData != null ? argumentsData.length : 0));
        buffer.put(RpcCodecFactory.REQ_MAGIC);
        buffer.putInt(this.opaque);
        buffer.putInt(this.beanName.length());
        buffer.put(this.beanName.getBytes());
        buffer.putInt(this.methodName.length());
        buffer.put(this.methodName.getBytes());
        buffer.putInt(this.arguments == null ? 0 : this.arguments.length);
        if (argumentsData != null) {
            buffer.putInt(argumentsData.length);
            buffer.put(argumentsData);
        }
        buffer.flip();
        return buffer;
    }
    /**
     * 将字节进行解码
     *
     * @param buffer
     * @return
     */
    public boolean decode(final IoBuffer buffer) {
        buffer.mark();
        if (buffer.remaining() >= 4) {
            this.setOpaque(buffer.getInt());
            if (buffer.remaining() >= 4) {
                final int beanNameLen = buffer.getInt();
                if (buffer.remaining() >= beanNameLen) {
                    byte[] data = new byte[beanNameLen];
                    buffer.get(data);
                    this.setBeanName(new String(data));
                    if (buffer.remaining() >= 4) {
                        final int methodNameLen = buffer.getInt();
                        if (buffer.remaining() >= methodNameLen) {
                            data = new byte[methodNameLen];
                            buffer.get(data);
                            this.setMethodName(new String(data));
                            if (buffer.remaining() >= 4) {
                                if (this.decodeArguments(buffer)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

        }
        buffer.reset();
        return false;
    }


    private boolean decodeArguments(final IoBuffer buffer) {
        byte[] data;
        final int argumentCount = buffer.getInt();
        if (argumentCount > 0) {
            this.arguments = new Object[argumentCount];
            if (buffer.remaining() >= 4) {
                final int argumentDataLen = buffer.getInt();
                if (argumentDataLen == 0) {
                    return true;
                }
                if (buffer.remaining() >= argumentDataLen) {
                    data = new byte[argumentDataLen];
                    buffer.get(data);
                    final ByteArrayInputStream in = new ByteArrayInputStream(data);
                    try {
                        final ObjectInputStream objIn = new ObjectInputStream(in);
                        for (int i = 0; i < argumentCount; i++) {
                            this.arguments[i] = objIn.readObject();
                        }
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            in.close();
                        } catch (final IOException e) {
                            // ignore
                        }
                    }
                    return true;
                }
                return false;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
    public CommandHeader getRequestHeader() {
        return this;
    }


    // getter and setter ...

    public void setOpaque(final Integer opaque) {
        this.opaque = opaque;
    }
    public Integer getOpaque() {
        return this.opaque;
    }
    public String getBeanName() {
        return this.beanName;
    }
    public void setBeanName(final String beanName) {
        this.beanName = beanName;
    }
    public void setMethodName(final String methodName) {
        this.methodName = methodName;
    }
    public void setArguments(final Object[] arguments) {
        this.arguments = arguments;
    }
    public String getMethodName() {
        return this.methodName;
    }
    public Object[] getArguments() {
        return this.arguments;
    }



    public static void main(final String[] args) {
        final RpcRequest request = new RpcRequest("hello", "hello", new Object[]{"dennis", 26});
        final IoBuffer buffer = request.encode();
        buffer.get();
        System.out.println(request.decode(buffer));
    }

}