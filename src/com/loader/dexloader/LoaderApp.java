package com.loader.dexloader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.Base64;
import dalvik.system.DexClassLoader;

public class LoaderApp extends Application {

    private static final String appkey = "APPLICATION_CLASS_NAME";
    private static final String LODER_CONFIG_FILE = "loaderconfig.dat";
    private String mEncryptionDexFile = "classes.dex";
    private DexClassLoader mDexClassLoader;
    private Application mApp;

    /*
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            String odexPath = getDir("odex_path", MODE_PRIVATE)
                    .getAbsolutePath();
            parseLoaderConfig();
            String dexPath = generateSrcDex();
            String libPath = getApplicationInfo().nativeLibraryDir;
            Log.d(Log.TAG, "dexPath : " + dexPath);
            Log.d(Log.TAG, "libPath : " + libPath);
            Log.d(Log.TAG, "odexPath : " + odexPath);
            if (TextUtils.isEmpty(dexPath)) {
                Log.d(Log.TAG, "Fail to Write " + mEncryptionDexFile);
                System.exit(0);
            }
            // 配置动态加载环境
            Object currentActivityThread = RefInvoke.invokeStaticMethod(
                    "android.app.ActivityThread", "currentActivityThread",
                    new Class[] {}, new Object[] {});
            String packageName = this.getPackageName();
            HashMap mPackages = (HashMap) RefInvoke.getFieldOjbect(
                    "android.app.ActivityThread", currentActivityThread,
                    "mPackages");
            WeakReference wr = (WeakReference) mPackages.get(packageName);
            mDexClassLoader = new DexClassLoader(dexPath, odexPath,
                    libPath, (ClassLoader) RefInvoke.getFieldOjbect(
                            "android.app.LoadedApk", wr.get(), "mClassLoader"));
            RefInvoke.setFieldOjbect("android.app.LoadedApk", "mClassLoader",
                    wr.get(), mDexClassLoader);

        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }*/

    /*
    public void onCreate() {
        super.onCreate();
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
        Log.d(Log.TAG, "app : " + app);
        app.onCreate();
    }*/
    public void onCreate() {
        super.onCreate();
        setApkClassLoader();
        try {
            String str2 = getPackageManager().getApplicationInfo(
                    getPackageName(), PackageManager.GET_META_DATA).metaData
                    .getString(appkey);
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

    public void setApkClassLoader() {
        try {
            String odexPath = getDir("odex_path", MODE_PRIVATE)
                    .getAbsolutePath();
            parseLoaderConfig();
            String dexPath = generateSrcDex();
            String libPath = getApplicationInfo().nativeLibraryDir;
            Log.d(Log.TAG, "dexPath : " + dexPath);
            Log.d(Log.TAG, "libPath : " + libPath);
            Log.d(Log.TAG, "odexPath : " + odexPath);
            if (TextUtils.isEmpty(dexPath)) {
                Log.d(Log.TAG, "Fail to Write " + mEncryptionDexFile);
                System.exit(0);
            }
            Class localClass = Class.forName("android.app.ActivityThread");
            Object localObject1 = localClass.getMethod("currentActivityThread",
                    new Class[0]).invoke(null, new Object[0]);
            Field localField1 = localClass.getDeclaredField("mPackages");
            localField1.setAccessible(true);
            Object localObject2 = localField1.get(localObject1);
            localField1.setAccessible(false);
            Method localMethod = localObject2.getClass().getMethod("get",
                    new Class[] { Object.class });
            Object[] arrayOfObject = new Object[1];
            arrayOfObject[0] = getPackageName();
            Object localObject3 = ((WeakReference) localMethod.invoke(
                    localObject2, arrayOfObject)).get();
            Field localField2 = localObject3.getClass().getDeclaredField(
                    "mClassLoader");
            mDexClassLoader = new DexClassLoader(dexPath, odexPath, libPath,
                    getClassLoader());
            localField2.setAccessible(true);
            localField2.set(localObject3, mDexClassLoader);
            localField2.setAccessible(false);
            return;
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

    private void parseLoaderConfig() {
        try {
            InputStream is = getAssets().open(LODER_CONFIG_FILE);
            // is = decodeString(is);
            if (is != null) {
                parseXml(is);
                is.close();
            }
        } catch (IOException e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    private String generateSrcDex() {
        try {
            String srcDexPath = getDir("dex_path", MODE_PRIVATE)
                    .getAbsolutePath() + File.separator + mEncryptionDexFile;
            FileOutputStream fis = new FileOutputStream(srcDexPath);
            InputStream is = getAssets().open(mEncryptionDexFile);
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

    private void parseXml(InputStream in) {
        int eventType;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xmlParser = factory.newPullParser();
            xmlParser.setInput(in, "UTF-8");
            eventType = xmlParser.getEventType();
            String strName = null;
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG) {
                    strName = xmlParser.getName();
                    if ("dexfile".equalsIgnoreCase(strName)) {
                        mEncryptionDexFile = xmlParser.nextText();
                    }
                }
                eventType = xmlParser.next();
            }
        } catch (XmlPullParserException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IOException e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    private InputStream decodeString(InputStream is) {
        if (is == null) {
            return null;
        }
        try {
            byte[] buffer = new byte[1024];
            int read = -1;
            StringBuilder builder = new StringBuilder();
            while ((read = is.read(buffer)) > 0) {
                builder.append(new String(buffer, 0, read, "UTF-8"));
            }
            is.close();
            if (builder.length() > 0) {
                byte[] decodedString = Base64.decode(builder.toString(),
                        Base64.DEFAULT);
                InputStream inputStream = new ByteArrayInputStream(
                        decodedString);
                return inputStream;
            }
        } catch (IOException e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;
    }
}