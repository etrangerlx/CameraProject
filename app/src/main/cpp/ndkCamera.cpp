//
// Created by Administrator on 2023/9/25.
//

#include "ndkCamera.h"

static void onDisconnected(void *context, ACameraDevice *device) {
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "onDisconnected %p", device);
}

static void onError(void *context, ACameraDevice *device, int error) {
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "onError %p %d", device, error);
}

static void onSessionActive(void *context, ACameraCaptureSession *session) {
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "onSessionActive %p", session);
}

static void onSessionReady(void *context, ACameraCaptureSession *session) {
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "onSessionReady %p", session);
}

static void onSessionClosed(void *context, ACameraCaptureSession *session) {
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "onSessionClosed %p", session);
}

void onCaptureCompleted(void *context, ACameraCaptureSession *session, ACaptureRequest *request,
                        const ACameraMetadata *result) {
//     __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "onCaptureCompleted %p %p %p", session, request, result);
}

void onCaptureFailed(void *context, ACameraCaptureSession *session, ACaptureRequest *request,
                     ACameraCaptureFailure *failure) {
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "onCaptureFailed %p %p %p", session, request,
                        failure);
}

void onCaptureSequenceCompleted(void *context, ACameraCaptureSession *session, int sequenceId,
                                int64_t frameNumber) {
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "onCaptureSequenceCompleted %p %d %ld",
                        session, sequenceId, frameNumber);
}

void onCaptureSequenceAborted(void *context, ACameraCaptureSession *session, int sequenceId) {
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "onCaptureSequenceAborted %p %d", session,
                        sequenceId);
}

static void onImageAvailable(void *context, AImageReader *reader) {
//     __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "onImageAvailable %p", reader);

    AImage *image = nullptr;
    media_status_t status = AImageReader_acquireLatestImage(reader, &image);

    if (status != AMEDIA_OK) {
        // error
        return;
    }

    int32_t format;
    AImage_getFormat(image, &format);

    int32_t width = 0;
    int32_t height = 0;
    AImage_getWidth(image, &width);
    AImage_getHeight(image, &height);
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "image format:%d,size:(%dx%d)", format,
                        width, height);
    // assert format == AIMAGE_FORMAT_YUV_420_888

    int32_t y_pixelStride = 0;
    int32_t u_pixelStride = 0;
    int32_t v_pixelStride = 0;
    AImage_getPlanePixelStride(image, 0, &y_pixelStride);
    AImage_getPlanePixelStride(image, 1, &u_pixelStride);
    AImage_getPlanePixelStride(image, 2, &v_pixelStride);
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "image stride Y:%d,U:%d,V:%d", y_pixelStride,
                        u_pixelStride, v_pixelStride);

    int32_t y_rowStride = 0;
    int32_t u_rowStride = 0;
    int32_t v_rowStride = 0;
    AImage_getPlaneRowStride(image, 0, &y_rowStride);
    AImage_getPlaneRowStride(image, 1, &u_rowStride);
    AImage_getPlaneRowStride(image, 2, &v_rowStride);
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "image PlaneRowStride Y:%d,U:%d,V:%d",
                        y_rowStride, u_rowStride, v_rowStride);

    uint8_t *y_data = nullptr;
    uint8_t *u_data = nullptr;
    uint8_t *v_data = nullptr;
    int y_len = 0;
    int u_len = 0;
    int v_len = 0;
    AImage_getPlaneData(image, 0, &y_data, &y_len);
    AImage_getPlaneData(image, 1, &u_data, &u_len);
    AImage_getPlaneData(image, 2, &v_data, &v_len);
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "image Planelength Y:%d,U:%d,V:%d", y_len,
                        u_len, v_len);

    if (u_data == v_data + 1 && v_data == y_data + width * height && y_pixelStride == 1 &&
        u_pixelStride == 2 && v_pixelStride == 2 && y_rowStride == width && u_rowStride == width &&
        v_rowStride == width) {
        // already nv21  :)
        ((ndkCamera *) context)->ImageProcess((unsigned char *) y_data, (int) width, (int) height);
    } else {
        // construct nv21
        unsigned char *nv21 = new unsigned char[width * height + width * height / 2];
        {
            // Y
            unsigned char *yptr = nv21;
            for (int y = 0; y < height; y++) {
                const unsigned char *y_data_ptr = y_data + y_rowStride * y;
                for (int x = 0; x < width; x++) {
                    yptr[0] = y_data_ptr[0];
                    yptr++;
                    y_data_ptr += y_pixelStride;
                }
            }
            // UV
            unsigned char *uvptr = nv21 + width * height;
            for (int y = 0; y < height / 2; y++) {
                const unsigned char *v_data_ptr = v_data + v_rowStride * y;
                const unsigned char *u_data_ptr = u_data + u_rowStride * y;
                for (int x = 0; x < width / 2; x++) {
                    uvptr[0] = v_data_ptr[0];
                    uvptr[1] = u_data_ptr[0];
                    uvptr += 2;
                    v_data_ptr += v_pixelStride;
                    u_data_ptr += u_pixelStride;
                }
            }
        }
        ((ndkCamera *) context)->ImageProcess((unsigned char *) nv21, (int) width, (int) height);
        if (nv21 != nullptr) {
            delete[] nv21;
            nv21 = nullptr;
        }
    }

    AImage_delete(image);
}

