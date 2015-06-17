#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>

#include "zip.h"
#include "dexloader.h"

#define BUFFER_SIZE 4096
#define MAX_SIZE 512
#ifdef __cplusplus
extern "C" {
#endif

void readres() {
    LOGD("ReadAssets");
    int i = 0;
    jboolean iscopy;
    const char *mpath = "AAAAA.apk";
    struct zip* apkArchive = zip_open(mpath, 0, NULL);
    struct zip_stat fstat;
    zip_stat_init(&fstat);
    int numFiles = zip_get_num_files(apkArchive);
    LOGD("File numFiles %i \n", numFiles);
    for (i = 0; i < numFiles; i++) {
        const char* name = zip_get_name(apkArchive, i, 0);
        if (name == NULL) {
            LOGE("Error reading zip file name at index %i : %s",
                    zip_strerror(apkArchive));
            return;
        }
        zip_stat(apkArchive, name, 0, &fstat);
        LOGD("File %i:%s Size1: %d Size2: %d", i, fstat.name, fstat.size,
                fstat.comp_size);
    }
    const char *fname = "BBBBB";
    struct zip_file* file = zip_fopen(apkArchive, fname, 0);
    if (!file) {
        LOGE("Error opening %s from APK", fname);
        return;
    }
    zip_stat(apkArchive, fname, 0, &fstat);
    char *buffer = (char *) malloc(fstat.size + 1);
    buffer[fstat.size] = 0;
    int numBytesRead = zip_fread(file, buffer, fstat.size);
    LOGD(": %s\n", buffer);
    free(buffer);
    zip_fclose(file);
    zip_close(apkArchive);
}
unsigned long get_file_size(const char *path) {
    unsigned long filesize = -1;
    FILE *fp;
    fp = fopen(path, "r");
    if(fp == NULL)
        return filesize;
    fseek(fp, 0L, SEEK_END);
    filesize = ftell(fp);
    fclose(fp);
    return filesize;
}

void copyassets(char *apkfile, char *srcfile, char *dstfile) {
    struct zip* apkArchive = zip_open(apkfile, 0, NULL);
    struct zip_stat fstat;
    zip_stat_init(&fstat);
    struct zip_file* file = zip_fopen(apkArchive, srcfile, 0);
    if (!file) {
        LOGE("Error opening %s from APK", srcfile);
        return;
    }
    zip_stat(apkArchive, srcfile, 0, &fstat);
    unsigned long filesize = get_file_size(dstfile);
    LOGD("filesize : %ld", filesize);
    if (fstat.size == filesize) {
        goto end;
    }

    char *buffer = (char *) malloc(BUFFER_SIZE);
    int numBytesRead = 0;
    int numByteWrite = 0;
    FILE *pFile = fopen(dstfile, "wb");
    if (!pFile) {
        LOGD("Can not open %s", dstfile);
        goto end;
    }
    long writesize = 0;
    long leftsize = fstat.size;
    while(leftsize > 0) {
        zip_fread(file, buffer, BUFFER_SIZE);
        writesize = leftsize / BUFFER_SIZE > 0 ? BUFFER_SIZE : leftsize % BUFFER_SIZE;
        numByteWrite = fwrite(buffer, 1, writesize, pFile);
        // LOGD("numByteWrite : %d, writesize : %ld, leftsize : %ld", numByteWrite, writesize, leftsize);
        leftsize -= writesize;
        memset(buffer, 0, BUFFER_SIZE);
    }
    fclose(pFile);
    LOGD(": %d\n", fstat.size);
end:
    free(buffer);
    zip_fclose(file);
    zip_close(apkArchive);
}

void change_classloader(JNIEnv *env, jobject obj, jstring pkgname, jstring libdir, jobject clsloader) {
	jclass ActivityThread = (*env)->FindClass(env, "android/app/ActivityThread");
	jmethodID currentActivityThread = (*env)->GetStaticMethodID(env, ActivityThread, "currentActivityThread", "()Landroid/app/ActivityThread;");
	jobject currentActivityThreadObj = (*env)->CallStaticObjectMethod(env, ActivityThread, currentActivityThread);
	jmethodID peekPackageInfoId = (*env)->GetMethodID(env, ActivityThread, "peekPackageInfo", "(Ljava/lang/String;Z)Landroid/app/LoadedApk;");
	jobject packageInfo = (*env)->CallObjectMethod(env, currentActivityThreadObj, peekPackageInfoId, pkgname, 1);
	jclass clsLoader = (*env)->FindClass(env, "dalvik/system/DexClassLoader");
	jmethodID constructor = (*env)->GetMethodID(env, clsLoader, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)V");

    char package_name[256];
    const char *tmp1 = (*env)->GetStringUTFChars(env, pkgname, 0);
    strcpy(package_name, tmp1);
    (*env)->ReleaseStringUTFChars(env, pkgname, tmp1);

	char dexpath_c[256] = {0};
	sprintf(dexpath_c, "/data/data/%s/.cache/dex/", package_name);
	jstring odexpath = (*env)->NewStringUTF(env, dexpath_c);

	strcat(dexpath_c, "decryptdata.jar");
	jstring dexpath = (*env)->NewStringUTF(env, dexpath_c);
	jobject clsLoaderObj = (*env)->NewObject(env, clsLoader, constructor, dexpath, odexpath, libdir, clsloader);
	(*env)->DeleteLocalRef(env, dexpath);
	(*env)->DeleteLocalRef(env, odexpath);

	jclass loadedApkcls = (*env)->FindClass(env, "android/app/LoadedApk");
	jfieldID clsloaderId = (*env)->GetFieldID(env, loadedApkcls, "mClassLoader", "Ljava/lang/ClassLoader;");
	(*env)->SetObjectField(env, packageInfo, clsloaderId, clsLoaderObj);
	(*env)->DeleteLocalRef(env, clsLoaderObj);
}

JNIEXPORT void JNICALL native_attatch(JNIEnv *env, jobject obj, jobject app,
		jstring pkgname, jstring apkfile, jstring libdir, jobject clsloader) {
    char package_name[256];
    const char *tmp1 = (*env)->GetStringUTFChars(env, pkgname, 0);
    strcpy(package_name, tmp1);
    (*env)->ReleaseStringUTFChars(env, pkgname, tmp1);
    LOGD("%s", package_name);

    char package_file[256];
    const char *tmp2 = (*env)->GetStringUTFChars(env, apkfile, 0);
    strcpy(package_file, tmp2);
    (*env)->ReleaseStringUTFChars(env, apkfile, tmp2);
    LOGD("%s", package_file);

    char dstdexpath[256] = {0};
    char dstdexfile[] = "decryptdata.jar";
    sprintf(dstdexpath, "/data/data/%s/.cache", package_name);
    LOGD("%s", dstdexpath);
    int status = mkdir(dstdexpath, 0755);
    LOGD("status : %d", status);
    strcat(dstdexpath, "/dex");
    LOGD("%s", dstdexpath);
    status = mkdir(dstdexpath, 0755);
    LOGD("status : %d", status);
    strcat(dstdexpath, "/");
    strcat(dstdexpath, dstdexfile);
    LOGD("%s", dstdexpath);
    copyassets(package_file, "assets/encryptdata.dat", dstdexpath);
    change_classloader(env, obj, pkgname, libdir, clsloader);
}

static JNINativeMethod gMethods[] = {
    { "attatch",         "(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)V", (void*)native_attatch }
};

/*
* Register several native methods for one class.
*/
static int registerNativeMethods(JNIEnv* env, const char* className,
        JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;
    clazz = (*env)->FindClass(env, className);
    if (clazz == NULL) {
        LOGD("clazz == NULL");
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, gMethods, numMethods) < 0) {
        LOGD("RegisterNatives result < 0");
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
* Register native methods for all classes we know about.
*/
static int registerNatives(JNIEnv* env)
{
    if (!registerNativeMethods(env, JNIREG_CLASS, gMethods, sizeof(gMethods) / sizeof(gMethods[0]))) {
        LOGD("RegisterNatives result < 0");
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = NULL;
    jint result = -1;

    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGD("GetEnv != JNI_OK");
        return -1;
    }
    assert(env != NULL);

    if (!registerNatives(env)) {//注册
        LOGD("registerNatives Failure");
        return -1;
    }
    /* success -- return valid version number */
    result = JNI_VERSION_1_4;
    LOGD("Native function register success");
    return result;
}

#ifdef __cplusplus
}
#endif
