#ifndef DOLPHIN_H_
#define DOLPHIN_H_

#include <jni.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <sys/wait.h>
#include <android/log.h>
#include <sys/inotify.h>

#define LOG_TAG "loader"
#define DEBUG

#ifdef DEBUG
#define LOGD(format, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "%s : %s : %d ---> "format"%s",__FILE__,__FUNCTION__,__LINE__,##__VA_ARGS__,"\n");
#else
#define LOGD(format, ...) 0;
#endif

#define LOGE(format, ...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, format"%s",##__VA_ARGS__,"\n");
#define LOGV(format, ...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, format"%s",##__VA_ARGS__,"\n");

static const char *JNIREG_CLASS = "com/loader/dexloader/LoaderUtils";

#endif /* DOLPHIN_H_ */