static void NV2RGB(const unsigned char *in, unsigned char *out, int InWidth, int InHeight) {
    int R, G, B, Y, U, V;
    unsigned char *pOut = out;
    unsigned char *pYIn = const_cast<unsigned char *>(in);
    unsigned char *pUVIn = const_cast<unsigned char *>(in + InHeight * InWidth);
    int j = 0;
    for (; j < InHeight; j++) {
        unsigned char *pYLine = pYIn + j * InWidth;
        unsigned char *pUVLine = pUVIn + ((int) (ceil(j / 2))) * InWidth;
        int i = 0;
        for (; i < InWidth; i++) {
            Y = pYLine[i];
            U = pUVLine[i - i % 2];
            V = pUVLine[i - i % 2 + 1];
            R = Y + (140 * (U - 128)) / 100;  //r
            G = Y - (34 * (V - 128)) / 100 - (71 * (U - 128)) / 100; //g
            B = Y + (177 * (V - 128)) / 100; //b
            R = R < 0 ? 0 : R;
            R = R > 255 ? 255 : R;
            G = G < 0 ? 0 : G;
            G = G > 255 ? 255 : G;
            B = B < 0 ? 0 : B;
            B = B > 255 ? 255 : B;
            pOut[((i) * InHeight + (InHeight - j)) * 3 + 2] = (unsigned char) B;
            pOut[((i) * InHeight + (InHeight - j)) * 3 + 1] = (unsigned char) G;
            pOut[((i) * InHeight + (InHeight - j)) * 3 + 0] = (unsigned char) R;
        }
    }
}

ndkCamera::ndkCamera() {
    win = nullptr;
    image_reader = nullptr;
    image_reader_surface = nullptr;

    camera_manager = nullptr;
    camera_device = nullptr;
    camera_facing = ACAMERA_LENS_FACING_FRONT;
    image_reader_target = nullptr;

    capture_request = nullptr;
    capture_session_output_container = nullptr;
    capture_session_output = nullptr;
    capture_session = nullptr;
    {
        AImageReader_new(1080, 1080, AIMAGE_FORMAT_YUV_420_888, 2, &image_reader);

        AImageReader_ImageListener listener;

        listener.context = this;

        listener.onImageAvailable = onImageAvailable;

        AImageReader_setImageListener(image_reader, &listener);

        AImageReader_getWindow(image_reader, &image_reader_surface);

        ANativeWindow_acquire(image_reader_surface);
    }
}

ndkCamera::~ndkCamera() {
    uninit();
    if (image_reader) {
        AImageReader_delete(image_reader);
        image_reader = 0;
    }

    if (image_reader_surface) {
        ANativeWindow_release(image_reader_surface);
        image_reader_surface = 0;
    }
    if (win) {
        ANativeWindow_release(win);
    }
}

void nearst_inner(unsigned char *pInImage, int Width, int Height, float x_float, float y_float,
                  unsigned char *pOutImage) {

}

