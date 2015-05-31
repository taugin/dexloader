package com.loader.dexloader;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.text.TextUtils;
import dalvik.system.DexClassLoader;

public class LoaderApp extends Application {

    private Application mApp;

    public void onCreate() {
        super.onCreate();
        try {
            String str2 = getPackageManager().getApplicationInfo(
                    getPackageName(), PackageManager.GET_META_DATA).metaData
                    .getString(LoaderHelper.APPLICATION_KEY);
            mApp = ((Application) getClassLoader().loadClass(str2)
                    .newInstance());
            Method localMethod = Class
                    .forName("android.content.ContextWrapper")
                    .getDeclaredMethod("attachBaseContext",
                            new Class[] { Context.class });
            localMethod.setAccessible(true);
            localMethod.invoke(this.mApp, new Object[] { this });
            localMethod.setAccessible(false);
            mApp.onCreate();
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            String odexPath = getDir(LoaderHelper.APP_DEX_PATH, MODE_PRIVATE)
                    .getAbsolutePath();
            LoaderHelper helper = new LoaderHelper(this);
            String dexPath = helper.extractJarFile();
            String libPath = getApplicationInfo().nativeLibraryDir;
            Log.d(Log.TAG, "dexPath : " + dexPath);
            Log.d(Log.TAG, "libPath : " + libPath);
            Log.d(Log.TAG, "odexPath : " + odexPath);
            if (TextUtils.isEmpty(dexPath)) {
                Log.d(Log.TAG, "Fail to Generate jar file ");
                System.exit(0);
            }
            Class classActivityThread = Class
                    .forName("android.app.ActivityThread");
            Method methodCurrentActivityThread = classActivityThread
                    .getMethod("currentActivityThread");

            Object objectActivityThread = methodCurrentActivityThread
                    .invoke(null);

            Field fieldMPackages = classActivityThread
                    .getDeclaredField("mPackages");
            fieldMPackages.setAccessible(true);
            Object loadedApks = fieldMPackages.get(objectActivityThread);
            fieldMPackages.setAccessible(false);

            Method methodGet = loadedApks.getClass().getMethod("get",
                    Object.class);
            WeakReference wr = (WeakReference) methodGet.invoke(loadedApks,
                    getPackageName());

            Object objLoadedApk = wr.get();
            Class classLoadedApk = objLoadedApk.getClass();
            Field fieldMClassLoader = classLoadedApk
                    .getDeclaredField("mClassLoader");
            fieldMClassLoader.setAccessible(true);
            DexClassLoader loader = new DexClassLoader(dexPath, odexPath, libPath, getClassLoader());
            fieldMClassLoader.set(objLoadedApk, loader);
            fieldMClassLoader.setAccessible(false);
        } catch (Exception e) {
            Log.e(Log.TAG, "error : " + e);
        }
    }

    public void onLowMemory() {
        super.onLowMemory();
        if (mApp != null) {
            mApp.onLowMemory();
        }
    }

    public void onConfigurationChanged(Configuration paramConfiguration) {
        super.onConfigurationChanged(paramConfiguration);
        if (mApp != null) {
            mApp.onConfigurationChanged(paramConfiguration);
        }
    }

    public void onTerminate() {
        super.onTerminate();
        if (mApp != null) {
            mApp.onTerminate();
        }
    }
}