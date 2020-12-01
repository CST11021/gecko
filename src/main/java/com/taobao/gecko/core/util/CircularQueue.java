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
package com.taobao.gecko.core.util;

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

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;


/**
 * 来自mina的循环队列实现，非线程安全的队列
 *
 * @author boyan
 * @since 1.0, 2009-12-16 下午06:19:56
 */
public class CircularQueue<E> extends AbstractList<E> implements List<E>, Queue<E>, Serializable {

    private static final long serialVersionUID = 3993421269224511264L;

    private static final int DEFAULT_CAPACITY = 4;

    private final int initialCapacity;
    // XXX: This volatile keyword here is a workaround for SUN Java Compiler bug, which produces buggy byte code.
    // I don't event know why adding a volatile fixes the problem.
    // Eclipse Java Compiler seems to produce correct byte code.
    private volatile Object[] items;
    private int mask;
    private int first = 0;
    private int last = 0;
    private boolean full;
    private int shrinkThreshold;


    /**
     * Construct a new, empty queue.
     */
    public CircularQueue() {
        this(DEFAULT_CAPACITY);
    }
    public CircularQueue(int initialCapacity) {
        int actualCapacity = normalizeCapacity(initialCapacity);
        items = new Object[actualCapacity];
        mask = actualCapacity - 1;
        this.initialCapacity = actualCapacity;
        this.shrinkThreshold = 0;
    }


    private static int normalizeCapacity(int initialCapacity) {
        int actualCapacity = 1;
        while (actualCapacity < initialCapacity) {
            actualCapacity <<= 1;
            if (actualCapacity < 0) {
                actualCapacity = 1 << 30;
                break;
            }
        }
        return actualCapacity;
    }

    /**
     * 返回队列的容量大小
     *
     * @return
     */
    public int capacity() {
        return items.length;
    }

    /**
     * 清空队列
     */
    @Override
    public void clear() {
        if (!isEmpty()) {
            Arrays.fill(items, null);
            first = 0;
            last = 0;
            full = false;
            shrinkIfNeeded();
        }
    }

    /**
     * 获取指定索引位置的元素
     *
     * @param idx
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public E get(int idx) {
        // 检查索引是否越界
        checkIndex(idx);
        return (E) items[getRealIndex(idx)];
    }

    /**
     * 设置指定位置的元素
     *
     * @param idx
     * @param o
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public E set(int idx, E o) {
        checkIndex(idx);

        int realIdx = getRealIndex(idx);
        Object old = items[realIdx];
        items[realIdx] = o;
        return (E) old;
    }

    /**
     * 返回该队列是否为空
     *
     * @return
     */
    @Override
    public boolean isEmpty() {
        return (first == last) && !full;
    }

    /**
     * 返回当前该队列中元素的个数
     *
     * @return
     */
    @Override
    public int size() {
        if (full) {
            return capacity();
        }

        if (last >= first) {
            return last - first;
        } else {
            return last - first + capacity();
        }
    }





    // 添加元素

    /**
     * 增加一个元索，如果队列已满，则抛出一个IIIegaISlabEepeplian异常
     *
     * @param o
     * @return
     */
    @Override
    public boolean add(E o) {
        return offer(o);
    }

    /**
     * 增加一个元索，如果队列已满，则抛出一个IIIegaISlabEepeplian异常
     *
     * @param idx
     * @param o
     */
    @Override
    public void add(int idx, E o) {
        if (idx == size()) {
            offer(o);
            return;
        }

        checkIndex(idx);
        expandIfNeeded();

        int realIdx = getRealIndex(idx);

        // Make a room for a new element.
        if (first < last) {
            System.arraycopy(items, realIdx, items, realIdx + 1, last - realIdx);
        } else {
            if (realIdx >= first) {
                System.arraycopy(items, 0, items, 1, last);
                items[0] = items[items.length - 1];
                System.arraycopy(items, realIdx, items, realIdx + 1, items.length - realIdx - 1);
            } else {
                System.arraycopy(items, realIdx, items, realIdx + 1, last - realIdx);
            }
        }

        items[realIdx] = o;
        increaseSize();
    }

