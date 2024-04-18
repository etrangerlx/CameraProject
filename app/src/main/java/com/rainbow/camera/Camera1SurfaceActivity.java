package com.rainbow.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.List;

public class Camera1SurfaceActivity extends AppCompatActivity {
    private static String Tag = "Camera1Activity";
    private Camera mCamera  = null;
    private Camera.Parameters mParameters  = null;
    private Camera.CameraInfo mCameraInfo = null;
    private Camera.Size mCameraSize  = null;

    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mSurfaceHolder = null;
    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {

        /**
         *  在 Surface 首次创建时被立即调用：活得叫焦点时。一般在这里开启画图的线程
         * @param surfaceHolder 持有当前 Surface 的 SurfaceHolder 对象
         */
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            Log.i(Tag,"surfaceCreated");
            if(mCameraInfo == null) {
                mCameraInfo = new Camera.CameraInfo();
            }
            mCameraInfo.facing = Camera.CameraInfo.CAMERA_FACING_FRONT;

            mCamera = Camera.open(mCameraInfo.facing);

        }

        /**
         *  在 Surface 格式 和 大小发生变化时会立即调用，可以在这个方法中更新 Surface
         * @param surfaceHolder   持有当前 Surface 的 SurfaceHolder 对象
         * @param format          surface 的新格式
         * @param width           surface 的新宽度
         * @param height          surface 的新高度
         */
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            Log.i(Tag,"surfaceChanged");
            changeCamera(surfaceHolder);
            initCamera(width, height);
            try {
                mCamera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         *  在 Surface 被销毁时立即调用：失去焦点时。一般在这里将画图的线程停止销毁
         * @param surfaceHolder 持有当前 Surface 的 SurfaceHolder 对象
         */
        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            Log.i(Tag,"surfaceDestroyed");
            mCamera.stopPreview();
        }
    };

    public void changeCamera(SurfaceHolder holder) {
        //切换前后摄像头
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            //现在是后置，变更为前置
            mCamera.stopPreview();//停掉原来摄像头的预览
            mCamera.release();//释放资源
            mCamera = null;//取消原来摄像头
            mCameraInfo.facing = Camera.CameraInfo.CAMERA_FACING_FRONT;
            mCamera = Camera.open(mCameraInfo.facing);//打开当前选中的摄像头
            try {
                mCamera.setPreviewDisplay(holder);
                initCamera(mCameraSize.width, mCameraSize.height);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            //现在是前置， 变更为后置
            mCamera.stopPreview();//停掉原来摄像头的预览
            mCamera.release();//释放资源
            mCamera = null;//取消原来摄像头
            mCameraInfo.facing = Camera.CameraInfo.CAMERA_FACING_BACK;
            mCamera = Camera.open(mCameraInfo.facing);//打开当前选中的摄像头
            try {
                mCamera.setPreviewDisplay(holder);
                initCamera(mCameraSize.width, mCameraSize.height);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void getOptimalPreviewSize(int w, int h) {

        mParameters = mCamera.getParameters();
        List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            Log.i("Main", "width: " + size.width + "  height：" + size.height);
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                mCameraSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        minDiff = Double.MAX_VALUE;
        for (Camera.Size size : sizes) {
            if (Math.abs(size.height - targetHeight) < minDiff) {
                mCameraSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
    }

    private int getisplayOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;//该值有其它用途
    }

    public void initCamera(int width, int height) {
        mParameters = mCamera.getParameters();
        mParameters.setPreviewFormat(ImageFormat.NV21);
//        List<Integer> support =parameters.getSupportedPictureFormats();
//        int format = parameters.getPreviewFormat();

        //根据设置的宽高 和手机支持的分辨率对比计算出合适的宽高算法
        getOptimalPreviewSize(width, height);
//        LogUtil.d("mWidth"+ optionSize.width + "optionSize.height"+optionSize.height);
//        int frame = parameters.getPreviewFrameRate();
//       LogUtil.i(parameters.getPreviewFrameRate() + "哈哈哈")
        mParameters.setPreviewSize(mCameraSize.width, mCameraSize.height);
        mCamera.setParameters(mParameters);
        mCamera.setDisplayOrientation(90);
        //1.增加缓冲区buffer: 这里指定的是yuv420sp格式
//        mCamera.addCallbackBuffer(new byte[((mWidth * mHeight) *
//                ImageFormat.getBitsPerPixel(ImageFormat.NV21)) / 8]);
//        mCamera.addCallbackBuffer(new byte[width * height * 3 / 2]);
//        //2.设置回调:系统相机某些核心部分不走JVM,进行特殊优化，所以效率很高
//        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
//            @Override
//            public void onPreviewFrame(byte[] datas, Camera camera) {
//                //回收缓存处理
//                camera.addCallbackBuffer(datas);
//            }
//        });
        //3开启预览
        mCamera.startPreview();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_surfaceview);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 11);
            }
        }
        mSurfaceView = findViewById(R.id.surface_view);

        mSurfaceHolder = mSurfaceView.getHolder();

        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);

        mSurfaceHolder.addCallback(mSurfaceCallback);
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        mCamera.startPreview();
//    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera.stopPreview();
        mCamera.release();//释放资源
    }
}
