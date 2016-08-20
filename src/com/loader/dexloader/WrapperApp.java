package com.loader.dexloader;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.loader.dexloader.DexConfig.DexState;

import dalvik.system.DexClassLoader;

public class WrapperApp extends Application {

    // 保存ProxyApp的mContext,后面有用
    private static Context sContext = null;

    @Override
    public void onCreate() {
        super.onCreate();
        String className = Application.class.getName();
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(
                    super.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = appInfo.metaData;
            if (bundle != null && bundle.containsKey(DexConfig.APPLICATION_KEY)) {
                className = bundle.getString(DexConfig.APPLICATION_KEY);
                if (className.startsWith(".")) {
                    className = super.getPackageName() + className;
                }
            }
            Log.d(Log.TAG, "Application : " + className);
            Class<?> delegateClass = Class.forName(className, true,
                    getClassLoader());
            Application delegate = (Application) delegateClass.newInstance();
            // 获取当前Application的applicationContext
            Application proxyApplication = (Application) getApplicationContext();

            // 使用反射一一替换proxyApplicationContext，这是本程序的重难点
            // 首先更改proxy的mbaseContext中的成员mOuterContext
            Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
            Field mOuterContext = contextImplClass
                    .getDeclaredField("mOuterContext");
            mOuterContext.setAccessible(true);
            mOuterContext.set(sContext, delegate);
            mOuterContext.setAccessible(false);

            // 再获取context的mPackageInfo变量对象
            Field mPackageInfoField = contextImplClass
                    .getDeclaredField("mPackageInfo");
            mPackageInfoField.setAccessible(true);
            Object mPackageInfo = mPackageInfoField.get(sContext);
            mPackageInfoField.setAccessible(false);

            // 修改mPackageInfo中的成员变量mApplication
            // mPackageInfo是android.app.LoadedApk类
            String packageInfo = null;
            Log.d(Log.TAG, "SDK VERSION : " + Build.VERSION.SDK_INT);
            if (Build.VERSION.SDK_INT > 8) {
                packageInfo = "android.app.LoadedApk";
            } else {
                packageInfo = "android.app.ActivityThread$PackageInfo";
            }
            Class<?> loadedApkClass = Class.forName(packageInfo);
            Field mApplication = loadedApkClass
                    .getDeclaredField("mApplication");
            mApplication.setAccessible(true);
            mApplication.set(mPackageInfo, delegate);
            mApplication.setAccessible(false);

            // 然后再获取mPackageInfo中的成员对象mActivityThread
            Class<?> activityThreadClass = Class
                    .forName("android.app.ActivityThread");
            Field mAcitivityThreadField = loadedApkClass
                    .getDeclaredField("mActivityThread");
            mAcitivityThreadField.setAccessible(true);
            Object mActivityThread = mAcitivityThreadField.get(mPackageInfo);
            mAcitivityThreadField.setAccessible(false);

            // 设置mActivityThread对象中的mInitialApplication
            Field mInitialApplicationField = activityThreadClass
                    .getDeclaredField("mInitialApplication");
            mInitialApplicationField.setAccessible(true);
            mInitialApplicationField.set(mActivityThread, delegate);
            mInitialApplicationField.setAccessible(false);

            // 最后是mActivityThread对象中的mAllApplications，注意这个是List
            Field mAllApplicationsField = activityThreadClass
                    .getDeclaredField("mAllApplications");
            mAllApplicationsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Application> al = (ArrayList<Application>) mAllApplicationsField
                    .get(mActivityThread);

            mAllApplicationsField.setAccessible(false);
            al.add(delegate);
            al.remove(proxyApplication);

            // 设置baseContext并调用onCreate
            Method attach = Application.class.getDeclaredMethod("attach",
                    Context.class);
            attach.setAccessible(true);
            attach.invoke(delegate, sContext);
            attach.setAccessible(false);
            delegate.onCreate();

        } catch (NameNotFoundException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (ClassNotFoundException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (InstantiationException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (NoSuchFieldException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (NoSuchMethodException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (InvocationTargetException e) {
            Log.d(Log.TAG, "error : " + e);
        }
        PatchManager.get(sContext).checkPatch();
    }

    @Override
    public String getPackageName() {
        try{
            return getClass().getPackage().getName();
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return "";
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sContext = base;
        try {
            String odexPath = getDir(DexConfig.APP_DEX_PATH, MODE_PRIVATE)
                    .getAbsolutePath();
            DexConfig config = new DexConfig(this);
            config.extractOriginJarFile();
            String dexPath = config.getExtractJarFilePath();
            String libPath = null;
            try {
                libPath = getApplicationInfo().nativeLibraryDir;
            } catch(NoSuchFieldError e) {
                Log.d(Log.TAG, "error : " + e);
                libPath = new File(getApplicationInfo().dataDir, "lib").getAbsolutePath();
            }
            Log.d(Log.TAG, "dex Path : " + dexPath);
            Log.d(Log.TAG, "lib Path : " + libPath);
            Log.d(Log.TAG, "odexPath : " + odexPath);
            if (TextUtils.isEmpty(dexPath)) {
                Log.d(Log.TAG, "Fail to Generate jar file");
                System.exit(0);
            }
            Class<?> classActivityThread = Class.forName("android.app.ActivityThread");
            Method methodCurrentActivityThread = classActivityThread.getMethod("currentActivityThread");

            Object objectActivityThread = methodCurrentActivityThread.invoke(null);

            Field fieldMPackages = classActivityThread.getDeclaredField("mPackages");
            fieldMPackages.setAccessible(true);
            Object loadedApks = fieldMPackages.get(objectActivityThread);
            fieldMPackages.setAccessible(false);

            Method methodGet = loadedApks.getClass().getMethod("get", Object.class);
            WeakReference<?> wr = (WeakReference<?>) methodGet.invoke(loadedApks, super.getPackageName());

            Log.d(Log.TAG, "Old packageName : " + getPackageName());
            Log.d(Log.TAG, "New packageName : " + super.getPackageName());


            DexState state = config.dexInject();
            Log.d(Log.TAG, "DexState : " + state.name());
            if (state == DexState.DEX_INJECT) {
                DexInjector.initClassLoader(this, dexPath, odexPath, libPath);
            } else if (state == DexState.DEX_REPLACE) {
                DexClassLoader loader = new DexClassLoader(dexPath, odexPath,
                        libPath, getClassLoader());
                Object objLoadedApk = wr.get();
                Class<?> classLoadedApk = objLoadedApk.getClass();
                Field fieldMClassLoader = classLoadedApk
                        .getDeclaredField("mClassLoader");
                fieldMClassLoader.setAccessible(true);
                fieldMClassLoader.set(objLoadedApk, loader);
                fieldMClassLoader.setAccessible(false);
            } else if (state == DexState.DEX_PARENT) {
                ClassLoader systemParentLoader = base.getClassLoader()
                        .getParent();
                ClassLoader systemLoader = base.getClassLoader();
                DexClassLoader loader = new DexClassLoader(dexPath, odexPath,
                        libPath, systemParentLoader);
                Field parentField = null;
                Class<?> s1 = systemLoader.getClass();
                while (parentField == null && s1 != null) {
                    Log.d(Log.TAG, "s1 : " + s1);
                    try {
                        parentField = s1.getDeclaredField("parent");
                    } catch (NoSuchFieldException e) {
                        Log.d(Log.TAG, "error : " + e);
                    }
                    s1 = s1.getSuperclass();
                }
                Log.d(Log.TAG, "parentField : " + parentField);
                parentField.setAccessible(true);
                parentField.set(systemLoader, loader);
                parentField.setAccessible(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(Log.TAG, "error : " + e);
        }
    }
}