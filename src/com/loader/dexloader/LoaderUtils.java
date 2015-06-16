package com.loader.dexloader;

import android.content.Context;

public class LoaderUtils {

    static {
        try {
            System.loadLibrary("dexloader");
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    public static native void attatch(Context context, String pkgName,
            String apkFile);
}
