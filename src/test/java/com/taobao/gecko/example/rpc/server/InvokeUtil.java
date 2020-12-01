package com.taobao.gecko.example.rpc.server;

import java.lang.reflect.Method;

/**
 * @Author: wanghz
 * @Date: 2020/12/1 11:45 AM
 */
public class InvokeUtil {

    /**
     * 根据方法名和入参调用指定的方法
     *
     * @param methodName
     * @param args
     * @return
     */
    public static Object invoke(Object object, String methodName, Object[] args) {
        Method method = getMethod(object, methodName, args);
        if (method == null) {
            throw new RuntimeException("Unknow method in " + object.getClass().getCanonicalName() + ":" + methodName);
        }
        try {
            return method.invoke(object, args);
        } catch (Exception e) {
            throw new RuntimeException("Invoke " + object.getClass().getCanonicalName() + "." + methodName + " failure", e);
        }
    }

    /**
     * 根据方法名获取对应的方法
     *
     * @param methodName
     * @param args
     * @return
     */
    private static Method getMethod(Object object, String methodName, Object[] args) {
        Method method = null;
        Class<?> clazz = object.getClass();
        try {
            if (args != null && args.length > 0) {
                Class<?>[] parameterTypes = new Class<?>[args.length];
                for (int i = 0; i < args.length; i++) {
                    parameterTypes[i] = args[i] != null ? args[i].getClass() : null;
                }
                method = clazz.getMethod(methodName, parameterTypes);
            } else {
                method = clazz.getMethod(methodName);
            }

        } catch (NoSuchMethodException e) {
        }
        if (method == null) {
            Method[] methods = clazz.getMethods();
            for (Method m : methods) {
                if (m.getName().equals(methodName) && m.getParameterTypes().length == args.length) {
                    method = m;
                    break;
                }
            }
        }
        return method;
    }

}
