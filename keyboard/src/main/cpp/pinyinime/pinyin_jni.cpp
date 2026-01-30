#include <jni.h>
#include <string.h>
#include <unistd.h>

#include <android/log.h>

#include "include/pinyinime.h"

using namespace ime_pinyin;

#define LOG_TAG "InAppPinyin"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define RET_BUF_LEN 256
static char16 retbuf[RET_BUF_LEN];

static struct {
    jclass mClass;
    jfieldID mDescriptor;
} gFileDescriptorOffsets;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_carbit_inappkeyboard_keyboard_PinyinDecoder_nativeImOpenDecoderFd(
        JNIEnv* env,
        jclass /*clazz*/,
        jobject fd_sys_dict,
        jlong startoffset,
        jlong length,
        jbyteArray fn_usr_dict) {

    jint fd = env->GetIntField(fd_sys_dict, gFileDescriptorOffsets.mDescriptor);
    jbyte* fud = env->GetByteArrayElements(fn_usr_dict, nullptr);

    jboolean jret = JNI_FALSE;
    int newfd = dup(fd);
    if (newfd >= 0) {
        if (im_open_decoder_fd(newfd, (long)startoffset, (long)length, (const char*)fud)) {
            jret = JNI_TRUE;
        } else {
            LOGE("im_open_decoder_fd failed");
        }
        close(newfd);
    } else {
        LOGE("dup(fd) failed");
    }

    env->ReleaseByteArrayElements(fn_usr_dict, fud, 0);
    return jret;
}

extern "C" JNIEXPORT void JNICALL
Java_com_carbit_inappkeyboard_keyboard_PinyinDecoder_nativeImSetMaxLens(
        JNIEnv* /*env*/, jclass /*clazz*/, jint max_sps_len, jint max_hzs_len) {
    im_set_max_lens((size_t)max_sps_len, (size_t)max_hzs_len);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_carbit_inappkeyboard_keyboard_PinyinDecoder_nativeImCloseDecoder(
        JNIEnv* /*env*/, jclass /*clazz*/) {
    im_close_decoder();
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_carbit_inappkeyboard_keyboard_PinyinDecoder_nativeImResetSearch(
        JNIEnv* /*env*/, jclass /*clazz*/) {
    im_reset_search();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_carbit_inappkeyboard_keyboard_PinyinDecoder_nativeImSearch(
        JNIEnv* env, jclass /*clazz*/, jbyteArray pybuf, jint pylen) {
    jbyte* array_body = env->GetByteArrayElements(pybuf, nullptr);
    jint jret = 0;
    if (array_body != nullptr) {
        jret = (jint)im_search((const char*)array_body, (size_t)pylen);
    }
    env->ReleaseByteArrayElements(pybuf, array_body, 0);
    return jret;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_carbit_inappkeyboard_keyboard_PinyinDecoder_nativeImGetChoice(
        JNIEnv* env, jclass /*clazz*/, jint candidateId) {
    if (im_get_candidate(candidateId, retbuf, RET_BUF_LEN)) {
        return env->NewString((unsigned short*)retbuf, (jsize)utf16_strlen(retbuf));
    }
    return env->NewString((unsigned short*)retbuf, 0);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_carbit_inappkeyboard_keyboard_PinyinDecoder_nativeImChoose(
        JNIEnv* /*env*/, jclass /*clazz*/, jint choiceId) {
    return (jint)im_choose(choiceId);
}

static int registerFileDescriptorOffsets(JNIEnv* env) {
    jclass localClass = env->FindClass("java/io/FileDescriptor");
    if (!localClass) return JNI_FALSE;

    gFileDescriptorOffsets.mClass = (jclass)env->NewGlobalRef(localClass);
    env->DeleteLocalRef(localClass);
    if (!gFileDescriptorOffsets.mClass) return JNI_FALSE;

    gFileDescriptorOffsets.mDescriptor = env->GetFieldID(gFileDescriptorOffsets.mClass, "descriptor", "I");
    if (!gFileDescriptorOffsets.mDescriptor) {
        // Some Android versions use 'fd' instead.
        gFileDescriptorOffsets.mDescriptor = env->GetFieldID(gFileDescriptorOffsets.mClass, "fd", "I");
    }
    return gFileDescriptorOffsets.mDescriptor != nullptr;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    if (!registerFileDescriptorOffsets(env)) {
        LOGE("Failed to find FileDescriptor descriptor field");
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
