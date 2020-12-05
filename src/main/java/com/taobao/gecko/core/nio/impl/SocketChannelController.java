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
import com.taobao.gecko.core.core.*;
import com.taobao.gecko.core.core.impl.StandardSocketOption;
import com.taobao.gecko.core.nio.NioSession;
import com.taobao.gecko.core.nio.NioSessionConfig;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;


/**
 * Nio tcp实现
 *
 * @author boyan
 * @since 1.0, 2009-12-16 下午06:18:00
 */
public abstract class SocketChannelController extends NioController {

    protected boolean soLingerOn = false;

    public SocketChannelController() {
        super();
    }
    public SocketChannelController(final Configuration configuration) {
        super(configuration, null, null);

    }
    public SocketChannelController(final Configuration configuration, final CodecFactory codecFactory) {
        super(configuration, null, codecFactory);
    }
    public SocketChannelController(final Configuration configuration, final Handler handler, final CodecFactory codecFactory) {
        super(configuration, handler, codecFactory);
    }

    public void setSoLinger(final boolean on, final int value) {
        this.soLingerOn = on;
        // SO_LINGER选项用来设置延迟关闭的时间，等待套接字发送缓冲区中的数据发送完成。没有设置该选项时，在调用close()后，
        // 在发送完FIN后会立即进行一些清理工作并返回。如果设置了SO_LINGER选项，并且等待时间为正值，则在清理之前会等待一段时间。
        this.socketOptions.put(StandardSocketOption.SO_LINGER, value);
    }

    /**
     * Dispatch read event
     *
     * @param key
     * @return
     */
    @Override
    protected final void dispatchReadEvent(final SelectionKey key) {
        final Session session = (Session) key.attachment();
        if (session != null) {
            // 派发IO事件
            ((NioSession) session).onEvent(EventType.READABLE, key.selector());
        } else {
            log.warn("Could not find session for readable event,maybe it is closed");
        }
    }

    /**
     * Dispatch write event
     *
     * @param key
     * @return
     */
    @Override
    protected final void dispatchWriteEvent(final SelectionKey key) {
        final Session session = (Session) key.attachment();
        if (session != null) {
            // 派发IO事件
            ((NioSession) session).onEvent(EventType.WRITEABLE, key.selector());
        } else {
            log.warn("Could not find session for writable event,maybe it is closed");
        }

    }

    /**
     * 创建一个NioSession会话对象
     *
     * @param sc
     * @return
     */
    protected NioSession buildSession(final SocketChannel sc) {
        final Queue<WriteMessage> queue = this.buildQueue();
        final NioSessionConfig sessionConfig = this.buildSessionConfig(sc, queue);
        final NioSession session = new NioTCPSession(sessionConfig, this.configuration.getSessionReadBufferSize());
        return session;
    }

    /**
     * 配置socket通道
     *
     * @param sc
     * @throws IOException
     */
    protected final void configureSocketChannel(final SocketChannel sc) throws IOException {
        sc.socket().setSoTimeout(this.soTimeout);
        sc.configureBlocking(false);
        if (this.socketOptions.get(StandardSocketOption.SO_REUSEADDR) != null) {
            sc.socket().setReuseAddress(
                    StandardSocketOption.SO_REUSEADDR.type()
                            .cast(this.socketOptions.get(StandardSocketOption.SO_REUSEADDR)));
        }
        if (this.socketOptions.get(StandardSocketOption.SO_SNDBUF) != null) {
            sc.socket().setSendBufferSize(
                    StandardSocketOption.SO_SNDBUF.type().cast(this.socketOptions.get(StandardSocketOption.SO_SNDBUF)));
        }
        if (this.socketOptions.get(StandardSocketOption.SO_KEEPALIVE) != null) {
            sc.socket().setKeepAlive(
                    StandardSocketOption.SO_KEEPALIVE.type()
                            .cast(this.socketOptions.get(StandardSocketOption.SO_KEEPALIVE)));
        }
        if (this.socketOptions.get(StandardSocketOption.SO_LINGER) != null) {
            sc.socket().setSoLinger(this.soLingerOn,
                    StandardSocketOption.SO_LINGER.type().cast(this.socketOptions.get(StandardSocketOption.SO_LINGER)));
        }
        if (this.socketOptions.get(StandardSocketOption.SO_RCVBUF) != null) {
            sc.socket().setReceiveBufferSize(
                    StandardSocketOption.SO_RCVBUF.type().cast(this.socketOptions.get(StandardSocketOption.SO_RCVBUF)));

        }
        if (this.socketOptions.get(StandardSocketOption.TCP_NODELAY) != null) {
            sc.socket().setTcpNoDelay(
                    StandardSocketOption.TCP_NODELAY.type().cast(this.socketOptions.get(StandardSocketOption.TCP_NODELAY)));
        }
    }

}