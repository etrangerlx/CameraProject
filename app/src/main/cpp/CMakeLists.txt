project(NativeCamera2)

cmake_minimum_required(VERSION 3.10)


add_library(NativeCamera2 SHARED Camera2_jni.cpp ndkCamera.cpp)

target_link_libraries(NativeCamera2 camera2ndk mediandk android log)
