package com.loader.dexloader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import dalvik.system.DexClassLoader;

public class CocosPayAdapter {

    private static String sDexPath;
    private static String sOdexPath;
    private static String sLibPath;
    public static final void init(Application app) {
        Context context = app.getBaseContext();
        loadMegJb();
        setDexPath(context);
        loadClassLoader(context);
        loadApplication(context);

        Log.d(Log.TAG, "loader 1 : " + context.getClassLoader());
        Log.d(Log.TAG, "loader 2 : " + context.getClassLoader().getParent());
        Log.d(Log.TAG, "loader 3 : "
                + context.getClassLoader().getParent().getParent());
    }

    private static void setDexPath(Context context) {
        String odexPath = context.getDir(LoaderHelper.APP_DEX_PATH,
                Context.MODE_PRIVATE)
                .getAbsolutePath();
        LoaderHelper helper = new LoaderHelper(context);
        String dexPath = helper.extractJarFile();
        String libPath = context.getApplicationInfo().nativeLibraryDir;
        sDexPath = dexPath;
        sOdexPath = odexPath;
        sLibPath = libPath;
        Log.d(Log.TAG, "dex Path : " + sDexPath);
        Log.d(Log.TAG, "odexPath : " + sOdexPath);
        Log.d(Log.TAG, "lib Path : " + sLibPath);
    }

    private static final void loadClassLoader(Context context) {
        try {
            if (TextUtils.isEmpty(sDexPath)) {
                Log.d(Log.TAG, "Fail to Generate jar file");
                System.exit(0);
            }
            ClassLoader systemParentLoader = context.getClassLoader()
                    .getParent();
            ClassLoader systemLoader = context.getClassLoader();
            Log.d(Log.TAG, "loader0 : " + systemLoader);
            DexClassLoader loader = new DexClassLoader(sDexPath, sOdexPath,
                    sLibPath, systemParentLoader);
            Class s1 = systemLoader.getClass().getSuperclass().getSuperclass();
            Log.d(Log.TAG, "s1 : " + s1);
            Field parentField = s1.getDeclaredField("parent");
            parentField.setAccessible(true);
            parentField.set(systemLoader, loader);
            parentField.setAccessible(false);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(Log.TAG, "error : " + e);
        }
    }

    private static void loadMegJb() {
        try {
            System.loadLibrary("megjb");
        } catch (UnsatisfiedLinkError e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    private static void loadApplication(Context context) {
        try {
            Application app = ((Application) context.getClassLoader()
                    .loadClass("com.cocospay.CocosPayApp")
                    .newInstance());
            Method localMethod = Class
                    .forName("android.content.ContextWrapper")
                    .getDeclaredMethod("attachBaseContext",
                            new Class[] { Context.class });
            localMethod.setAccessible(true);
            localMethod.invoke(app, new Object[] { context });
            localMethod.setAccessible(false);
            Log.d(Log.TAG, "start CocosPayApp.onCreate");
            app.onCreate();
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }
}
