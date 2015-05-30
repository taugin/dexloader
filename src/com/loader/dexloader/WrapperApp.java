package com.loader.dexloader;

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
import android.os.Bundle;
import android.text.TextUtils;
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
            if (bundle != null && bundle.containsKey(DexUtil.APPLICATION_KEY)) {
                className = bundle.getString(DexUtil.APPLICATION_KEY);
                if (className.startsWith(".")) {
                    className = super.getPackageName() + className;
                }
            }
            Log.d(Log.TAG, "Application : " + className);
            Class delegateClass = Class.forName(className, true,
                    getClassLoader());
            Application delegate = (Application) delegateClass.newInstance();
            // 获取当前Application的applicationContext
            Application proxyApplication = (Application) getApplicationContext();

            // 使用反射一一替换proxyApplicationContext，这是本程序的重难点
            // 首先更改proxy的mbaseContext中的成员mOuterContext
            Class contextImplClass = Class.forName("android.app.ContextImpl");
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
            Class loadedApkClass = Class.forName("android.app.LoadedApk");
            Field mApplication = loadedApkClass
                    .getDeclaredField("mApplication");
            mApplication.setAccessible(true);
            mApplication.set(mPackageInfo, delegate);
            mApplication.setAccessible(false);

            // 然后再获取mPackageInfo中的成员对象mActivityThread
            Class activityThreadClass = Class
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
    }

    @Override
    public String getPackageName() {
        return "Package for ContentProvider";
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sContext = base;
        try {
            String odexPath = getDir(DexUtil.APP_DEX_PATH, MODE_PRIVATE)
                    .getAbsolutePath();
            DexUtil util = new DexUtil(this);
            String dexPath = util.extractJarFile();
            String libPath = getApplicationInfo().nativeLibraryDir;
            Log.d(Log.TAG, "dex Path : " + dexPath);
            Log.d(Log.TAG, "lib Path : " + libPath);
            Log.d(Log.TAG, "odexPath : " + odexPath);
            if (TextUtils.isEmpty(dexPath)) {
                Log.d(Log.TAG, "Fail to Generate jar file");
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
                    super.getPackageName());

            Log.d(Log.TAG, "packageName : " + super.getPackageName());
            Object objLoadedApk = wr.get();
            Class classLoadedApk = objLoadedApk.getClass();
            Field fieldMClassLoader = classLoadedApk
                    .getDeclaredField("mClassLoader");
            fieldMClassLoader.setAccessible(true);
            DexClassLoader loader = new DexClassLoader(dexPath, odexPath, libPath, getClassLoader());
            fieldMClassLoader.set(objLoadedApk, loader);
            fieldMClassLoader.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(Log.TAG, "error : " + e);
        }
    }
}