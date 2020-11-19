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
package com.taobao.gecko.example.rpc.example.client;

import com.taobao.gecko.example.rpc.client.RpcProxyFactory;
import com.taobao.gecko.example.rpc.example.Hello;


public class HelloClient {
    public static void main(String[] args) throws Exception {

        // RPC代理工厂
        RpcProxyFactory factory = new RpcProxyFactory();
        // 创建一个服务代理
        Hello hello = factory.proxyRemote("rpc://localhost:8080", "hello", Hello.class);

        // 发起远程服务调用
        String response = hello.sayHello("庄晓丹", 10000);
        System.out.println(response);


        hello.add(1, 300);

        hello.getDate();

    }
}