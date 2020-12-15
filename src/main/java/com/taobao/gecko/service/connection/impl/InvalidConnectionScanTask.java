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
package com.taobao.gecko.service.connection.impl;

import com.taobao.gecko.service.connection.Connection;
import com.taobao.gecko.service.connection.ScanTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * 扫描无效的连接任务，仅用于服务器
 *
 * @author boyan
 * @Date 2010-5-26
 */
public class InvalidConnectionScanTask implements ScanTask {

    static final Log log = LogFactory.getLog(InvalidConnectionScanTask.class);

    /** 对于服务器来说，如果5分钟没有任何操作，那么将断开连接，因为客户端总是会发起心跳检测，因此不会对正常的空闲连接误判。*/
    public static long TIMEOUT_THRESHOLD = Long.parseLong(System.getProperty("notify.remoting.connection.timeout_threshold", "300000"));

    /**
     * 5分钟没有任何操作，那么将断开连接
     *
     * @param now  扫描触发的时间点
     * @param conn 当前扫描到的连接
     */
    public void visit(final long now, final Connection conn) {
        // 返回连接对象的上一次的操作时间，包括读和写操作
        final long lastOpTimestamp = ((DefaultConnection) conn).getSession().getLastOperationTimeStamp();
        // 超过5分钟没有请求，则关闭连接
        if (now - lastOpTimestamp > TIMEOUT_THRESHOLD) {
            log.info("无效的连接" + conn.getRemoteSocketAddress() + "被关闭，超过" + TIMEOUT_THRESHOLD + "毫秒没有任何IO操作");
            try {
                conn.close(false);
            } catch (final Throwable t) {
                log.error("关闭连接失败", t);
            }
        }
    }
}