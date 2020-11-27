/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.taobao.gecko.service.impl;

import com.taobao.gecko.core.command.Constants;
import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.extension.ConnectFailListener;
import com.taobao.gecko.core.extension.GeckoTCPConnectorController;
import com.taobao.gecko.core.nio.NioSession;
import com.taobao.gecko.core.nio.impl.SocketChannelController;
import com.taobao.gecko.core.nio.impl.TimerRef;
import com.taobao.gecko.core.util.RemotingUtils;
import com.taobao.gecko.core.util.StringUtils;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * RemotingClient的默认实现
 *
 * @author boyan
 * @since 1.0, 2009-12-16 下午03:42:14
 */
public class DefaultRemotingClient extends BaseRemotingController implements RemotingClient, ConnectFailListener {

    private static final Log log = LogFactory.getLog(DefaultRemotingClient.class);

    /** 重连管理器 */
    private ReconnectManager reconnectManager;


    public DefaultRemotingClient(final ClientConfig clientConfig) {
        super(clientConfig);
        this.config = clientConfig;
        // 默认分组的最大连接数设置为Integer.MAX_VALUE
        this.setAttribute(Constants.DEFAULT_GROUP, Constants.CONNECTION_COUNT_ATTR, Integer.MAX_VALUE);
    }


    @Override
    protected SocketChannelController initController(final Configuration conf) {
        final GeckoTCPConnectorController notifyTCPConnectorController = new GeckoTCPConnectorController(conf);
        // 设置连接失败监听器
        notifyTCPConnectorController.setConnectFailListener(this);
        return notifyTCPConnectorController;
    }

    /**
     * 启动
     *
     * @throws NotifyRemotingException
     */
    @Override
    protected void doStart() throws NotifyRemotingException {
        // 启动GeckoTCPConnectorController
        this.startController();
        this.startReconnectManager();
    }
    /**
     * 停止
     *
     * @throws NotifyRemotingException
     */
    @Override
    protected void doStop() throws NotifyRemotingException {
        this.stopReconnectManager();
        this.closeAllConnection();
    }


    // 与服务端建立连接

    /**
     * 根据URL连接服务端，如果连接失败将转入重连模式
     *
     * @param group 服务端的URL，形如schema://host:port的字符串
     * @throws IOException
     */
    @Override
    public void connect(final String group) throws NotifyRemotingException {
        this.connect(group, 1);
    }
    /**
     * 这里需要同步，防止对同一个分组发起多个请求
     *
     * @param group
     * @param connCount
     * @throws NotifyRemotingException
     */
    @Override
    public synchronized void connect(String group, final int connCount) throws NotifyRemotingException {
        this.connect(group, group, connCount);
    }
    @Override
    public void connect(String url, String targetGroup) throws NotifyRemotingException {
        this.connect(url, targetGroup, 1);
    }
    // 核心方法
    /**
     * 根据URL连接服务端，如果连接失败将转入重连模式，但是连接加入的分组将为target group。
     *
     * @param url         服务端的URL，形如schema://host:port的字符串
     * @param targetGroup 连接成功后加入的分组
     * @param connCount   表示客户端要创建的连接数
     * @throws IOException
     */
    @Override
    public void connect(String url, String targetGroup, int connCount) throws NotifyRemotingException {
        // 客户端要创建的连接数必须大于0
        if (connCount <= 0) {
            throw new IllegalArgumentException("非法连接数，必须大于0");
        }

        url = url.trim();

        // 判断该客户端是否对该分组发起过连接请求
        if (this.isGroupConnectPending(targetGroup)) {
            return;
        }

        // 解析group，获取服务地址和端口
        final InetSocketAddress remoteAddress = this.getSocketAddrFromGroup(url);

        // 将group从重连管理器中移除
        this.reconnectManager.removeCanceledGroup(targetGroup);

        // 设置客户端要创建的连接数属性
        if (this.setAttributeIfAbsent(targetGroup, Constants.CONNECTION_COUNT_ATTR, connCount) != null) {
            return;
        }
        // 设置连接就绪锁
        if (this.setAttributeIfAbsent(targetGroup, Constants.GROUP_CONNECTION_READY_LOCK, new Object()) != null) {
            return;
        }

        final Set<String> groupSet = new HashSet<String>();
        groupSet.add(targetGroup);
        // 创建指定数量的连接对象
        for (int i = 0; i < connCount; i++) {
            try {
                final TimerRef timerRef = new TimerRef(((ClientConfig) this.config).getConnectTimeout(), null);
                // 建立连接
                final Future<NioSession> future = ((GeckoTCPConnectorController) this.controller).connect(remoteAddress, groupSet, remoteAddress, timerRef);

                // 用于检测连接建立是否成功
                final CheckConnectFutureRunner runnable = new CheckConnectFutureRunner(future, remoteAddress, groupSet, this);

                timerRef.setRunnable(runnable);
                // 将TimerRef实例添加Reactor#timerQueue的双向队列中
                this.insertTimer(timerRef);
            } catch (final Exception e) {
                log.error("连接" + RemotingUtils.getAddrString(remoteAddress) + "失败,启动重连任务", e);
                this.reconnectManager.addReconnectTask(new ReconnectTask(groupSet, remoteAddress));
            }
        }

    }


