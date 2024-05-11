package com.rainbow.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;

import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class Camera1SurfaceActivity extends AppCompatActivity {
    private static String Tag = "Camera1Activity";
    private Camera mCamera = null;
    private Camera.Parameters mParameters = null;
    private Camera.CameraInfo mCameraInfo = null;
    private Camera.Size mCameraSize = null;
    byte[] mPreBuffer = null;
    private Thread previewwrite = null;
    private SurfaceView mSurfaceView = null;
    private ProcessWithHandlerThread processFrameHandlerThread;
    private Handler processFrameHandler;
    private SurfaceHolder mSurfaceHolder = null;
    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {

        /**
         *  在 Surface 首次创建时被立即调用：活得叫焦点时。一般在这里开启画图的线程
         * @param surfaceHolder 持有当前 Surface 的 SurfaceHolder 对象
         */
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            Log.i(Tag, "surfaceCreated");
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
            Log.i(Tag, "surfaceChanged");
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
            Log.i(Tag, "surfaceDestroyed");
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

    private void saveBmp2SD(String path, Bitmap bitmap) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = path + "/" + "IMG_" + timeStamp + ".jpg";
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            Toast.makeText(getApplicationContext(),
                    "Photo saved to " + fileName, Toast.LENGTH_SHORT).show();
            bos.flush();
            bos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeFile(String path, byte[] data) {
        Bitmap bitmap = null;
        if (data != null) {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        }

        if (bitmap != null) {
            Matrix matrix = new Matrix();
//            if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            matrix.postRotate(90);
//            }else if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT){
//                matrix.postRotate(90);
//                matrix.postScale(1, -1);
//            }
            Bitmap rotateBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, false);
            saveBmp2SD(path, rotateBmp);
            rotateBmp.recycle();
        }
    }

    public static final String STORAGE_PATH = Environment.getExternalStorageDirectory().toString();
    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String path = STORAGE_PATH + "/DCIM" + "/CameraV1";
                    Log.d("dump path", path);
                    writeFile(path, data);
                }
            });
            camera.startPreview();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(Tag, "onCreate");
        setContentView(R.layout.activity_surfaceview);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    this.startActivityForResult(intent, 1024);
                }
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 11);
            }
        }
        if (mCameraInfo == null) {
            mCameraInfo = new Camera.CameraInfo();
        }
        mCameraInfo.facing = Camera.CameraInfo.CAMERA_FACING_FRONT;

        mCamera = Camera.open(mCameraInfo.facing);


        mSurfaceView = findViewById(R.id.surface_view);

        mSurfaceHolder = mSurfaceView.getHolder();

        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);

        mSurfaceHolder.addCallback(mSurfaceCallback);


        processFrameHandlerThread = new ProcessWithHandlerThread("process frame");
        processFrameHandler = new Handler(processFrameHandlerThread.getLooper(), processFrameHandlerThread);


        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                Log.i(Tag, "Thread id:" + Thread.currentThread().getId() + ":sendData");
                processFrameHandler.obtainMessage(ProcessWithHandlerThread.WHAT_PROCESS_FRAME, data).sendToTarget();
            }
        });
        ImageView exchangeview = findViewById(R.id.ivExchange);
        exchangeview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCamera(mSurfaceHolder);
            }
        });


        ImageButton mCaptureButton = findViewById(R.id.btnTakePicSV);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null) {
                    mCamera.takePicture(null, null, mPictureCallback);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(Tag, "onResume");
        mCamera.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(Tag, "onPause");
        mCamera.stopPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(Tag, "onDestroy");
        mCamera.release();//释放资源
        previewwrite.stop();
    }

    public class ProcessWithHandlerThread extends HandlerThread implements Handler.Callback {
        private static final String TAG = "HandlerThread";
        public static final int WHAT_PROCESS_FRAME = 1;

        public ProcessWithHandlerThread(String name) {
            super(name);
            start();
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case WHAT_PROCESS_FRAME:
                    byte[] frameData = (byte[]) msg.obj;
                    processFrame(frameData);
                    return true;
                default:
                    return false;
            }
        }

        private void processFrame(byte[] frameData) {
            Log.d(Tag, "Thread id:" + getThreadId() + ":Download data length:" + frameData.length);
            saveyuvdata(frameData);
        }
    }


    private void saveyuvdata(byte[] frameData) {
        Log.d(Tag, "Download data Time:" + System.currentTimeMillis());
        Camera.Parameters parameters = mCamera.getParameters();
        int width = mCamera.getParameters().getPreviewSize().width;
        int height = mCamera.getParameters().getPreviewSize().height;
        Camera.Size size = parameters.getPreviewSize();
        String path = STORAGE_PATH + "/DCIM" + "/CameraV1";
//                    String path =   "/data/local/tmp/DCIM" + "/CameraV1";

        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        String fileName = path + "/" + "IMG_" + width + "x" + height + System.currentTimeMillis() + ".avi";
        try {
            FileOutputStream output = new FileOutputStream(new File(fileName));
            output.write(frameData);
            output.flush();
            output.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class ProcessWithQueue extends Thread {
        private static final String TAG = "Queue";
        private LinkedBlockingQueue<byte[]> mQueue;

        public ProcessWithQueue(LinkedBlockingQueue<byte[]> frameQueue) {
            mQueue = frameQueue;
            start();
        }

        @Override
        public void run() {
            while (true) {
                byte[] frameData = null;
                try {
                    frameData = mQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                processFrame(frameData);
            }
        }

        private void processFrame(byte[] frameData) {
            Log.i(TAG, "test");
        }
    }
}
