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
package com.taobao.gecko.core.config;

/**
 * 用于创建SocketChannelController对象的配置类
 *
 * @author dennis
 *
 */
public class Configuration {

    public static final int DEFAULT_INCREASE_BUFF_SIZE = 32 * 1024;

    public static final int MAX_READ_BUFFER_SIZE = Integer.parseInt(System.getProperty("notify.remoting.max_read_buffer_size", "2097152"));


    /** 从会话读取信息的缓存区大小 */
    private int sessionReadBufferSize = 32 * 1024;

    private int soTimeout = 0;

    private boolean handleReadWriteConcurrently = true;

    private int readThreadCount = 0;

    private int writeThreadCount = 0;

    private int dispatchMessageThreadCount = 0;

    /** session空闲超时时间，默认5秒 */
    private volatile long sessionIdleTimeout = 5000L;

    /** 检查session是否超时的间隔时间，默认是1秒 */
    private volatile long checkSessionTimeoutInterval = 1000L;

    /** 是否开启统计组件 */
    private boolean statisticsServer = false;

    /** 统计周期：统计器的统计间隔时间，默认5分钟 */
    protected long statisticsInterval = 5 * 60 * 1000L;







    public final int getWriteThreadCount() {
        return writeThreadCount;
    }
    public final int getDispatchMessageThreadCount() {
        return dispatchMessageThreadCount;
    }
    public final void setDispatchMessageThreadCount(final int dispatchMessageThreadCount) {
        this.dispatchMessageThreadCount = dispatchMessageThreadCount;
    }
    public final void setWriteThreadCount(final int writeThreadCount) {
        this.writeThreadCount = writeThreadCount;
    }
    public final long getSessionIdleTimeout() {
        return sessionIdleTimeout;
    }
    public final void setSessionIdleTimeout(final long sessionIdleTimeout) {
        this.sessionIdleTimeout = sessionIdleTimeout;
    }
    public final int getSessionReadBufferSize() {
        return sessionReadBufferSize;
    }
    public final boolean isHandleReadWriteConcurrently() {
        return handleReadWriteConcurrently;
    }
    public final int getSoTimeout() {
        return soTimeout;
    }
    public final long getStatisticsInterval() {
        return statisticsInterval;
    }
    public final void setStatisticsInterval(final long statisticsInterval) {
        this.statisticsInterval = statisticsInterval;
    }
    public final void setSoTimeout(final int soTimeout) {
        if (soTimeout < 0) {
            throw new IllegalArgumentException("soTimeout<0");
        }
        this.soTimeout = soTimeout;
    }
    public final void setHandleReadWriteConcurrently(final boolean handleReadWriteConcurrently) {
        this.handleReadWriteConcurrently = handleReadWriteConcurrently;
    }
    public final void setSessionReadBufferSize(final int tcpHandlerReadBufferSize) {
        if (tcpHandlerReadBufferSize <= 0) {
            throw new IllegalArgumentException("tcpHandlerReadBufferSize<=0");
        }
        sessionReadBufferSize = tcpHandlerReadBufferSize;
    }
    public final boolean isStatisticsServer() {
        return statisticsServer;
    }
    public final void setStatisticsServer(final boolean statisticsServer) {
        this.statisticsServer = statisticsServer;
    }
    public final int getReadThreadCount() {
        return readThreadCount;
    }
    public final void setReadThreadCount(final int readThreadCount) {
        if (readThreadCount < 0) {
            throw new IllegalArgumentException("readThreadCount<0");
        }
        this.readThreadCount = readThreadCount;
    }
    public void setCheckSessionTimeoutInterval(final long checkSessionTimeoutInterval) {
        this.checkSessionTimeoutInterval = checkSessionTimeoutInterval;
    }
    public long getCheckSessionTimeoutInterval() {
        return checkSessionTimeoutInterval;
    }

}