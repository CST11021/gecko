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
package com.taobao.gecko.remoting.controller.connector;

import com.taobao.gecko.remoting.RemotingClient;
import com.taobao.gecko.service.config.Configuration;
import com.taobao.gecko.core.codec.CodecFactory;
import com.taobao.gecko.service.handler.Handler;
import com.taobao.gecko.service.session.SessionEventType;
import com.taobao.gecko.core.future.FutureImpl;
import com.taobao.gecko.remoting.controller.SocketChannelController;
import com.taobao.gecko.service.session.NioSession;
import com.taobao.gecko.service.connection.ConnectFailListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Gecko的连接管理器，扩展SocketChannelController，提供单个Controller管理多个客户端连接功能
 *
 * @author boyan
 * @since 1.0, 2009-12-16 下午05:56:50
 */
public class GeckoTCPConnectorController extends SocketChannelController {

    /** 连接失败监听器 */
    private ConnectFailListener connectFailListener;

    public GeckoTCPConnectorController(final RemotingClient remotingClient) {
        super();
    }
    public GeckoTCPConnectorController(final Configuration configuration, final CodecFactory codecFactory) {
        super(configuration, codecFactory);
    }
    public GeckoTCPConnectorController(final Configuration configuration, final Handler handler, final CodecFactory codecFactory) {
        super(configuration, handler, codecFactory);
    }
    public GeckoTCPConnectorController(final Configuration configuration) {
        super(configuration);
    }


    @Override
    protected void doStart() throws IOException {
        // do nothing
    }

    /**
     * 与指定的服务建立连接，并返回一个会话对象
     *
     * @param remoteAddress 目标服务地址
     * @param args
     * @return
     * @throws IOException
     */
    public FutureImpl<NioSession> connect(final InetSocketAddress remoteAddress, final Object... args) throws IOException {
        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open();
            // 配置socket通道
            this.configureSocketChannel(socketChannel);

            final FutureImpl<NioSession> resultFuture = new FutureImpl<NioSession>(args);
            if (!socketChannel.connect(remoteAddress)) {
                // 将channel注册到selector
                this.selectorManager.registerChannel(socketChannel, SelectionKey.OP_CONNECT, resultFuture);
            } else {
                // 创建Session
                final NioSession session = this.createSession(socketChannel, args);
                resultFuture.setResult(session);
            }

            return resultFuture;
        } catch (final IOException e) {
            if (socketChannel != null) {
                socketChannel.close();
            }
            throw e;
        }
    }

    public void closeChannel(final Selector selector) throws IOException {

    }

    /**
     * 当通道注册到selector后，客户端与服务端建立连接时（即：SelectionKey.OP_CONNECT事件发生时）调用该方法
     *
     * @param key
     * @throws IOException
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onConnect(final SelectionKey key) throws IOException {
        key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
        final FutureImpl<NioSession> future = (FutureImpl<NioSession>) key.attachment();
        key.attach(null);
        try {
            if (!((SocketChannel) key.channel()).finishConnect()) {
                throw new IOException("Connect Fail");
            }

            future.setResult(this.createSession((SocketChannel) key.channel(), future.getArgs()));
        } catch (final Exception e) {
            this.cancelKey(key);
            future.failure(e);
            log.error(e, e);
            // 通知连接失败
            if (this.connectFailListener != null) {
                this.connectFailListener.onConnectFail(future.getArgs());
            }
        }
    }

    /**
     * 当客户端与服务端建立连接手，调用该方法创建Session
     *
     * @param socketChannel
     * @param args
     * @return
     */
    protected NioSession createSession(final SocketChannel socketChannel, final Object... args) {
        final NioSession session = this.buildSession(socketChannel);
        this.selectorManager.registerSession(session, SessionEventType.ENABLE_READ);
        this.setLocalSocketAddress((InetSocketAddress) socketChannel.socket().getLocalSocketAddress());
        session.start();
        this.handler.onSessionConnected(session, args);
        return session;
    }

    /**
     * 关闭SelectionKey
     *
     * @param key
     * @throws IOException
     */
    private void cancelKey(final SelectionKey key) throws IOException {
        try {
            if (key.channel() != null) {
                key.channel().close();
            }
        } finally {
            key.cancel();
        }
    }

    // getter and setter ...

    public ConnectFailListener getConnectFailListener() {
        return this.connectFailListener;
    }
    public void setConnectFailListener(final ConnectFailListener connectFailListener) {
        this.connectFailListener = connectFailListener;
    }

}