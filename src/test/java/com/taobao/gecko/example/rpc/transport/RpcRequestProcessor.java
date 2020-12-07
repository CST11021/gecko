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

import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.example.rpc.command.RpcRequest;
import com.taobao.gecko.example.rpc.command.RpcResponse;
import com.taobao.gecko.example.rpc.example.server.BeanFactory;
import com.taobao.gecko.example.rpc.server.InvokeUtil;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RequestProcessor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 请求处理器
 */
public class RpcRequestProcessor implements RequestProcessor<RpcRequest> {

    private final ThreadPoolExecutor executor;

    private final BeanFactory beanFactory;


    public RpcRequestProcessor(ThreadPoolExecutor executor) {
        super();
        this.executor = executor;
        this.beanFactory = new BeanFactory();
    }


    public ThreadPoolExecutor getExecutor() {
        return this.executor;
    }

    /**
     * 处理请求：
     * 1、通过BeanLocator获取服务实现；
     * 2、
     *
     * @param request 请求命令
     * @param conn    请求来源的连接
     */
    public void handleRequest(RpcRequest request, Connection conn) {
        Object bean = this.beanFactory.getBeanByName(request.getBeanName());
        if (bean == null) {
            throw new RuntimeException("Could not find bean named " + request.getBeanName());
        }

        // 调用指定服务的目标方法
        Object result = InvokeUtil.invoke(bean, request.getMethodName(), request.getArguments());

        try {
            // 回写响应数据
            conn.response(new RpcResponse(request.getOpaque(), ResponseStatus.NO_ERROR, result));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}