package com.rainbow.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Camera1TextureViewActivity extends AppCompatActivity {
    private TextureView mTextureView;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera.Size mPreviewSize;

    private Camera mCamera;
    private Camera.Parameters mParameter;
    byte[] mPreBuffer = null;

    Bitmap captureBitmap = null;
    Canvas canvas = null;
    Paint paint1 = null;
    TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            try {
//                captureBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//                canvas = new Canvas(captureBitmap);
//                paint1 = new Paint();
//                paint1.setColor(Color.RED);
//                ColorMatrix colorMartrix = new ColorMatrix();
//                colorMartrix.setScale(1.2f, 1.2f, 1.2f, 1);
//                paint1.setColorFilter(new ColorMatrixColorFilter(colorMartrix));
//                final int textSize = 24;
//                paint1.setColor(0xff00ffff);
//                paint1.setTextSize(textSize);

                // 打开摄像头并将展示方向旋转90度
                mCamera = Camera.open(0);
                mCamera.setDisplayOrientation(90);
                mParameter = mCamera.getParameters();
                mParameter.setPreviewFormat(ImageFormat.NV21);
                Log.i("mPreviewSize", "width = " + width + " height= " + height );
//                mPreviewSize.width = width;
//                mPreviewSize.height = height;
                mPreBuffer = new byte[width * height * 3 / 2];//首先分配一块内存作为缓冲区，size的计算方式见第四点中
//                mCamera.setPreviewCallback(mPreviewCallback);
                mCamera.addCallbackBuffer(mPreBuffer);//将此缓冲区添加到预览回调缓冲区队列中
                mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);
                mCamera.setPreviewTexture(surface);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            mCamera.stopPreview();
            mCamera.release();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            // Invoked every time there's a new Camera preview frame
            long time0 = System.currentTimeMillis();

//            mTextureView.getBitmap(captureBitmap);
//
            long time1 = System.currentTimeMillis() - time0;
//            final Canvas c = mSurfaceHolder.lockCanvas();
//            if (c != null) {
////                canvas.drawText("getBmp= "  + time1, 500, 400, paint1);
//                c.drawBitmap(captureBitmap, 0, 0, paint1);
//                mSurfaceHolder.unlockCanvasAndPost(c);
//            }
            long total = System.currentTimeMillis() - time0;
            long time2 = total - time1;
            Log.i("onFrame", "timing: getBmp= " + time1 + " blit= " + time2 + " total= " + total);
        }
    };
    public static final String STORAGE_PATH = Environment.getExternalStorageDirectory().toString();
    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String path = STORAGE_PATH + "/DCIM" + "/CameraV1";
                    Log.d("dump path", path);
                    writeFile(path, data);
                }
            }, "captureThread").start();
            camera.startPreview();
        }
    };


    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            String path = STORAGE_PATH + "/DCIM" + "/CameraV1";

            long Start = System.currentTimeMillis();
            ////TODO
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = path + "/" + "IMG_" + timeStamp + ".yuv";
            int width=camera.getParameters().getPreviewSize().width;
            int height=camera.getParameters().getPreviewSize().height;
//            Canvas canvas=mSurfaceHolder.lockCanvas();
//            if (canvas != null) {
//                canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
//                Bitmap cacheBitmap=nv21ToBitmap(data, width, height);
//                canvas.drawBitmap(cacheBitmap, 0, 0, null);
//                mSurfaceHolder.unlockCanvasAndPost(canvas);
//            }
            long End = System.currentTimeMillis();
            System.out.println(Thread.currentThread().getName()+",相机数据间隔:" + (End - Start));
            mCamera.addCallbackBuffer(mPreBuffer);//将此缓冲区添加到预览回调缓冲区队列中
        }
    };
    //输出图像
    private static Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        Bitmap bitmap=null;
        try {
            YuvImage image=new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);
            //将rawImage转换成bitmap
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inPreferredConfig=Bitmap.Config.ARGB_8888;
            bitmap=BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size(), options);


            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
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
            bos.flush();
            bos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textureview);
        mTextureView = findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(mTextureListener);

//        mSurfaceView = (SurfaceView) findViewById(R.id.textureView);
//        mSurfaceHolder = mSurfaceView.getHolder();

        ImageButton mCaptureButton = findViewById(R.id.btnTakePicTV);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null) {
                    mCamera.takePicture(null, null, mPictureCallback);
                }
            }
        });


    }

}
