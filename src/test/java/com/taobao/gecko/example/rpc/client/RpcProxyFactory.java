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
package com.taobao.gecko.example.rpc.client;

import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.example.rpc.command.RpcRequest;
import com.taobao.gecko.example.rpc.command.RpcResponse;
import com.taobao.gecko.example.rpc.RpcRuntimeException;
import com.taobao.gecko.example.rpc.transport.RpcWireFormatType;
import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 创建客户端的代理服务
 */
public class RpcProxyFactory {

    private final RemotingClient remotingClient;

    public RpcProxyFactory() throws IOException {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setWireFormatType(new RpcWireFormatType());
        this.remotingClient = RemotingFactory.newRemotingClient(clientConfig);
        try {
            this.remotingClient.start();
        } catch (NotifyRemotingException e) {
            throw new IOException(e);
        }
    }

    /**
     * 创建远程服务代理，客户端会通过该方法创建代理服务
     *
     * @param uri               调用服务的所有IP地址和端口
     * @param beanName          调用的目标服务beanName
     * @param serviceClass      目标服务beanName的类型
     * @param <T>
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @SuppressWarnings("unchecked")
    public <T> T proxyRemote(final String uri, final String beanName, Class<T> serviceClass) throws IOException, InterruptedException {
        try {
            this.remotingClient.connect(uri);
            this.remotingClient.awaitReadyInterrupt(uri);
        } catch (NotifyRemotingException e) {
            throw new IOException(e);
        }

        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{serviceClass}, new InvocationHandler() {

                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        // 1、创建请求对象
                        RpcRequest request = new RpcRequest(beanName, method.getName(), args);

                        // 2、向指定的服务发起请求
                        RpcResponse response = null;
                        try {
                            response = (RpcResponse) RpcProxyFactory.this.remotingClient.invokeToGroup(uri, request);
                        } catch (Exception e) {
                            throw new RpcRuntimeException("Rpc failure", e);
                        }

                        // 3、返回响应结果
                        if (response == null) {
                            throw new RpcRuntimeException("Rpc failure,no response from rpc server");
                        }

                        if (response.getResponseStatus() != ResponseStatus.NO_ERROR) {
                            throw new RpcRuntimeException("Rpc failure:" + response.getErrorMsg());
                        }

                        return response.getResult();
                    }
                });

    }

}