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
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved.
 */
package com.taobao.gecko.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.EmptyStackException;
import java.util.Stack;


/**
 * 用于维护选择器池的工厂
 *
 * Temp selector factory, come from grizzly
 *
 * @author dennis zhuang
 */
public class SelectorFactory {

    public static final int DEFAULT_MAX_SELECTORS = 20;

    private static final Log logger = LogFactory.getLog(SelectorFactory.class);
    /**
     * The timeout before we exit.
     */
    public final static long timeout = 5000;

    /**
     * The number of <code>Selector</code> to create.
     */
    private static int maxSelectors;

    /**
     * Cache of <code>Selector</code>
     */
    private final static Stack<Selector> selectors = new Stack<Selector>();

    /**
     * 初始化选择器池
     */
    static {
        try {
            setMaxSelectors(DEFAULT_MAX_SELECTORS);
        } catch (IOException ex) {
            logger.warn("SelectorFactory initializing Selector pool", ex);
        }
    }


    /**
     * 从选择器池获取一个选择器
     *
     * @return Selector
     */
    public final static Selector getSelector() {
        synchronized (selectors) {
            Selector s = null;
            try {
                if (selectors.size() != 0) {
                    s = selectors.pop();
                }
            } catch (EmptyStackException ex) {
            }

            int attempts = 0;
            try {
                while (s == null && attempts < 2) {
                    selectors.wait(timeout);
                    try {
                        if (selectors.size() != 0) {
                            s = selectors.pop();
                        }
                    } catch (EmptyStackException ex) {
                        break;
                    }
                    attempts++;
                }
            } catch (InterruptedException ex) {
            }
            return s;
        }
    }


    /**
     * 将选择器归还给池子
     *
     * @param s
     */
    public final static void returnSelector(Selector s) {
        synchronized (selectors) {
            selectors.push(s);
            if (selectors.size() == 1) {
                selectors.notify();
            }
        }
    }


    /**
     * 获取当前选择器池的大小
     *
     * @return max pool size
     */
    public final static int getMaxSelectors() {
        return maxSelectors;
    }

    /**
     * 设置选择器池的大小，如果目前的选择器个数>size，则将多余的选择器销毁，如果<size，则创建选择器直到达到size
     *
     * @param size max pool size
     */
    public final static void setMaxSelectors(int size) throws IOException {
        synchronized (selectors) {
            if (size < maxSelectors) {
                reduce(size);
            } else if (size > maxSelectors) {
                grow(size);
            }

            maxSelectors = size;
        }
    }

    /**
     * 创建相应数量的选择器到池子，直到池中的选择器数量大小size
     *
     * @param size
     * @throws IOException
     */
    private static void grow(int size) throws IOException {
        for (int i = 0; i < size - maxSelectors; i++) {
            selectors.add(Selector.open());
        }
    }

    /**
     * 销毁池子中多出的选择器
     *
     * @param size
     */
    private static void reduce(int size) {
        for (int i = 0; i < maxSelectors - size; i++) {
            try {
                Selector selector = selectors.pop();
                selector.close();
            } catch (IOException e) {
                logger.error("SelectorFactory.reduce", e);
            }
        }
    }

}