package com.loader.dexloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import dalvik.system.DexClassLoader;

public class LoaderApp extends Application {

    private static final String appkey = "APPLICATION_CLASS_NAME";

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            String odexPath = getDir("odex_path", MODE_PRIVATE)
                    .getAbsolutePath();
            String dexPath = generateSrcDex();
            String libPath = getApplicationInfo().nativeLibraryDir;
            Log.d(Log.TAG, "dexPath : " + dexPath);
            Log.d(Log.TAG, "libPath : " + libPath);
            Log.d(Log.TAG, "odexPath : " + odexPath);
            // 配置动态加载环境
            Object currentActivityThread = RefInvoke.invokeStaticMethod(
                    "android.app.ActivityThread", "currentActivityThread",
                    new Class[] {}, new Object[] {});
            String packageName = this.getPackageName();
            HashMap mPackages = (HashMap) RefInvoke.getFieldOjbect(
                    "android.app.ActivityThread", currentActivityThread,
                    "mPackages");
            WeakReference wr = (WeakReference) mPackages.get(packageName);
            DexClassLoader dLoader = new DexClassLoader(dexPath, odexPath,
                    libPath, (ClassLoader) RefInvoke.getFieldOjbect(
                            "android.app.LoadedApk", wr.get(), "mClassLoader"));
            RefInvoke.setFieldOjbect("android.app.LoadedApk", "mClassLoader",
                    wr.get(), dLoader);

        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    public void onCreate() {

        // 如果源应用配置有Appliction对象，则替换为源应用Applicaiton，以便不影响源程序逻辑。
        String appClassName = null;
        try {
            ApplicationInfo ai = this.getPackageManager().getApplicationInfo(
                    this.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            if (bundle != null && bundle.containsKey("APPLICATION_CLASS_NAME")) {
                appClassName = bundle.getString("APPLICATION_CLASS_NAME");
            } else {
                return;
            }
        } catch (NameNotFoundException e) {
            Log.d(Log.TAG, "error : " + e);
        }

        Object currentActivityThread = RefInvoke.invokeStaticMethod(
                "android.app.ActivityThread", "currentActivityThread",
                new Class[] {}, new Object[] {});
        Object mBoundApplication = RefInvoke.getFieldOjbect(
                "android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");
        Object loadedApkInfo = RefInvoke.getFieldOjbect(
                "android.app.ActivityThread$AppBindData", mBoundApplication,
                "info");
        RefInvoke.setFieldOjbect("android.app.LoadedApk", "mApplication",
                loadedApkInfo, null);
        Object oldApplication = RefInvoke.getFieldOjbect(
                "android.app.ActivityThread", currentActivityThread,
                "mInitialApplication");
        ArrayList<Application> mAllApplications = (ArrayList<Application>) RefInvoke
                .getFieldOjbect("android.app.ActivityThread",
                        currentActivityThread, "mAllApplications");
        mAllApplications.remove(oldApplication);
        ApplicationInfo applicationInfo = (ApplicationInfo) RefInvoke
                .getFieldOjbect("android.app.LoadedApk", loadedApkInfo,
                        "mApplicationInfo");
        ApplicationInfo appInfoInAppBindData = (ApplicationInfo) RefInvoke
                .getFieldOjbect("android.app.ActivityThread$AppBindData",
                        mBoundApplication, "appInfo");
        applicationInfo.className = appClassName;
        appInfoInAppBindData.className = appClassName;
        Application app = (Application) RefInvoke.invokeMethod(
                "android.app.LoadedApk", "makeApplication", loadedApkInfo,
                new Class[] { boolean.class, Instrumentation.class },
                new Object[] { false, null });
        RefInvoke.setFieldOjbect("android.app.ActivityThread",
                "mInitialApplication", currentActivityThread, app);

        HashMap mProviderMap = (HashMap) RefInvoke.getFieldOjbect(
                "android.app.ActivityThread", currentActivityThread,
                "mProviderMap");
        Iterator it = mProviderMap.values().iterator();
        while (it.hasNext()) {
            Object providerClientRecord = it.next();
            Object localProvider = RefInvoke.getFieldOjbect(
                    "android.app.ActivityThread$ProviderClientRecord",
                    providerClientRecord, "mLocalProvider");
            RefInvoke.setFieldOjbect("android.content.ContentProvider",
                    "mContext", localProvider, app);
        }
        Log.d(Log.TAG, "appClassName : " + appClassName);
        app.onCreate();
    }

    private String generateSrcDex() {
        try {
            String srcDexPath = getDir("dex_path", MODE_PRIVATE)
                    .getAbsolutePath() + File.separator + "classes.dex";
            FileOutputStream fis = new FileOutputStream(srcDexPath);
            String assetDex = "classes.dex";
            InputStream is = getAssets().open(assetDex);
            byte buffer[] = new byte[4096];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                fis.write(buffer, 0, read);
            }
            is.close();
            fis.close();
            return srcDexPath;
        } catch (IOException e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;
    }
}