    /**
     * 添加一个元素并返回true，如果队列已满，则返回false
     *
     * @param item
     * @return
     */
    public boolean offer(E item) {
        if (item == null) {
            throw new NullPointerException("item");
        }
        expandIfNeeded();
        items[last] = item;
        increaseSize();
        return true;
    }


    // 移除元素

    /**
     * 移除并返回队列头部的元素，如果队列为空，则抛出一个NoSuchElementException异常
     *
     * @return
     */
    public E remove() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }

        return poll();
    }

    @SuppressWarnings("unchecked")
    @Override
    public E remove(int idx) {
        if (idx == 0) {
            return poll();
        }

        checkIndex(idx);

        int realIdx = getRealIndex(idx);
        Object removed = items[realIdx];

        // Remove a room for the removed element.
        if (first < last) {
            System.arraycopy(items, first, items, first + 1, realIdx - first);
        } else {
            if (realIdx >= first) {
                System.arraycopy(items, first, items, first + 1, realIdx - first);
            } else {
                System.arraycopy(items, 0, items, 1, realIdx);
                items[0] = items[items.length - 1];
                System.arraycopy(items, first, items, first + 1, items.length - first - 1);
            }
        }

        items[first] = null;
        decreaseSize();

        shrinkIfNeeded();
        return (E) removed;
    }

    /**
     * 移除并返问队列头部的元素，如果队列为空，则返回null
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public E poll() {
        if (isEmpty()) {
            return null;
        }

        Object ret = items[first];
        items[first] = null;
        decreaseSize();

        if (first == last) {
            first = last = 0;
        }

        shrinkIfNeeded();
        return (E) ret;
    }



    // 返回队列头部的元素

    /**
     * 返回队列头部的元素，如果队列为空，则抛出一个NoSuchElementException异常
     *
     * @return
     */
    public E element() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }

        return peek();
    }

    /**
     * 返回队列头部的元素，如果队列为空，则返回null
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public E peek() {
        if (isEmpty()) {
            return null;
        }

        return (E) items[first];
    }


    /**
     * 检查索引是否越界
     *
     * @param idx
     */
    private void checkIndex(int idx) {
        if (idx < 0 || idx >= size()) {
            throw new IndexOutOfBoundsException(String.valueOf(idx));
        }
    }

    private int getRealIndex(int idx) {
        return (first + idx) & mask;
    }

    private void increaseSize() {
        last = (last + 1) & mask;
        full = first == last;
    }

    private void decreaseSize() {
        first = (first + 1) & mask;
        full = false;
    }

    private void expandIfNeeded() {
        if (full) {
            // expand queue
            final int oldLen = items.length;
            final int newLen = oldLen << 1;
            Object[] tmp = new Object[newLen];

            if (first < last) {
                System.arraycopy(items, first, tmp, 0, last - first);
            } else {
                System.arraycopy(items, first, tmp, 0, oldLen - first);
                System.arraycopy(items, 0, tmp, oldLen - first, last);
            }

            first = 0;
            last = oldLen;
            items = tmp;
            mask = tmp.length - 1;
            if (newLen >>> 3 > initialCapacity) {
                shrinkThreshold = newLen >>> 3;
            }
        }
    }

    private void shrinkIfNeeded() {
        int size = size();
        if (size <= shrinkThreshold) {
            // shrink queue
            final int oldLen = items.length;
            int newLen = normalizeCapacity(size);
            if (size == newLen) {
                newLen <<= 1;
            }

            if (newLen >= oldLen) {
                return;
            }

            if (newLen < initialCapacity) {
                if (oldLen == initialCapacity) {
                    return;
                } else {
                    newLen = initialCapacity;
                }
            }

            Object[] tmp = new Object[newLen];

            // Copy only when there's something to copy.
            if (size > 0) {
                if (first < last) {
                    System.arraycopy(items, first, tmp, 0, last - first);
                } else {
                    System.arraycopy(items, first, tmp, 0, oldLen - first);
                    System.arraycopy(items, 0, tmp, oldLen - first, last);
                }
            }

            first = 0;
            last = size;
            items = tmp;
            mask = tmp.length - 1;
            shrinkThreshold = 0;
        }
    }


    @Override
    public String toString() {
        return "first=" + first + ", last=" + last + ", size=" + size() + ", mask = " + mask;
    }
}