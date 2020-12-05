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

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Controller是网络层IO主控接口，其客户端连接器和服务端的实现都继承该接口
 *
 * 客户端网络IO实现：
 *      GeckoTCPConnectorController
 *      TCPConnectorController
 *      UDPConnectorController
 *
 * 服务端网络IO实现：
 *      TCPController
 *      UDPController
 *
 * @author boyan
 * @since 1.0, 2009-12-16 下午05:57:49
 */
public interface Controller {

    /**
     * 启动客户端或者服务端
     *
     * @throws IOException
     */
    void start() throws IOException;

    /**
     * 返回客户端或服务端是否启动
     *
     * @return
     */
    boolean isStarted();

    /**
     * 停止客户端或者服务端
     *
     * @throws IOException
     */
    void stop() throws IOException;


    /**
     * 应用服务的本地端口，可能是客户端的端口也可能是服务端的端口
     *
     * @return
     */
    int getPort();
    /**
     * 获取绑定的本地InetSocketAddress
     *
     * @return
     */
    InetSocketAddress getLocalSocketAddress();
    /**
     * 设置本地InetSocketAddress
     *
     * @param inetAddress
     */
    void setLocalSocketAddress(InetSocketAddress inetAddress);

    /**
     * 获取统计器组件
     *
     * @return
     */
    Statistics getStatistics();

    /**
     * 设置Socket选项
     *
     * @param socketOption
     * @param value
     * @param <T>
     */
    <T> void setSocketOption(SocketOption<T> socketOption, T value);

    //

    long getSessionTimeout();
    void setSessionTimeout(long sessionTimeout);

    long getSessionIdleTimeout();
    void setSessionIdleTimeout(long sessionIdleTimeout);

    int getSoTimeout();
    void setSoTimeout(int timeout);

    /**
     * 添加Controller生命周期监听器
     *
     * @param listener
     */
    void addStateListener(ControllerStateListener listener);

    /**
     * 移除Controller生命周期监听器
     *
     * @param listener
     */
    void removeStateListener(ControllerStateListener listener);

    boolean isHandleReadWriteConcurrently();
    void setHandleReadWriteConcurrently(boolean handleReadWriteConcurrently);



    /** 会话生命周期处理器 */
    Handler getHandler();
    void setHandler(Handler handler);



    CodecFactory getCodecFactory();
    void setCodecFactory(CodecFactory codecFactory);

    void setReceiveThroughputLimit(double receivePacketRate);
    double getReceiveThroughputLimit();

    int getDispatchMessageThreadCount();
    void setDispatchMessageThreadCount(int dispatchMessageThreadPoolSize);

    /**
     * readEventDispatcher派发器的线程数
     *
     * @return
     */
    int getReadThreadCount();

    /**
     * readEventDispatcher派发器的线程数
     *
     * @param readThreadCount
     */
    void setReadThreadCount(int readThreadCount);

    /**
     * writeEventDispatcher派发器的线程数
     *
     * @return
     */
    int getWriteThreadCount();

    /**
     * writeEventDispatcher派发器的线程数
     *
     * @param writeThreadCount
     */
    void setWriteThreadCount(int writeThreadCount);




}