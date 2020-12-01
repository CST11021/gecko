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
package com.taobao.gecko.core.core;

import com.taobao.gecko.core.statistics.Statistics;

import java.util.Queue;


/**
 * 连接配置
 *
 * @author boyan
 * @since 1.0, 2009-12-16 下午06:01:37
 */
public class SessionConfig {

    /** 会话生命周期处理器 */
    public final Handler handler;
    /** 消息编解码工厂 */
    public final CodecFactory codecFactory;
    /** 统计管理器 */
    public final Statistics statistics;
    /** 网session写消息的消息队列：WriteMessage是发送消息的包装类 */
    public final Queue<WriteMessage> queue;
    /** 发送消息的派发器 */
    public final Dispatcher dispatchMessageDispatcher;
    public final boolean handleReadWriteConcurrently;
    /** session的超时时间 */
    public final long sessionTimeout;
    /** session闲置的超时时间 */
    public final long sessionIdelTimeout;


    public SessionConfig(final Handler handler, final CodecFactory codecFactory, final Statistics statistics,
                         final Queue<WriteMessage> queue, final Dispatcher dispatchMessageDispatcher,
                         final boolean handleReadWriteConcurrently, final long sessionTimeout, final long sessionIdelTimeout) {

        this.handler = handler;
        this.codecFactory = codecFactory;
        this.statistics = statistics;
        this.queue = queue;
        this.dispatchMessageDispatcher = dispatchMessageDispatcher;
        this.handleReadWriteConcurrently = handleReadWriteConcurrently;
        this.sessionTimeout = sessionTimeout;
        this.sessionIdelTimeout = sessionIdelTimeout;
    }
}