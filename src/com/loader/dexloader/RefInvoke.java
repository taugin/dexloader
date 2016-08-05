package com.loader.dexloader;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RefInvoke {

    public static Object invokeStaticMethod(String className,
            String methodName, Class<?>[] pareTyple, Object[] pareVaules) {

        try {
            Class<?> objClass = Class.forName(className);
            Method method = objClass.getMethod(methodName, pareTyple);
            return method.invoke(null, pareVaules);
        } catch (SecurityException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (NoSuchMethodException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (InvocationTargetException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (ClassNotFoundException e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;

    }

    public static Object invokeMethod(String className, String methodName,
            Object obj, Class<?>[] pareTyple, Object[] pareVaules) {
        try {
            Class<?> objClass = Class.forName(className);
            Method method = objClass.getMethod(methodName, pareTyple);
            return method.invoke(obj, pareVaules);
        } catch (SecurityException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (NoSuchMethodException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (InvocationTargetException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (ClassNotFoundException e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;

    }

    public static Object getFieldOjbect(String className, Object obj,
            String filedName) {
        try {
            Class<?> objClass = Class.forName(className);
            Field field = objClass.getDeclaredField(filedName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (SecurityException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (NoSuchFieldException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (ClassNotFoundException e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;

    }

    public static Object getStaticFieldOjbect(String className,
            String filedName) {

        try {
            Class<?> objClass = Class.forName(className);
            Field field = objClass.getDeclaredField(filedName);
            field.setAccessible(true);
            return field.get(null);
        } catch (SecurityException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (NoSuchFieldException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (ClassNotFoundException e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;

    }

    public static void setFieldOjbect(String className, String filedName,
            Object obj, Object filedVaule) {
        try {
            Class<?> objClass = Class.forName(className);
            Field field = objClass.getDeclaredField(filedName);
            field.setAccessible(true);
            field.set(obj, filedVaule);
        } catch (SecurityException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (NoSuchFieldException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (ClassNotFoundException e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    public static void setStaticOjbect(String className, String filedName,
            Object filedVaule) {
        try {
            Class<?> objClass = Class.forName(className);
            Field field = objClass.getDeclaredField(filedName);
            field.setAccessible(true);
            field.set(null, filedVaule);
        } catch (SecurityException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (NoSuchFieldException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (ClassNotFoundException e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

}
