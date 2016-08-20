package com.loader.dexloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class PatchManager {

    private static final String SP_DEXLOADER = "_dexloader_config";
    public static final String APP_DEX_PATH = "dex_path";

    private Context mContext;
    private static PatchManager sPatchManager;

    public static PatchManager get(Context context) {
        if (sPatchManager == null) {
            sPatchManager = new PatchManager();
        }
        sPatchManager.mContext = context;
        return sPatchManager;
    }

    public void checkPatch() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.d(Log.TAG, "error : " + e);
                }
                patchRequest();
            }
        };
        thread.start();
    }

    private void patchRequest() {
        if (!Utils.isWifiNetwork(mContext)) {
            Log.d(Log.TAG, "No wifi network");
            return;
        }
        String configUrl = getPatchConfigUrl();
        Log.d(Log.TAG, "config : " + configUrl);
        if (TextUtils.isEmpty(configUrl)) {
            Log.d(Log.TAG, "Patch config url is empty");
            return;
        }
        String patchStr = getPatchConfig(configUrl);
        if (TextUtils.isEmpty(patchStr)) {
            Log.d(Log.TAG, "No patch configed");
            return;
        }
        PatchConfig config = toConfig(patchStr);
        if (config == null) {
            Log.d(Log.TAG, "Can not create PatchConfig");
            return;
        }
        String patchUrl = config.getPatchUrl();
        if (TextUtils.isEmpty(patchUrl)) {
            Log.d(Log.TAG, "Did not config the patch url");
            return;
        }
        String localDex = config.getLocalDex();
        if (TextUtils.isEmpty(localDex)) {
            Log.d(Log.TAG, "Did not config the local dex name");
            return;
        }
        String patchPath = generatePatchPath(localDex);
        if (TextUtils.isEmpty(patchPath)) {
            Log.d(Log.TAG, "Can not locate local patch file");
            return;
        }
        if (checkPatchExist(config, patchPath)) {
            Log.d(Log.TAG, "Patch file exists");
            return;
        }
        boolean result = download(patchUrl, patchPath);
        if (!result) {
            Log.d(Log.TAG, "dowload patch dex failure");
            return;
        }
        // 将patch文件路径放置到预配置的路径
        SharedPreferences sp = mContext.getSharedPreferences(SP_DEXLOADER, Context.MODE_PRIVATE);
        sp.edit().putString(APP_DEX_PATH, patchPath).commit();
        // 当patch文件设置了MD5时，才会强行重启
        if (config.isRestartOnPatch() && !TextUtils.isEmpty(config.getDexMd5())) {
            System.exit(0);
        }
    }


    private URLConnection connect(String webUrl) {
        try {
            URL url = new URL(webUrl);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("charsert", "UTF-8");
            conn.setRequestProperty(
                    "user-agent",
                    "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.8.1.11) Gecko/20071127 Firefox/2.0.0.11");
            conn.setConnectTimeout(10000);
            conn.connect();
            return conn;
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;
    }

    private String getPatchConfig(String configUrl) {
        try {
            URLConnection conn = connect(configUrl);
            InputStream inStream = conn.getInputStream();
            byte buf[] = new byte[1024];
            int read = 0;
            StringBuilder builder = new StringBuilder();
            while ((read = inStream.read(buf)) > 0) {
                builder.append(new String(buf, 0, read));
            }
            inStream.close();
            return builder.toString();
        } catch (MalformedURLException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;
    }

    private boolean download(String remoteUrl, String localPath) {
        try {
            URLConnection conn = connect(remoteUrl);
            InputStream inStream = conn.getInputStream();
            byte buf[] = new byte[1024];
            int read = 0;
            if (TextUtils.isEmpty(localPath)) {
                return false;
            }
            File patchFile = new File(localPath);
            if (patchFile.exists()) {
                patchFile.delete();
            }
            FileOutputStream fos = new FileOutputStream(localPath);
            while ((read = inStream.read(buf)) > 0) {
                fos.write(buf, 0, read);
            }
            fos.close();
            inStream.close();
            return true;
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return false;
    }

    private String generatePatchPath(String localDex) {
        File packagePath = mContext.getFilesDir();
        if (packagePath != null) {
            File patchDir = new File(packagePath, "patch");
            patchDir.mkdirs();
            File patchFile = new File(patchDir, localDex);
            return patchFile.getAbsolutePath();
        }
        return null;
    }

    private PatchConfig toConfig(String str) {
        PatchConfig config = null;
        try {
            JSONObject jobj = new JSONObject(str);
            config = new PatchConfig();
            if (jobj.has("patchUrl")) {
                config.setPatchUrl(jobj.getString("patchUrl"));
            }
            if (jobj.has("localDex")) {
                config.setLocalDex(jobj.getString("localDex"));
            }
            if (jobj.has("dexMd5")) {
                config.setDexMd5(jobj.getString("dexMd5"));
            }
            if (jobj.has("patchVerCode")) {
                config.setPatchVerCode(jobj.getInt("patchVerCode"));
            }
            if (jobj.has("patchVerName")) {
                config.setPatchVerName(jobj.getString("patchVerName"));
            }
            if (jobj.has("restartOnPatch")) {
                config.setRestartOnPatch(jobj.getBoolean("restartOnPatch"));
            }
        } catch(Exception e) {
        }
        return config;
    }

    private boolean checkPatchExist(PatchConfig config, String patchPath) {
        if (TextUtils.isEmpty(patchPath)) {
            return false;
        }
        if (config == null || TextUtils.isEmpty(config.getDexMd5())) {
            return false;
        }
        File patchFile = new File(patchPath);
        if (patchFile != null) {
            if (patchFile.exists()) {
                String patchMd5 = Utils.md5sum(patchPath);
                if (patchMd5 != null && patchMd5.toLowerCase(Locale.CHINESE).equals(config.getDexMd5().toLowerCase(Locale.CHINESE))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getPatchConfigUrl() {
        try {
            int urlId = mContext.getResources().getIdentifier("patch_config_url", "string", mContext.getPackageName());
            return mContext.getResources().getString(urlId);
        } catch(Exception e) {
        }
        return null;
    }
}
