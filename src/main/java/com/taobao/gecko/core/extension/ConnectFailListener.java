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
package com.taobao.gecko.core.extension;


/**
 * 扩展监听器，监听连接失败事件
 *
 * @author dennis
 */
public interface ConnectFailListener {

    /**
     * 当客户端与服务端建立连接失败时会调用该方法
     *
     * @param args
     */
    public void onConnectFail(Object... args);
}