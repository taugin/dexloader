#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>

#include "zip.h"
#include "dbridge.h"

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
JNIEXPORT void JNICALL native_init(JNIEnv *env, jobject obj) {
}

JNIEXPORT void JNICALL get_pkgname(JNIEnv *env, jobject obj) {
	int pipes[2];
	int rc = pipe(pipes);
	if( rc == -1 ) {
		perror( "pipes" );
		exit( 1 );
	}
	char buffer[1024 + 1];
	int si;
	pid_t pid = fork();
	if (pid == 0) {
		LOGD("child pid : %d", pid);
		close(pipes[0]);
		dup2(pipes[1], 1);
		dup2(pipes[1], 2);
		execlp("pm", "pm", "list", "packages", (char *) NULL);
		close(pipes[1]);
	} else if (pid > 0) {
		LOGD("Parent pid : %d", pid);
		int cpid;
		wait(&cpid);
		close(pipes[1]);
		si = 1;
		while(si > 0) {
			si = read(pipes[0], buffer, 1024);
			buffer[si] = '\0';
			LOGD("%s", buffer);
		}
		LOGD("Run the driver install shell finished.\n");
	}

}

static JNINativeMethod gMethods[] = {
	{ "getpkg", 		"()V", (void*)get_pkgname }
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
