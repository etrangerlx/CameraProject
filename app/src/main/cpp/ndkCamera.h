//
// Created by Administrator on 2023/9/25.
//

#ifndef CAMERA2DEMO_NDKCAMERA_H
#define CAMERA2DEMO_NDKCAMERA_H


#include <android/log.h>
#include <android/looper.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/asset_manager_jni.h>
#include <android/sensor.h>

#include <camera/NdkCameraDevice.h>
#include <camera/NdkCameraManager.h>
#include <camera/NdkCameraMetadata.h>

#include <media/NdkImageReader.h>

#include <string>

class ndkCamera {

public:
    ndkCamera();

    int init();

    void uninit();

    virtual ~ndkCamera();

    void ImageProcess(unsigned char *nv21, int nv21_width, int nv21_height);

    void set_window(ANativeWindow *win);

    void set_Camera(bool isFront);

//
    int camera_facing;
    int camera_orientation;
    ANativeWindow *win = nullptr;
private:
    ACameraManager *camera_manager;
    ACameraDevice *camera_device;
    ACameraOutputTarget *image_reader_target;

    AImageReader *image_reader;
    ANativeWindow *image_reader_surface;


    ACaptureRequest *capture_request;
    ACaptureSessionOutputContainer *capture_session_output_container;
    ACaptureSessionOutput *capture_session_output;
    ACameraCaptureSession *capture_session;
};


#endif //CAMERA2DEMO_NDKCAMERA_H
