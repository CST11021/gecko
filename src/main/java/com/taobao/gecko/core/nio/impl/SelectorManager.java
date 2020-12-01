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
package com.taobao.gecko.core.nio.impl;

import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.core.EventType;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.util.PositiveAtomicCounter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * Selector管理器，管理多个reactor
 *
 * @author boyan
 * @since 1.0, 2009-12-16 下午06:10:59
 */
public class SelectorManager {

    private static final Log log = LogFactory.getLog(SelectorManager.class);

    /** Reactor保存在session中的key */
    public static final String REACTOR_ATTRIBUTE = System.currentTimeMillis() + "_Reactor_Attribute";

    /** 一个简单递增的统计器： */
    private final PositiveAtomicCounter sets = new PositiveAtomicCounter();
    /**
     * Reactor准备就绪的个数
     */
    private int reactorReadyCount;
    /** 用于标识SelectorManager是否启动 */
    private volatile boolean started;
    /** 表示为reactorSet.length - 1 */
    private final int dividend;
    /** 在SelectorManager的构造器中初始化该组件 */
    private final Reactor[] reactorSet;

    private final NioController controller;


    /**
     * 创建NioController实例和Reactor集合
     *
     * @param selectorPoolSize
     * @param controller
     * @param conf
     * @throws IOException
     */
    public SelectorManager(final int selectorPoolSize, final NioController controller, final Configuration conf) throws IOException {
        if (selectorPoolSize <= 0) {
            throw new IllegalArgumentException("selectorPoolSize<=0");
        }

        log.info("Creating " + selectorPoolSize + " rectors...");
        this.reactorSet = new Reactor[selectorPoolSize];
        this.controller = controller;
        // 创建selectorPoolSize个selector
        for (int i = 0; i < selectorPoolSize; i++) {
            this.reactorSet[i] = new Reactor(this, conf, i);
        }
        this.dividend = this.reactorSet.length - 1;
    }

    /**
     * 返回SelectorManager是否启动
     *
     * @return
     */
    public final boolean isStarted() {
        return this.started;
    }

    /**
     * 启动SelectorManager：启动Reactor
     */
    public synchronized void start() {
        if (this.started) {
            return;
        }
        this.started = true;
        for (final Reactor reactor : this.reactorSet) {
            reactor.start();
        }
    }

    /**
     * 停止SelectorManager：停止Reactor
     */
    public synchronized void stop() {
        if (!this.started) {
            return;
        }
        this.started = false;
        for (final Reactor reactor : this.reactorSet) {
            reactor.interrupt();
        }
    }

    /**
     * 通过Reactor将channel注册到selector
     *
     * @param channel       可选择的通道
     * @param ops           注册的事件类型
     * @param attachment
     * @return
     */
    public final Reactor registerChannel(final SelectableChannel channel, final int ops, final Object attachment) {
        // 等待Reactor准备好
        this.awaitReady();

        int index = 0;
        // Accept单独一个Reactor
        if (ops == SelectionKey.OP_ACCEPT || ops == SelectionKey.OP_CONNECT) {
            index = 0;
        } else {
            if (this.dividend > 0) {
                index = this.sets.incrementAndGet() % this.dividend + 1;
            } else {
                index = 0;
            }
        }

        final Reactor reactor = this.reactorSet[index];
        // 将channel注册到selector
        reactor.registerChannel(channel, ops, attachment);
        return reactor;
    }

    /**
     * 注册连接事件
     *
     * @param session
     * @param event
     */
    public final void registerSession(final Session session, final EventType event) {
        final Reactor reactor = this.getReactorFromSession(session);
        reactor.registerSession(session, event);
    }







    /**
     * 等待Reactor准备好
     */
    void awaitReady() {
        synchronized (this) {
            while (!this.started || this.reactorReadyCount != this.reactorSet.length) {
                try {
                    this.wait(1000);
                } catch (final InterruptedException e) {
                    // reset interrupt status
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 从Reactor集合获取一个Reactor实例
     *
     * @return
     */
    final Reactor nextReactor() {
        if (this.dividend > 0) {
            return this.reactorSet[this.sets.incrementAndGet() % this.dividend + 1];
        } else {
            return this.reactorSet[0];
        }
    }

    /**
     * 从session的属性获取Reactor对象，如果为null，则创建一个Reactor对象
     *
     * @param session
     * @return
     */
    Reactor getReactorFromSession(final Session session) {
        Reactor reactor = (Reactor) session.getAttribute(REACTOR_ATTRIBUTE);

        if (reactor == null) {
            reactor = this.nextReactor();
            final Reactor oldReactor = (Reactor) session.setAttributeIfAbsent(REACTOR_ATTRIBUTE, reactor);
            if (oldReactor != null) {
                reactor = oldReactor;
            }
        }
        return reactor;
    }

    /**
     * 将定时器检测连接建立是否成功的TimerRef放到session对应的Reactor实例中
     *
     * @param session
     * @param timerRef
     */
    public final void insertTimer(final Session session, final TimerRef timerRef) {
        final Reactor reactor = this.getReactorFromSession(session);
        // 将TimerRef实例添加Reactor#timerQueue的双向队列中
        reactor.insertTimer(timerRef);
    }

    /**
     * 随机选择一个Reactor实例，并将TimerRef实例添加Reactor#timerQueue的双向队列中
     *
     * @param timerRef
     */
    public final void insertTimer(final TimerRef timerRef) {
        // 将TimerRef实例添加Reactor#timerQueue的双向队列中
        this.nextReactor().insertTimer(timerRef);
    }

    /**
     * 获取
     *
     * @return
     */
    public NioController getController() {
        return this.controller;
    }

    /**
     * Reactor线程即将开始指定前调用该方法，让其他相关组件做好准备
     */
    synchronized void notifyReady() {
        this.reactorReadyCount++;
        if (this.reactorReadyCount == this.reactorSet.length) {
            // 通知所有的Controller生命周期监听器准备好
            this.controller.notifyReady();
            this.notifyAll();
        }
    }

    /**
     * 获取Reactor集合的大小
     *
     * @return
     */
    public int getSelectorCount() {
        return this.reactorSet == null ? 0 : this.reactorSet.length;
    }

    /**
     * 仅用于测试
     *
     * @param index
     * @return
     */
    Reactor getReactorByIndex(final int index) {
        if (index < 0 || index > this.reactorSet.length - 1) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return this.reactorSet[index];
    }

}