    // 等待连接就绪

    /**
     * 等待连接就绪：是指指定分组的有效连接数达到设定值，并且可用，默认等待超时为连接数乘以连接超时
     *
     * @param group
     * @throws NotifyRemotingException
     * @throws InterruptedException
     */
    @Override
    public void awaitReadyInterrupt(final String group) throws NotifyRemotingException, InterruptedException {
        final Object readyLock = this.getAttribute(group, Constants.GROUP_CONNECTION_READY_LOCK);
        final Object attribute = this.getAttribute(group, Constants.CONNECTION_COUNT_ATTR);
        if (readyLock == null || attribute == null) {
            throw new IllegalStateException("非法状态，你还没有调用connect方法进行连接操作。");
        }
        final long defaultConnectTimeout = ((ClientConfig) this.config).getConnectTimeout();
        this.awaitReadyInterrupt(group, defaultConnectTimeout * (Integer) attribute);
    }
    /**
     * 等待连接就绪：是指指定分组的有效连接数达到设定值，并且可用，默认等待超时为连接数乘以连接超时
     *
     * @param group
     * @param time
     * @throws NotifyRemotingException
     * @throws InterruptedException
     */
    @Override
    public void awaitReadyInterrupt(final String group, final long time) throws NotifyRemotingException, InterruptedException {
        if (StringUtils.isBlank(group)) {
            throw new IllegalArgumentException("Blank group");
        }

        // 获取分组连接就绪锁
        final Object readyLock = this.getAttribute(group, Constants.GROUP_CONNECTION_READY_LOCK);
        final Object attribute = this.getAttribute(group, Constants.CONNECTION_COUNT_ATTR);
        if (readyLock == null || attribute == null) {
            throw new IllegalStateException("非法状态，你还没有调用connect方法进行连接操作。");
        } else {
            final int maxConnCount = (Integer) attribute;
            long totalTime = 0;
            synchronized (readyLock) {
                // 一直轮询：直到该客户端的连接数个数等于创建连接#connect()方法指定的数量
                while (this.getConnectionCount(group) != maxConnCount) {
                    final long start = System.currentTimeMillis();
                    readyLock.wait(1000);
                    totalTime += System.currentTimeMillis() - start;
                    if (totalTime >= time) {
                        throw new NotifyRemotingException("等待连接就绪超时，超时时间为" + time + "毫秒");
                    }
                }
            }
        }

    }



    /**
     * 根据group从可用连接获取服务端的InetSocketAddress
     *
     * @param group
     * @return
     */
    @Override
    public InetSocketAddress getRemoteAddress(final String group) {
        if (this.remotingContext == null) {
            return null;
        }

        final List<Connection> connections = this.remotingContext.getConnectionsByGroup(group);
        if (connections == null || connections.size() == 0) {
            return null;
        }

        for (final Connection conn : connections) {
            if (conn.getRemoteSocketAddress() != null) {
                return conn.getRemoteSocketAddress();
            }
        }
        return null;
    }

    /**
     * 获取远端地址
     *
     * @param group 服务端的group，形如schema://host:port的字符串
     * @return
     */
    @Override
    public String getRemoteAddressString(final String group) {
        return RemotingUtils.getAddrString(this.getRemoteAddress(group));
    }

