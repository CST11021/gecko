package com.taobao.gecko.example.rpc.registry;

import com.taobao.gecko.example.rpc.example.server.HelloImpl;

/**
 * @Author: wanghz
 * @Date: 2020/12/1 11:56 AM
 */
public class RegistryImpl implements Registry {

    public Object findServer(String name) {
        if (name.equals("hello")) {
            return new HelloImpl();
        }
        return null;
    }

}
