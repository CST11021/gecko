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
/**
 * Copyright [2009-2010] [dennis zhuang]
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package com.taobao.gecko.core.nio;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.EventType;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.nio.impl.TimerRef;

import java.nio.channels.FileChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.util.concurrent.Future;

/**
 * 基于Nio实现的会话接口
 *
 * @author boyan
 *
 */
public interface NioSession extends Session {

    /**
     * 派发IO事件
     *
     * @param event     对应session的事件类型
     * @param selector  当前连接对应的选择器，用于注册相关事件
     */
    public void onEvent(EventType event, Selector selector);

    /**
     * 注册读
     *
     * @param selector
     */
    public void enableRead(Selector selector);

    /**
     * 注册写
     *
     * @param selector
     */
    public void enableWrite(Selector selector);

    /**
     * 通过selectorManager将异步的请求回调器注册到reactor
     *
     * @param timerRef
     */
    public void insertTimer(TimerRef timerRef);

    /**
     * 获得连接对应的channel
     *
     * @return
     */
    public SelectableChannel channel();



    // 发送消息

    /**
     * 往该连接写入消息，可被中断，中断可能引起连接的关闭，慎重使用
     *
     * @param message
     */
    public void writeInterruptibly(Object message);

    /**
     * 往该连接写入消息，可被中断，中断可能引起连接的关闭，慎重使用
     *
     * @param message
     */
    public Future<Boolean> asyncWriteInterruptibly(Object message);

    /**
     * 从指定FileChannel的position位置开始传输size个字节到socket，其中head和tail是在传输文件前后写入的数据，可以为null
     *
     * @param src
     * @param position
     * @param size
     */
    public Future<Boolean> transferFrom(IoBuffer head, IoBuffer tail, FileChannel src, long position, long size);

    /**
     * 从指定FileChannel的position位置开始传输size个字节到socket，返回future对象查询状态, 其中head和tail是在传输文件前后写入的数据，可以为null
     *
     * @param src
     * @param position
     * @param size
     * @return
     */
    public Future<Boolean> asyncTransferFrom(IoBuffer head, IoBuffer tail, FileChannel src, long position, long size);

}