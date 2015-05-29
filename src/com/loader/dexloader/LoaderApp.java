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

    private static final String APPLICATION_KEY = "APPLICATION_CLASS_NAME";
    private static final String LODER_CONFIG_FILE = "loaderconfig.dat";
    private static final String APP_DEX_PATH = "dex_path";
    private String mEncryptionJarFile = "_dex_data";
    private String mDestJarFile = "_dex_data.jar";
    private Application mApp;

    public void onCreate() {
        super.onCreate();
        try {
            String str2 = getPackageManager().getApplicationInfo(
                    getPackageName(), PackageManager.GET_META_DATA).metaData
                    .getString(APPLICATION_KEY);
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
            String odexPath = getDir(APP_DEX_PATH, MODE_PRIVATE)
                    .getAbsolutePath();
            parseLoaderConfig();
            String dexPath = generateSrcDex();
            String libPath = getApplicationInfo().nativeLibraryDir;
            Log.d(Log.TAG, "dexPath : " + dexPath);
            Log.d(Log.TAG, "libPath : " + libPath);
            Log.d(Log.TAG, "odexPath : " + odexPath);
            if (TextUtils.isEmpty(dexPath)) {
                Log.d(Log.TAG, "Fail to Write " + mEncryptionJarFile);
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
            deleteFile(APP_DEX_PATH);
            String srcDexPath = getDir(APP_DEX_PATH, MODE_PRIVATE)
                    .getAbsolutePath() + File.separator + mDestJarFile;
            FileOutputStream fis = new FileOutputStream(srcDexPath);
            InputStream is = getAssets().open(mEncryptionJarFile);
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
                        mEncryptionJarFile = xmlParser.nextText();
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