bool NearstScaleImage(unsigned char *pInImge, int InWidth, int InHeight, unsigned char *pOutImage,
                      int OutWidth, int OutHeight) {
    const float x_a = float(InWidth) / OutWidth;
    const float y_a = float(InHeight) / OutHeight;
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "%d/%d=xscale:%f,yscale:%f", InWidth,
                        OutWidth, x_a, y_a);
    for (int i = 0; i < OutHeight; i++) {
        float y_float = i * y_a;
        for (int j = 0; j < OutWidth; j++) {
            float x_float = j * x_a;
            int x = (int) (x_float + 0.5);   //四舍五入
            int y = (int) (y_float + 0.5);   //四舍五入
            pOutImage[3 * (i * OutWidth + j) + 0] = *(pInImge + (y * InWidth + x) * 3 + 0);
            pOutImage[3 * (i * OutWidth + j) + 1] = *(pInImge + (y * InWidth + x) * 3 + 1);
            pOutImage[3 * (i * OutWidth + j) + 2] = *(pInImge + (y * InWidth + x) * 3 + 2);
        }
    }
    return true;
}

void
ndkCamera::ImageProcess(unsigned char *nv21, int nv21_width, int nv21_height) {

    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "win:%p,input Image:(%dx%d)",win, nv21_width,
                        nv21_height);
    if(win == nullptr) {
        return;
    }
    int win_w = ANativeWindow_getWidth(win);
    int win_h = ANativeWindow_getHeight(win);
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "win:(%dx%d)", win_w, win_h);
    ANativeWindow_setBuffersGeometry(win, win_w, win_h, AHARDWAREBUFFER_FORMAT_R8G8B8_UNORM);
    ANativeWindow_Buffer buf;
    ANativeWindow_lock(win, &buf, NULL);
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "ANativeWindow_Buffer:(%ldx%ldx%ld)",
                        buf.width, buf.height, buf.stride);
    unsigned char *rgb = new unsigned char[nv21_width * nv21_height * 3];
    unsigned char *rgbOut = new unsigned char[buf.width * buf.width * 3];
    NV2RGB(nv21, rgb, nv21_width, nv21_height);
    NearstScaleImage(rgb, nv21_width, nv21_height, rgbOut, buf.width, buf.width);
//    // scale to target size
    if (buf.format == AHARDWAREBUFFER_FORMAT_R8G8B8_UNORM) {
        for (int y = 0; y < buf.width; y++) {
            const unsigned char *ptr = rgbOut + (y) * buf.width * 3;
            unsigned char *outptr = (unsigned char *) buf.bits + buf.stride * y * 3;
            int x = 0;
            for (; x < buf.width; x++) {
                outptr[0] = ptr[0];
                outptr[1] = ptr[1];
                outptr[2] = ptr[2];
//                outptr[3] = 255;
                ptr += 3;
                outptr += 3;
            }
        }

    }
    if (rgb != nullptr) {
        delete rgb;
        rgb = nullptr;
    }
    if (rgbOut != nullptr) {
        delete rgbOut;
        rgbOut = nullptr;
    }
    ANativeWindow_unlockAndPost(win);
}