    /**
     * 判断group是否存在可用的连接对象，注意，如果设置了连接池，那么如果连接池中任一连接可用，即认为可用
     *
     * @param group 服务端的url，形如schema://host:port的字符串
     * @return
     */
    @Override
    public boolean isConnected(final String group) {
        if (this.remotingContext == null) {
            return false;
        }

        final List<Connection> connections = this.remotingContext.getConnectionsByGroup(group);
        if (connections == null || connections.size() == 0) {
            return false;
        }

        for (final Connection conn : connections) {
            if (conn.isConnected()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 设置客户端配置，只能在启动前设置，启动后设置无效
     *
     * @param clientConfig
     */
    @Override
    public void setClientConfig(final ClientConfig clientConfig) {
        if (this.controller != null && this.controller.isStarted()) {
            throw new IllegalStateException("RemotingClient已经启动，设置无效");
        }
        this.config = clientConfig;
    }







    @Override
    public void close(final String group, final boolean allowReconnect) throws NotifyRemotingException {
        if (!this.started) {
            throw new NotifyRemotingException("The controller has been stopped");
        }
        if (group == null) {
            throw new IllegalArgumentException("null group");
        }
        if (!allowReconnect) {
            // 取消重连任务
            this.reconnectManager.cancelReconnectGroup(group);
            // 删除属性
            this.attributes.remove(group);
        }
        final List<Connection> connections = this.remotingContext.getConnectionsByGroup(group);
        if (connections != null) {
            for (final Connection conn : connections) {
                if (conn.isConnected()) {
                    conn.close(allowReconnect);
                }
            }
        }

    }


    // -------------------
    // ConnectFailListener
    // -------------------

    /**
     * 当连接失败的时候回调
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onConnectFail(final Object... args) {
        if (args.length >= 2) {
            final Set<String> groupSet = (Set<String>) args[0];
            final InetSocketAddress remoteAddr = (InetSocketAddress) args[1];
            this.reconnectManager.addReconnectTask(new ReconnectTask(groupSet, remoteAddr));
            if (args.length >= 3) {
                final TimerRef timerRef = (TimerRef) args[2];
                timerRef.cancel();
            }
        }

    }





    /**
     * 关闭该客户端所有的连接
     *
     * @throws NotifyRemotingException
     */
    private void closeAllConnection() throws NotifyRemotingException {
        // 关闭所有连接
        final List<Connection> connections = this.remotingContext.getConnectionsByGroup(Constants.DEFAULT_GROUP);
        if (connections != null) {
            for (final Connection conn : connections) {
                // 强制为就绪状态
                ((DefaultConnection) conn).setReady(true);
                conn.close(false);
            }
        }
    }

    /**
     * 关闭连接管理器
     */
    private void stopReconnectManager() {
        this.reconnectManager.stop();
    }

    /**
     * 启动连接管理器
     */
    private void startReconnectManager() {
        // 启动重连管理器
        this.reconnectManager = new ReconnectManager(
                (GeckoTCPConnectorController) this.controller,
                (ClientConfig) this.config,
                this);
        ((GeckoHandler) this.controller.getHandler()).setReconnectManager(this.reconnectManager);
        this.reconnectManager.start();
    }

    /**
     *
     * @throws NotifyRemotingException
     */
    private void startController() throws NotifyRemotingException {
        try {
            this.controller.start();
        } catch (final IOException e) {
            throw new NotifyRemotingException("启动控制器出错", e);
        }
    }

    /**
     * 判断分组是否发起过连接请求
     *
     * @param group
     * @return
     */
    private boolean isGroupConnectPending(final String group) {
        final Object readyLock = this.getAttribute(group, Constants.GROUP_CONNECTION_READY_LOCK);
        final Object attribute = this.getAttribute(group, Constants.CONNECTION_COUNT_ATTR);
        return readyLock != null && attribute != null;
    }

    public ReconnectManager getReconnectManager() {
        return this.reconnectManager;
    }

    /**
     * 解析group，获取服务地址和端口
     *
     * @param group
     * @return
     * @throws NotifyRemotingException
     */
    private InetSocketAddress getSocketAddrFromGroup(String group) throws NotifyRemotingException {
        if (group == null) {
            throw new IllegalArgumentException("Null group");
        }

        group = group.trim();
        if (!group.startsWith(this.config.getWireFormatType().getScheme())) {
            throw new NotifyRemotingException("非法的Group格式，没有以" + this.config.getWireFormatType().getScheme() + "开头");
        }

        try {
            final URI uri = new URI(group);
            return new InetSocketAddress(uri.getHost(), uri.getPort());
        } catch (final Exception e) {
            throw new NotifyRemotingException("从uri生成服务器地址出错,url=" + group, e);
        }
    }



    /**
     * 用于检测连接建立是否成功：从future获取会话对象，如果10毫秒内还客户端还没有与服务创建连接，则将任务放到重连管理器中
     *
     * @author boyan
     * @since 1.0, 2009-12-23 下午01:49:41
     */
    public static final class CheckConnectFutureRunner implements Runnable {
        /** 客户端与服务端建立连接的会话引用 */
        final Future<NioSession> future;
        /** 表示服务地址 */
        final InetSocketAddress remoteAddress;
        /**  */
        final Set<String> groupSet;
        /** 表示发起连接的客户端对象 */
        final DefaultRemotingClient remotingClient;

        public CheckConnectFutureRunner(final Future<NioSession> future, final InetSocketAddress remoteAddress, final Set<String> groupSet, final DefaultRemotingClient remotingClient) {
            super();
            this.future = future;
            this.remoteAddress = remoteAddress;
            this.groupSet = groupSet;
            this.remotingClient = remotingClient;
        }

        /**
         * 从future获取会话，如果10毫秒内还客户端还没有与服务创建连接，则将任务放到重连管理器中
         */
        @Override
        public void run() {
            try {
                if (!this.future.isDone() && this.future.get(10, TimeUnit.MILLISECONDS) == null) {
                    final ReconnectManager reconnectManager = this.remotingClient.getReconnectManager();
                    reconnectManager.addReconnectTask(new ReconnectTask(this.groupSet, this.remoteAddress));
                }
            } catch (final Exception e) {
                log.error("连接" + this.remoteAddress + "失败", e);
            }
        }

    }

}
