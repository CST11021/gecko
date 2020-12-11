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
package com.taobao.gecko.service.impl;

import com.taobao.gecko.core.command.CommandFactory;
import com.taobao.gecko.core.command.Constants;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.nio.NioSession;
import com.taobao.gecko.core.util.MBeanUtils;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionLifeCycleListener;
import com.taobao.gecko.service.RemotingContext;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.config.BaseConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;


/**
 * 通讯层的全局上下文
 *
 * @author boyan
 * @since 1.0, 2009-12-15 下午02:46:34
 */
public class DefaultRemotingContext implements RemotingContext, DefaultRemotingContextMBean {

    static final Log log = LogFactory.getLog(DefaultRemotingContext.class);

    /** 网络层的基础配置 */
    private final BaseConfig config;
    /** 分组管理器：管理分组到连接的映射关系 */
    private final GroupManager groupManager;

    private final CommandFactory commandFactory;
    private final Semaphore callBackSemaphore;

    private final ConcurrentHashMap<Object, Object> attributes = new ConcurrentHashMap<Object, Object>();
    /** Session到connection的映射关系 */
    protected final ConcurrentHashMap<NioSession, DefaultConnection> session2ConnectionMap = new ConcurrentHashMap<NioSession, DefaultConnection>();
    /** 保存指定的命令类型对应的命令处理器：Map<RequestCommand, RequestProcessor> */
    protected ConcurrentHashMap<Class<? extends RequestCommand>, RequestProcessor<? extends RequestCommand>> processorMap = new ConcurrentHashMap<Class<? extends RequestCommand>, RequestProcessor<? extends RequestCommand>>();
    /** 连接的生命周期监听器 */
    protected final CopyOnWriteArrayList<ConnectionLifeCycleListener> connectionLifeCycleListenerList = new CopyOnWriteArrayList<ConnectionLifeCycleListener>();


    public DefaultRemotingContext(final BaseConfig config, final CommandFactory commandFactory) {
        this.groupManager = new GroupManager();
        this.config = config;
        if (commandFactory == null) {
            throw new IllegalArgumentException("CommandFactory不能为空");
        }
        this.commandFactory = commandFactory;
        this.callBackSemaphore = new Semaphore(this.config.getMaxCallBackCount());
        MBeanUtils.registerMBeanWithIdPrefix(this, null);
    }

    // 实现 DefaultRemotingContextMBean 接口

    public int getCallBackCountAvailablePermits() {
        return this.callBackSemaphore.availablePermits();
    }




    public CommandFactory getCommandFactory() {
        return this.commandFactory;
    }

    public BaseConfig getConfig() {
        return this.config;
    }

    /**
     * 请求允许加入callBack，做callBack总数限制
     *
     * @return
     */
    boolean aquire() {
        return this.callBackSemaphore.tryAcquire();
    }

    /**
     * 在应答到达时释放许可
     */
    void release() {
        this.callBackSemaphore.release();
    }

    void release(final int n) {
        this.callBackSemaphore.release(n);
    }

    /**
     * 连接被创建后调用该方法：触发所有的 ConnectionLifeCycleListener 监听器
     *
     * @param conn
     */
    void notifyConnectionCreated(final Connection conn) {
        for (final ConnectionLifeCycleListener listener : this.connectionLifeCycleListenerList) {
            try {
                listener.onConnectionCreated(conn);
            } catch (final Throwable t) {
                log.error("NotifyRemoting-调用ConnectionLifeCycleListener.onConnectionCreated出错", t);
            }
        }
    }

    /**
     * 连接被关闭后调用该方法：触发所有的 ConnectionLifeCycleListener 监听器
     *
     * @param conn
     */
    void notifyConnectionClosed(final Connection conn) {
        for (final ConnectionLifeCycleListener listener : this.connectionLifeCycleListenerList) {
            try {
                listener.onConnectionClosed(conn);
            } catch (final Throwable t) {
                log.error("NotifyRemoting-调用ConnectionLifeCycleListener.onConnectionClosed出错", t);
            }
        }
    }

    void addSession2ConnectionMapping(final NioSession session, final DefaultConnection conn) {
        this.session2ConnectionMap.put(session, conn);
    }

    DefaultConnection getConnectionBySession(final NioSession session) {
        return this.session2ConnectionMap.get(session);
    }

    DefaultConnection removeSession2ConnectionMapping(final NioSession session) {
        return this.session2ConnectionMap.remove(session);
    }

    public Set<String> getGroupSet() {
        return this.groupManager.getGroupSet();
    }

    /**
     * 将连接添加到所属的分组
     *
     * @param group
     * @param connection
     * @return
     */
    public boolean addConnectionToGroup(final String group, final Connection connection) {
        return this.groupManager.addConnection(group, connection);
    }

    /**
     * 将连接添加到默认的分组
     *
     * @param connection
     */
    public void addConnection(final Connection connection) {
        this.groupManager.addConnection(Constants.DEFAULT_GROUP, connection);
    }

    public void removeConnection(final Connection connection) {
        this.groupManager.removeConnection(Constants.DEFAULT_GROUP, connection);
    }

    /**
     * 根据group获取所有可用的连接对象
     *
     * @param group
     * @return
     */
    public List<Connection> getConnectionsByGroup(final String group) {
        return this.groupManager.getConnectionsByGroup(group);
    }

    public boolean removeConnectionFromGroup(final String group, final Connection connection) {
        return this.groupManager.removeConnection(group, connection);
    }

    public Object getAttribute(final Object key, final Object defaultValue) {
        final Object value = this.attributes.get(key);
        return value == null ? defaultValue : value;
    }

    public Object getAttribute(final Object key) {
        return this.attributes.get(key);
    }

    public Set<Object> getAttributeKeys() {
        return this.attributes.keySet();
    }

    public Object setAttribute(final Object key, final Object value) {
        return this.attributes.put(key, value);
    }

    public Object setAttribute(final Object key) {
        return this.attributes.put(key, null);
    }

    public Object setAttributeIfAbsent(final Object key, final Object value) {
        return this.attributes.putIfAbsent(key, value);
    }

    public Object removeAttribute(final Object key) {
        return this.attributes.remove(key);
    }

    public Object setAttributeIfAbsent(final Object key) {
        return this.attributes.putIfAbsent(key, null);
    }

    public void dispose() {
        this.groupManager.clear();
        this.attributes.clear();
    }

}