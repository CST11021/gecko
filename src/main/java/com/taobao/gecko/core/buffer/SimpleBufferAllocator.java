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
import java.nio.ByteOrder;


/**
 * 一个简单缓冲区分配器
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 671827 $, $Date: 2008-06-26 10:49:48 +0200 (Thu, 26 Jun 2008)
 * $
 */
public class SimpleBufferAllocator implements IoBufferAllocator {

    /**
     * 返回具有指定大小的NIO缓冲区
     *
     * @param capacity 缓冲区的容量
     * @param direct   为true时，表示获取的是直接缓冲，否则获取的堆缓冲区，直接缓冲区和堆缓冲区实现如下：
     *                 直接缓冲区：ByteBuffer.allocateDirect(capacity)
     *                 堆缓冲区：ByteBuffer.allocate(capacity);
     */
    public IoBuffer allocate(int capacity, boolean direct) {
        return wrap(allocateNioBuffer(capacity, direct));
    }
    /**
     * 返回具有指定大小的NIO缓冲区
     *
     * @param capacity 缓冲区的容量
     * @param direct   为true时，表示获取的是直接缓冲，否则获取的堆缓冲区，直接缓冲区和堆缓冲区实现如下：
     *                 直接缓冲区：ByteBuffer.allocateDirect(capacity)
     *                 堆缓冲区：ByteBuffer.allocate(capacity);
     */
    public ByteBuffer allocateNioBuffer(int capacity, boolean direct) {
        ByteBuffer nioBuffer;
        if (direct) {
            nioBuffer = ByteBuffer.allocateDirect(capacity);
        } else {
            // 这里分配的ByteBuffer都是用0填充的
            nioBuffer = ByteBuffer.allocate(capacity);
        }
        return nioBuffer;
    }

    /**
     * 将java.nio.ByteBuffer封装为IoBuffer返回
     *
     * @param nioBuffer
     * @return
     */
    public IoBuffer wrap(ByteBuffer nioBuffer) {
        return new SimpleBuffer(nioBuffer);
    }

    public void dispose() {
    }



    private class SimpleBuffer extends AbstractIoBuffer {

        private ByteBuffer buf;

        protected SimpleBuffer(ByteBuffer buf) {
            super(SimpleBufferAllocator.this, buf.capacity());
            this.buf = buf;
            buf.order(ByteOrder.BIG_ENDIAN);
        }
        protected SimpleBuffer(SimpleBuffer parent, ByteBuffer buf) {
            super(parent);
            this.buf = buf;
        }

        @Override
        public ByteBuffer buf() {
            return buf;
        }

        @Override
        protected void buf(ByteBuffer buf) {
            this.buf = buf;
        }

        @Override
        protected IoBuffer duplicate0() {
            return new SimpleBuffer(this, this.buf.duplicate());
        }

        @Override
        protected IoBuffer slice0() {
            return new SimpleBuffer(this, this.buf.slice());
        }

        @Override
        protected IoBuffer asReadOnlyBuffer0() {
            return new SimpleBuffer(this, this.buf.asReadOnlyBuffer());
        }

        @Override
        public byte[] array() {
            return buf.array();
        }

        @Override
        public int arrayOffset() {
            return buf.arrayOffset();
        }

        @Override
        public boolean hasArray() {
            return buf.hasArray();
        }

        @Override
        public void free() {
        }

    }
}