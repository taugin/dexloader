package com.loader.dexloader;

public class PatchConfig {

    private String patchUrl;
    private String localDex;
    private String dexMd5;
    private int patchVerCode;
    private String patchVerName;
    private boolean restartOnPatch;

    public String getPatchUrl() {
        return patchUrl;
    }

    public void setPatchUrl(String patchUrl) {
        this.patchUrl = patchUrl;
    }

    public String getLocalDex() {
        return localDex;
    }

    public void setLocalDex(String localDex) {
        this.localDex = localDex;
    }

    public String getDexMd5() {
        return dexMd5;
    }

    public void setDexMd5(String dexMd5) {
        this.dexMd5 = dexMd5;
    }

    public int getPatchVerCode() {
        return patchVerCode;
    }

    public void setPatchVerCode(int patchVerCode) {
        this.patchVerCode = patchVerCode;
    }

    public String getPatchVerName() {
        return patchVerName;
    }

    public void setPatchVerName(String patchVerName) {
        this.patchVerName = patchVerName;
    }

    public boolean isRestartOnPatch() {
        return restartOnPatch;
    }

    public void setRestartOnPatch(boolean restartOnPatch) {
        this.restartOnPatch = restartOnPatch;
    }

}
