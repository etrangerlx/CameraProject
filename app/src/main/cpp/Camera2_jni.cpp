//
// Created by Administrator on 2023/7/19.
//
#include "ndkCamera.h"
#include <jni.h>

#define TAG "NdkCamera"

static ndkCamera *g_camera = nullptr;

extern "C" {
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "JNI_OnLoad");
    if (g_camera == nullptr) {
        g_camera = new ndkCamera;
    }
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "JNI_OnUnload");
    if (g_camera != nullptr) {
        delete g_camera;
        g_camera = nullptr;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_rainbow_camera2demo_NativeCamera2_NativeCamera2Init(JNIEnv *env, jobject thiz) {
    g_camera->init();
//    g_camera->set_window(nullptr);
    return JNI_TRUE;

}

JNIEXPORT jboolean JNICALL
Java_com_rainbow_camera2demo_NativeCamera2_NativeCamera2Uninit(JNIEnv *env, jobject thiz) {
    g_camera->uninit();
//    g_camera->set_window(nullptr);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_rainbow_camera2demo_NativeCamera2_NativeCameraLens(JNIEnv *env, jobject thiz,
                                                            jint facing) {
    if (facing < 0 || facing > 1)
        return JNI_FALSE;
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "openCamera %d", facing);
    g_camera->set_Camera(facing);

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_rainbow_camera2demo_NativeCamera2_NativeCamera2GetSurface(JNIEnv *env, jobject thiz, jobject surface) {
    ANativeWindow *win = ANativeWindow_fromSurface(env, surface);
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "setOutputWindow %p", win);
    g_camera->set_window(win);
    return JNI_TRUE;
}
}