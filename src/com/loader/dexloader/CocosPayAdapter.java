package com.loader.dexloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import dalvik.system.DexClassLoader;

public class CocosPayAdapter {

    private static char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5', '6', '7','8', '9',
            'A', 'B', 'C', 'D', 'E', 'F' };

    private static final String APP_DEX_PATH = "dex_path";
    private static final String DECRYPT_JAR_FILE = "decryptdata.jar";
    // private static final String ENCRYPT_JAR_FILE = "encryptdata.dat";
    private static final String ENCRYPT_JAR_FILE = "com.cocospay.stub.dat";

    private static String sDexPath;
    private static String sOdexPath;
    private static String sLibPath;

    public static final void init(Application app) {
        Context context = app.getBaseContext();
        loadMegJb();
        setDexPath(context);
        loadClassLoader2(context);
        loadApplication(context);

        Log.d(Log.TAG, "loader 1 : " + context.getClassLoader());
        Log.d(Log.TAG, "loader 2 : " + context.getClassLoader().getParent());
        Log.d(Log.TAG, "loader 3 : "
                + context.getClassLoader().getParent().getParent());
    }

    private static void setDexPath(Context context) {
        String odexPath = context.getDir(APP_DEX_PATH, Context.MODE_PRIVATE)
                .getAbsolutePath();
        String dexPath = extractJarFile(context);
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

    private static final void loadClassLoader2(Context context) {
        try {
            if (TextUtils.isEmpty(sDexPath)) {
                Log.d(Log.TAG, "Fail to Generate jar file");
                System.exit(0);
            }
            AdapterHelper.initClassLoader(context, sDexPath, sOdexPath, null);
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

    private static String extractJarFile(Context context) {
        try {
            String srcDexPath = context.getDir(APP_DEX_PATH, Application.MODE_PRIVATE)
                    .getAbsolutePath() + File.separator + DECRYPT_JAR_FILE;

            if (!shouldCopyAssets(context, ENCRYPT_JAR_FILE, srcDexPath)) {
                Log.d(Log.TAG, DECRYPT_JAR_FILE + " is exsit ...");
                return srcDexPath;
            }

            Log.d(Log.TAG, "Delete old file and Copy new file ...");

            try {
                context.deleteFile(APP_DEX_PATH);
            } catch(Exception e) {
                Log.d(Log.TAG, "error : " + e);
            }

            InputStream is = context.getAssets().open(ENCRYPT_JAR_FILE);
            FileOutputStream fis = new FileOutputStream(srcDexPath);
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

    private static boolean shouldCopyAssets(Context context, String assetsFile, String fileName) {
        String md5Assets = md5sumAssetsFile(context, assetsFile);
        String md5File = md5sum(context, fileName);
        Log.d(Log.TAG, "AssetsFile : " + md5Assets + " , md5File : " + md5File);
        if (!TextUtils.isEmpty(md5Assets) && md5Assets.equals(md5File)) {
            return false;
        }
        return true;
    }

    private static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    private static String md5sum(Context context, String filename) {
        InputStream fis;
        byte[] buffer = new byte[1024];
        int numRead = 0;
        MessageDigest md5 = null;
        try {
            fis = new FileInputStream(filename);
            md5 = MessageDigest.getInstance("MD5");
            while ((numRead = fis.read(buffer)) > 0) {
                md5.update(buffer, 0, numRead);
            }
            fis.close();
            return toHexString(md5.digest());
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;
    }

    private static String md5sumAssetsFile(Context context, String filename) {
        InputStream fis;
        byte[] buffer = new byte[1024];
        int numRead = 0;
        MessageDigest md5 = null;
        try {
            fis = context.getAssets().open(filename);
            md5 = MessageDigest.getInstance("MD5");
            while ((numRead = fis.read(buffer)) > 0) {
                md5.update(buffer, 0, numRead);
            }
            fis.close();
            return toHexString(md5.digest());
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;
    }
}