int ndkCamera::init() {

    camera_manager = ACameraManager_create();
    //1、 find camera lists
    std::string camera_id;
    {
        ACameraIdList *camera_id_list = nullptr;
        ACameraManager_getCameraIdList(camera_manager, &camera_id_list);

        for (int i = 0; i < camera_id_list->numCameras; ++i) {
            const char *id = camera_id_list->cameraIds[i];
            ACameraMetadata *camera_metadata = 0;
            ACameraManager_getCameraCharacteristics(camera_manager, id, &camera_metadata);

            // query faceing
            acamera_metadata_enum_android_lens_facing_t facing = ACAMERA_LENS_FACING_FRONT;
            {
                ACameraMetadata_const_entry e = {0};
                ACameraMetadata_getConstEntry(camera_metadata, ACAMERA_LENS_FACING, &e);
                facing = (acamera_metadata_enum_android_lens_facing_t) e.data.u8[0];
            }

            if (camera_facing == 0 && facing != ACAMERA_LENS_FACING_FRONT) {
                ACameraMetadata_free(camera_metadata);
                continue;
            }

            if (camera_facing == 1 && facing != ACAMERA_LENS_FACING_BACK) {
                ACameraMetadata_free(camera_metadata);
                continue;
            }
            // get camera id
            camera_id = id;

            // query orientation
            int orientation = 0;
            {
                ACameraMetadata_const_entry e = {0};
                ACameraMetadata_getConstEntry(camera_metadata, ACAMERA_SENSOR_ORIENTATION, &e);

                orientation = (int) e.data.i32[0];
            }

            camera_orientation = orientation;

            ACameraMetadata_free(camera_metadata);

            break;
        }

        ACameraManager_deleteCameraIdList(camera_id_list);
    }
    //2、 open camera device
    {
        ACameraDevice_StateCallbacks camera_device_state_callbacks;
        camera_device_state_callbacks.context = this;
        camera_device_state_callbacks.onDisconnected = onDisconnected;
        camera_device_state_callbacks.onError = onError;
        ACameraManager_openCamera(camera_manager, camera_id.c_str(), &camera_device_state_callbacks,
                                  &camera_device);
    }
    //3、 capture request
    {
        ACameraDevice_createCaptureRequest(camera_device, TEMPLATE_PREVIEW, &capture_request);
        ACameraOutputTarget_create(image_reader_surface, &image_reader_target);
        ACaptureRequest_addTarget(capture_request, image_reader_target);
    }
    //4、 capture session
    {
        ACameraCaptureSession_stateCallbacks camera_capture_session_state_callbacks;
        camera_capture_session_state_callbacks.context = this;
        camera_capture_session_state_callbacks.onActive = onSessionActive;
        camera_capture_session_state_callbacks.onReady = onSessionReady;
        camera_capture_session_state_callbacks.onClosed = onSessionClosed;

        ACaptureSessionOutputContainer_create(&capture_session_output_container);

        ACaptureSessionOutput_create(image_reader_surface, &capture_session_output);

        ACaptureSessionOutputContainer_add(capture_session_output_container,
                                           capture_session_output);

        ACameraDevice_createCaptureSession(camera_device, capture_session_output_container,
                                           &camera_capture_session_state_callbacks,
                                           &capture_session);

        ACameraCaptureSession_captureCallbacks camera_capture_session_capture_callbacks;
        camera_capture_session_capture_callbacks.context = this;
        camera_capture_session_capture_callbacks.onCaptureStarted = nullptr;
        camera_capture_session_capture_callbacks.onCaptureProgressed = nullptr;
        camera_capture_session_capture_callbacks.onCaptureCompleted = onCaptureCompleted;
        camera_capture_session_capture_callbacks.onCaptureFailed = onCaptureFailed;
        camera_capture_session_capture_callbacks.onCaptureSequenceCompleted = onCaptureSequenceCompleted;
        camera_capture_session_capture_callbacks.onCaptureSequenceAborted = onCaptureSequenceAborted;
        camera_capture_session_capture_callbacks.onCaptureBufferLost = nullptr;

        ACameraCaptureSession_setRepeatingRequest(capture_session,
                                                  &camera_capture_session_capture_callbacks, 1,
                                                  &capture_request, nullptr);
    }
    return 0;
}

void ndkCamera::uninit() {
    if (capture_session) {
        ACameraCaptureSession_stopRepeating(capture_session);
        ACameraCaptureSession_close(capture_session);
        capture_session = nullptr;
    }

    if (camera_device) {
        ACameraDevice_close(camera_device);
        camera_device = nullptr;
    }

    if (capture_session_output_container) {
        ACaptureSessionOutputContainer_free(capture_session_output_container);
        capture_session_output_container = nullptr;
    }

    if (capture_session_output) {
        ACaptureSessionOutput_free(capture_session_output);
        capture_session_output = nullptr;
    }

    if (capture_request) {
        ACaptureRequest_free(capture_request);
        capture_request = nullptr;
    }

    if (image_reader_target) {
        ACameraOutputTarget_free(image_reader_target);
        image_reader_target = nullptr;
    }

    if (camera_manager) {
        ACameraManager_delete(camera_manager);
        camera_manager = nullptr;
    }
}

void ndkCamera::set_window(ANativeWindow *_win) {
    if (win) {
        __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "win:%p",win);
        ANativeWindow_release(win);
    }
    __android_log_print(ANDROID_LOG_WARN, "NdkCamera", "_win:%p",_win);
    win = _win;
    ANativeWindow_acquire(win);
}

void ndkCamera::set_Camera(bool isFront) {
    camera_facing = isFront;
}

