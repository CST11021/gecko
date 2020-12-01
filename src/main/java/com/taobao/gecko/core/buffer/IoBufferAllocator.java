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
/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package com.taobao.gecko.core.buffer;

import java.nio.ByteBuffer;


/**
 * 用于创建IoBuffer缓存区对象的分配器
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 671827 $, $Date: 2008-06-26 10:49:48 +0200 (Thu, 26 Jun 2008)
 * $
 */
public interface IoBufferAllocator {

    /**
     * 返回具有指定大小的缓冲区
     *
     * @param capacity  缓冲区的容量
     * @param direct    为true时，表示获取的是直接缓冲，否则获取的堆缓冲区
     */
    IoBuffer allocate(int capacity, boolean direct);

    /**
     * 返回具有指定大小的NIO缓冲区
     *
     * @param capacity 缓冲区的容量
     * @param direct   为true时，表示获取的是直接缓冲，否则获取的堆缓冲区
     */
    ByteBuffer allocateNioBuffer(int capacity, boolean direct);

    /**
     * 将指定的Nio的缓冲区（ByteBuffer）对象包装为MINA缓冲区（IoBuffer）并返回
     *
     * @param nioBuffer
     * @return
     */
    IoBuffer wrap(ByteBuffer nioBuffer);

    /**
     * 销毁分配器
     */
    void dispose();

}