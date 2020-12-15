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
package com.taobao.gecko.service.nio;

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
import com.taobao.gecko.remoting.controller.AbstractController;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * SelectionKey处理器
 *
 * @see AbstractController
 * @author dennis
 *
 */
public interface SelectionKeyHandler {

    /**
     * 当服务端接收到客户端的连接请求时，会调用该方法
     *
     * @param sk
     * @throws IOException
     */
    public void onAccept(SelectionKey sk) throws IOException;

    /**
     * 表示客户端与服务端建立的连接时调用
     *
     * @param key
     * @throws IOException
     */
    public void onConnect(SelectionKey key) throws IOException;

    /**
     * 当通道是可写的时候调用
     *
     * @param key
     */
    public void onWrite(SelectionKey key);

    /**
     * 当通道是可读取的时候调用
     *
     * @param key
     */
    public void onRead(SelectionKey key);

    /**
     * 执行TimerRef#runnable线程
     *
     * @param timerRef
     */
    public void onTimeout(TimerRef timerRef);

    /**
     * 关闭SelectionKey时调用
     *
     * @param key
     */
    public void closeSelectionKey(SelectionKey key);

    /**
     * 关闭Channel时调用
     *
     * @param selector
     * @throws IOException
     */
    public void closeChannel(Selector selector) throws IOException;

}