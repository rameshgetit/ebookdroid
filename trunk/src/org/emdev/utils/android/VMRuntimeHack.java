package org.emdev.utils.android;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class VMRuntimeHack {

    private static Object runtime = null;
    private static Method trackAllocation = null;
    private static Method trackFree = null;

    static {
        boolean success = false;
        try {
            Class<?> cl = Class.forName("dalvik.system.VMRuntime");
            Method getRt = cl.getMethod("getRuntime", new Class[0]);
            runtime = getRt.invoke(null, new Object[0]);
            trackAllocation = cl.getMethod("trackExternalAllocation", new Class[] { long.class });
            trackFree = cl.getMethod("trackExternalFree", new Class[] { long.class });
            success = true;
        } catch (ClassNotFoundException e) {
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        if (!success) {
            Log.i("VMRuntimeHack", "VMRuntime hack does not work!");
            runtime = null;
            trackAllocation = null;
            trackFree = null;
        } else {
            Log.i("VMRuntimeHack", "VMRuntime hack initialized!");
        }
    }

    public static boolean trackAlloc(long size) {
        if (runtime == null)
            return false;
        try {
            Object res = trackAllocation.invoke(runtime, Long.valueOf(size));
            return (res instanceof Boolean) ? (Boolean) res : true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        }
    }

    public static boolean trackFree(long size) {
        if (runtime == null)
            return false;
        try {
            Object res = trackFree.invoke(runtime, Long.valueOf(size));
            return (res instanceof Boolean) ? (Boolean) res : true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        }
    }
}
