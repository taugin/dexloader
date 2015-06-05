package com.loader.dexloader;

public class LoaderBridge {

    static {
        try {
            System.loadLibrary("dbridge");
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }
}
