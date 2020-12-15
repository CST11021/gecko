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
package com.taobao.gecko.core.util;

import com.taobao.gecko.service.Dispatcher;
import com.taobao.gecko.service.PoolDispatcher;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.TimeUnit;

/**
 * 用于创建事件分发器的工厂
 */
public class DispatcherFactory {

    /**
     * 创建一个基于线程池的事件分发器
     *
     * @param size                      线程池的核心线程数
     * @param rejectedExecutionHandler  任务拒绝处理器策略
     * @return
     */
    public static Dispatcher newDispatcher(final int size, final RejectedExecutionHandler rejectedExecutionHandler) {
        if (size > 0) {
            return new PoolDispatcher(size, 60, TimeUnit.SECONDS, rejectedExecutionHandler);
        } else {
            return null;
        }
    }

    /**
     * 创建一个基于线程池的事件分发器
     *
     * @param size                      线程池的核心线程数
     * @param prefix                    线程池中线程名的前缀
     * @param rejectedExecutionHandler  任务拒绝处理器策略
     * @return
     */
    public static Dispatcher newDispatcher(final int size, final String prefix, final RejectedExecutionHandler rejectedExecutionHandler) {
        if (size > 0) {
            return new PoolDispatcher(size, 60, TimeUnit.SECONDS, prefix, rejectedExecutionHandler);
        } else {
            return null;
        }
    }

}