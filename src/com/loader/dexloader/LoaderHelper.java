package com.loader.dexloader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Application;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.Base64;

public class LoaderHelper {

    private char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5', '6', '7','8', '9',
            'A', 'B', 'C', 'D', 'E', 'F' };
    public static final String APPLICATION_KEY = "APPLICATION_CLASS_NAME";
    public static final String APP_DEX_PATH = "dex_path";

    private static final String DECRYPT_JAR_FILE = "decryptdata.jar";
    private static final String LODER_CONFIG_FILE = "loaderconfig.dat";

    private String mEncryptionJarFile = "encryptdata.dat";

    private Context mContext;

    public LoaderHelper(Context context) {
        mContext = context;
        parseLoaderConfig();
    }

    private void parseLoaderConfig() {
        try {
            InputStream is = mContext.getAssets().open(LODER_CONFIG_FILE);
            // is = decodeString(is);
            if (is != null) {
                parseXml(is);
                is.close();
            }
        } catch (IOException e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    public String extractJarFile() {
        try {
            // mContext.deleteFile(APP_DEX_PATH);
            String srcDexPath = mContext.getDir(APP_DEX_PATH, Application.MODE_PRIVATE)
                    .getAbsolutePath() + File.separator + DECRYPT_JAR_FILE;

            if (!shouldCopyAssets(mEncryptionJarFile, srcDexPath)) {
                Log.d(Log.TAG, DECRYPT_JAR_FILE + " is exsit ...");
                return srcDexPath;
            }

            Log.d(Log.TAG, "Delete old file and Copy new file ...");

            InputStream is = mContext.getAssets().open(mEncryptionJarFile);
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

    private boolean shouldCopyAssets(String assetsFile, String fileName) {
        String md5Assets = md5sumAssetsFile(assetsFile);
        String md5File = md5sum(fileName);
        Log.d(Log.TAG, "AssetsFile : " + md5Assets + " , md5File : " + md5File);
        if (!TextUtils.isEmpty(md5Assets) && md5Assets.equals(md5File)) {
            return false;
        }
        return true;
    }

    public String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    public String md5sum(String filename) {
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

    public String md5sumAssetsFile(String filename) {
        InputStream fis;
        byte[] buffer = new byte[1024];
        int numRead = 0;
        MessageDigest md5 = null;
        try {
            fis = mContext.getAssets().open(filename);
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
