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
 * 网络层IO主控接口
 *
 * @author boyan
 * @since 1.0, 2009-12-16 下午05:57:49
 */
public interface Controller {

    void start() throws IOException;
    boolean isStarted();

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

    Statistics getStatistics();

    <T> void setSocketOption(SocketOption<T> socketOption, T value);

    //

    long getSessionTimeout();
    void setSessionTimeout(long sessionTimeout);

    public long getSessionIdleTimeout();
    public void setSessionIdleTimeout(long sessionIdleTimeout);

    int getSoTimeout();
    void setSoTimeout(int timeout);

    void addStateListener(ControllerStateListener listener);
    public void removeStateListener(ControllerStateListener listener);

    boolean isHandleReadWriteConcurrently();
    void setHandleReadWriteConcurrently(boolean handleReadWriteConcurrently);



    /** 会话生命周期处理 */
    Handler getHandler();
    void setHandler(Handler handler);



    CodecFactory getCodecFactory();
    void setCodecFactory(CodecFactory codecFactory);

    void setReceiveThroughputLimit(double receivePacketRate);
    double getReceiveThroughputLimit();

    int getDispatchMessageThreadCount();
    void setDispatchMessageThreadCount(int dispatchMessageThreadPoolSize);

    int getReadThreadCount();
    void setReadThreadCount(int readThreadCount);

    int getWriteThreadCount();
    void setWriteThreadCount(int writeThreadCount);




}