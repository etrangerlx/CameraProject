package com.rainbow.camera;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glTexParameterf;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.opengl.GLES11Ext;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Camera2GLSurfaceviewActivity extends AppCompatActivity {
    private static final String TAG = "Camera2GLSurfaceview";
    private String VERTEX_ATTRIB_POSITION = "aPosVertex";
    private int VERTEX_ATTRIB_POSITION_SIZE = 2;

    private String VERTEX_ATTRIB_TEXTURE_POSITION = "aTexVertex";
    private int VERTEX_ATTRIB_TEXTURE_POSITION_SIZE = 2;

    private String UNIFORM_TEXTURE = "s_texture";
    private String UNIFORM_VMATRIX = "vMatrix";

    GLSurfaceView mGLSurfaceView;
    private int mVertexLocation;
    private int mTextureLocation;
    private int mUTextureLocation;
    private int mVMatrixLocation;

    private float[] mVertexCoord = {
            -1f, -1f,  //左下
            1f, -1f,   //右下
            -1f, 1f,   //左上
            1f, 1f,    //右上
    };

    public float[] mTextureCoord = {
            0.0f, 0.0f,  //左下
            1.0f, 0.0f,  //右下
            0.0f, 1.0f,  //左上
            1.0f, 1.0f,  //右上
    };

    public float[] vMatrix = new float[16];

    private FloatBuffer mVertexCoordBuffer;
    private FloatBuffer mTextureCoordBuffer;

    private int mShaderProgram;

    private int[] mTextureId = new int[1];
    public SurfaceTexture mSurfaceTexture;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private String mCameraId = "0";
    
    // 独立的 FrameAvailableListener
    private final SurfaceTexture.OnFrameAvailableListener mFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            if (mGLSurfaceView != null) {
                mGLSurfaceView.requestRender();
            }
        }
    };

    GLSurfaceView.Renderer mGLRender = new GLSurfaceView.Renderer() {
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            mShaderProgram = createAndLinkProgram("texture_vertex_shader.glsl", "texture_fragtment_shader.glsl");
            if (mShaderProgram != 0) {
                glUseProgram(mShaderProgram);
            }

            initAttribLocation();
            initVertexAttrib();
            initTexture();

            if (mSurfaceTexture != null) {
                mSurfaceTexture.setOnFrameAvailableListener(mFrameAvailableListener);
            }

            openCamera();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (mSurfaceTexture == null) {
                return;
            }
            
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(vMatrix);

            glClear(GL_COLOR_BUFFER_BIT);

            glEnableVertexAttribArray(mVertexLocation);
            glEnableVertexAttribArray(mTextureLocation);

            glUniformMatrix4fv(mVMatrixLocation, 1, false, vMatrix, 0);

            glDrawArrays(GL_TRIANGLE_STRIP, 0, mVertexCoord.length / 2);

            glDisableVertexAttribArray(mVertexLocation);
            glDisableVertexAttribArray(mTextureLocation);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glsurfaceview);
        setWindowFlag();

        mGLSurfaceView = findViewById(R.id.glsurfaceView);
        mGLSurfaceView.setEGLContextClientVersion(3);
        mGLSurfaceView.setRenderer(mGLRender);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        mGLSurfaceView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCamera();
    }

    private void setWindowFlag() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            Log.e(TAG, "CameraManager is null");
            return;
        }
        
        // 确保 Handler 不为空
        if (mBackgroundHandler == null) {
            Log.e(TAG, "Background handler is not initialized");
            return;
        }
        
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            
            final Handler handler = mBackgroundHandler;
            manager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.i(TAG, "CameraDevice onOpened");
                    mCameraDevice = camera;
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.i(TAG, "CameraDevice onDisconnected");
                    if (camera != null) {
                        camera.close();
                    }
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(TAG, "CameraDevice onError: " + error);
                    if (camera != null) {
                        camera.close();
                    }
                    mCameraDevice = null;
                }
            }, handler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to open camera", e);
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startPreview() {
        if (mCameraDevice == null || mSurfaceTexture == null) {
            Log.e(TAG, "Cannot start preview: camera or surface is null");
            return;
        }
        
        // 确保 Handler 不为空
        if (mBackgroundHandler == null) {
            Log.e(TAG, "Background handler is not initialized");
            return;
        }

        try {
            Surface surface = new Surface(mSurfaceTexture);
            final CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            final Handler handler = mBackgroundHandler;
            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (mCameraDevice == null) {
                        return;
                    }
                    mCaptureSession = session;
                    try {
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        mCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, handler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Failed to start repeating request", e);
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Configuration failed");
                }
            }, handler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to start preview", e);
            e.printStackTrace();
        }
    }

    private void initVertexAttrib() {
        mVertexCoordBuffer = getFloatBuffer(mVertexCoord);
        glVertexAttribPointer(mVertexLocation, VERTEX_ATTRIB_POSITION_SIZE, GL_FLOAT, false, 0, mVertexCoordBuffer);

        mTextureCoordBuffer = getFloatBuffer(mTextureCoord);
        glVertexAttribPointer(mTextureLocation, VERTEX_ATTRIB_TEXTURE_POSITION_SIZE, GL_FLOAT, false, 0, mTextureCoordBuffer);
    }

    public void initAttribLocation() {
        mVertexLocation = glGetAttribLocation(mShaderProgram, VERTEX_ATTRIB_POSITION);
        mTextureLocation = glGetAttribLocation(mShaderProgram, VERTEX_ATTRIB_TEXTURE_POSITION);
        mUTextureLocation = glGetUniformLocation(mShaderProgram, UNIFORM_TEXTURE);
        mVMatrixLocation = glGetUniformLocation(mShaderProgram, UNIFORM_VMATRIX);
    }

    public void initTexture() {
        glGenTextures(mTextureId.length, mTextureId, 0);
        mSurfaceTexture = new SurfaceTexture(mTextureId[0]);
        mSurfaceTexture.setDefaultBufferSize(1920, 1080);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId[0]);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glUniform1i(mUTextureLocation, 0);
    }

    public int createAndLinkProgram(String vertexShaderFN, String fragShaderFN) {
        int shaderProgram = glCreateProgram();
        if (shaderProgram == 0) {
            Log.e(TAG, "Failed to create mShaderProgram ");
            return 0;
        }

        int vertexShader = loadShader(GL_VERTEX_SHADER, loadShaderSource(vertexShaderFN));
        if (0 == vertexShader) {
            Log.e(TAG, "Failed to load vertexShader");
            return 0;
        }

        int fragmentShader = loadShader(GL_FRAGMENT_SHADER, loadShaderSource(fragShaderFN));
        if (0 == fragmentShader) {
            Log.e(TAG, "Failed to load fragmentShader");
            return 0;
        }

        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);

        glLinkProgram(shaderProgram);
        int[] linked = new int[1];
        glGetProgramiv(shaderProgram, GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            glDeleteProgram(shaderProgram);
            Log.e(TAG, "Failed to link shaderProgram");
            return 0;
        }

        return shaderProgram;
    }

    public int loadShader(int type, String shaderSource) {
        if (shaderSource == null || shaderSource.isEmpty()) {
            Log.e(TAG, "Shader source is null or empty");
            return 0;
        }

        int shader = glCreateShader(type);
        if (shader == 0) {
            Log.e(TAG, "Failed to create shader");
            return 0;
        }
        glShaderSource(shader, shaderSource);
        glCompileShader(shader);
        int[] compiled = new int[1];
        glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader compilation failed: " + glGetShaderInfoLog(shader));
            glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    public String loadShaderSource(String fname) {
        if (fname == null || fname.isEmpty()) {
            Log.e(TAG, "Shader filename is null or empty");
            return "";
        }

        StringBuilder strBld = new StringBuilder();
        String nextLine;

        try {
            InputStream is = this.getResources().getAssets().open(fname);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            while ((nextLine = br.readLine()) != null) {
                strBld.append(nextLine);
                strBld.append('\n');
            }
            br.close();
            isr.close();
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to load shader source: " + fname, e);
            e.printStackTrace();
            return "";
        }

        return strBld.toString();
    }

    public FloatBuffer getFloatBuffer(float[] array) {
        FloatBuffer buffer = ByteBuffer
                .allocateDirect(array.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        buffer.put(array)
                .position(0);

        return buffer;
    }
}
