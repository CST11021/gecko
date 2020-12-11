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

/**
 * 会话生命周期处理器
 *
 * @author boyan
 * @since 1.0, 2009-12-16 下午06:00:24
 */
public interface Handler {

    /**
     * 创建会话对象时（会话对象初始化的时候）调用该方法
     *
     * @param session
     */
    void onSessionCreated(Session session);

    /**
     * 启动会话的时候调用
     *
     * @param session
     */
    void onSessionStarted(Session session);

    /**
     * 当建立了连接时会调用该方法
     *
     * @param session
     * @param args
     */
    void onSessionConnected(Session session, Object... args);

    /**
     * 处理会话接收到的消息
     *
     * @param session
     * @param msg
     */
    void onMessageReceived(Session session, Object msg);

    /**
     * 会话关闭（连接断开）的时候调用该方法
     *
     * @param session
     */
    void onSessionClosed(Session session);

    /**
     * 当消息发送出去之后，调用该方法
     *
     * @param session
     * @param msg
     */
    void onMessageSent(Session session, Object msg);

    /**
     * 网络IO异常的时候会调用该方法
     *
     * @param session
     * @param throwable
     */
    void onExceptionCaught(Session session, Throwable throwable);

    /**
     * 会话超时（即距离最近一次会话的网络IO处理时间超过了指定的时间）时调用该方法
     *
     * @param session
     */
    void onSessionExpired(Session session);

    void onSessionIdle(Session session);



}