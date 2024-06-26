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

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Camera1GLSurfaceviewActivity extends AppCompatActivity {
    private static final String TAG = "CameraGLSurfaceviewActivity";
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

    private Size mPreviewSize;

    private float[] mVertexCoord = {
            -1f, -1f,  //左下
            1f, -1f,   //右下
            -1f, 1f,   //左上
            1f, 1f,    //右上
    };

    //纹理坐标（s,t）
    /*纹理坐标需要经过变换
     * (1).顺时针旋转90°
     * (2).镜像
     */
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

    //接收相机数据的纹理
    private int[] mTextureId = new int[1];
    //接收相机数据的 SurfaceTexture
    public SurfaceTexture mSurfaceTexture;
    private Camera mCamera;
    GLSurfaceView.Renderer mGLRender = new GLSurfaceView.Renderer() {
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            //设置清除渲染时的颜色
            /*
             * 白色:(1.0f, 1.0f, 1.0f, 0.0f)
             * 黑色:(0.0f, 0.0f, 0.0f, 1.0f)
             */
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            //创建并连接程序
            mShaderProgram = createAndLinkProgram("texture_vertex_shader.glsl", "texture_fragtment_shader.glsl");
            if (mShaderProgram != 0) {
                glUseProgram(mShaderProgram);
            }

            //初始化着色器中各变量属性
            initAttribLocation();
            //初始化顶点数据
            initVertexAttrib();
            //初始化纹理
            initTexture();
            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
                mCamera.startPreview();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 gl) {
            //Log.v(TAG,"onDrawFrame()");
            //surfaceTexture 获取新的纹理数据
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(vMatrix);

            glClear(GL_COLOR_BUFFER_BIT);

            //允许顶点着色器中属性变量aPosVertex,接收来自缓冲区的顶点数据
            glEnableVertexAttribArray(mVertexLocation);
            //允许顶点着色器中属性变量 aTexVertex 接收来自缓冲区的纹理UV顶点数据
            glEnableVertexAttribArray(mTextureLocation);

            //矩阵赋值
            glUniformMatrix4fv(mVMatrixLocation, 1, false, vMatrix, 0);

            //开始绘制，绘制mVertexCoord.length/2即4个点
            //GL_TRIANGLE_STRIP 和 GL_TRIANGLE_FAN的绘制方式不同，需要注意
            glDrawArrays(GL_TRIANGLE_STRIP, 0, mVertexCoord.length / 2);

            //禁止顶点数组的句柄
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
        //设置GLES版本
        mGLSurfaceView.setEGLContextClientVersion(3);
        mGLSurfaceView.setRenderer(mGLRender);
        mCamera = Camera.open(0);
        mCamera.setDisplayOrientation(90);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        //应该放在SurfaceTexture销毁的地方，暂时先放在这里
        mCamera.stopPreview();
        mCamera.release();
    }
    private void setWindowFlag() {
        Window window = getWindow();
        //隐藏顶部 StatuBar状态栏
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //隐藏底部 NavigationBar导航栏
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void initVertexAttrib() {
        mVertexCoordBuffer = getFloatBuffer(mVertexCoord);
        //把顶点数据缓冲区 绑定到顶点着色器中 接收顶点数据的属性变量 aPosVertex
        glVertexAttribPointer(mVertexLocation, VERTEX_ATTRIB_POSITION_SIZE, GL_FLOAT, false, 0, mVertexCoordBuffer);

        mTextureCoordBuffer = getFloatBuffer(mTextureCoord);
        //把UV顶点数据缓冲区 绑定到顶点着色器中 接收顶点数据的属性变量 aTexVertex
        glVertexAttribPointer(mTextureLocation, VERTEX_ATTRIB_TEXTURE_POSITION_SIZE, GL_FLOAT, false, 0, mTextureCoordBuffer);
    }

    public void initAttribLocation() {
        mVertexLocation = glGetAttribLocation(mShaderProgram, VERTEX_ATTRIB_POSITION);
        mTextureLocation = glGetAttribLocation(mShaderProgram, VERTEX_ATTRIB_TEXTURE_POSITION);
        mUTextureLocation = glGetUniformLocation(mShaderProgram, UNIFORM_TEXTURE);
        mVMatrixLocation = glGetUniformLocation(mShaderProgram, UNIFORM_VMATRIX);
    }

    public void initTexture() {
        //创建纹理对象
        glGenTextures(mTextureId.length, mTextureId, 0);
        //使用纹理对象创建surfaceTexture，提供给外部使用
        mSurfaceTexture = new SurfaceTexture(mTextureId[0]);
        //激活纹理：默认0号纹理单元，一般最多能绑16个，视GPU而定
        glActiveTexture(GL_TEXTURE0);
        //绑定纹理：将纹理放到当前单元的 GL_TEXTURE_BINDING_EXTERNAL_OES 目标对象中
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId[0]);
        //配置纹理：过滤方式
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        //将片段着色器的采样器（纹理属性：s_texture）设置为0号单元
        glUniform1i(mUTextureLocation, 0);
    }
    /*
     * 创建和链接着色器程序
     * 参数：顶点着色器、片段着色器程序ResId
     * 返回：成功创建、链接了顶点和片段着色器的着色器程序Id
     */
    public int createAndLinkProgram(String vertexShaderFN, String fragShaderFN) {
        //创建着色器程序
        int shaderProgram = glCreateProgram();
        if (shaderProgram == 0) {
            Log.e(TAG, "Failed to create mShaderProgram ");
            return 0;
        }

        //获取顶点着色器对象
        int vertexShader = loadShader(GL_VERTEX_SHADER, loadShaderSource(vertexShaderFN));
//        int vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderFN);
        if (0 == vertexShader) {
            Log.e(TAG, "Failed to load vertexShader");
            return 0;
        }

        //获取片段着色器对象
        int fragmentShader = loadShader(GL_FRAGMENT_SHADER, loadShaderSource(fragShaderFN));
//        int fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragShaderFN);
        if (0 == fragmentShader) {
            Log.e(TAG, "Failed to load fragmentShader");
            return 0;
        }

        //绑定顶点着色器到着色器程序
        glAttachShader(shaderProgram, vertexShader);
        //绑定片段着色器到着色器程序
        glAttachShader(shaderProgram, fragmentShader);

        //链接着色器程序
        glLinkProgram(shaderProgram);
        //检查着色器链接状态
        int[] linked = new int[1];
        glGetProgramiv(shaderProgram, GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            glDeleteProgram(shaderProgram);
            Log.e(TAG, "Failed to link shaderProgram");
            return 0;
        }

        return shaderProgram;
    }

    /**
     * 加载着色器源，并编译
     *
     * @param type         顶点着色器（GL_VERTEX_SHADER）/片段着色器（GL_FRAGMENT_SHADER）
     * @param shaderSource 着色器源
     * @return 着色器
     */
    public int loadShader(int type, String shaderSource) {
        //创建着色器对象
        int shader = glCreateShader(type);
        if (shader == 0) {
            return 0;//创建失败
        }
        //加载着色器源
        glShaderSource(shader, shaderSource);
        //编译着色器对象
        glCompileShader(shader);
        //检查编译状态
        int[] compiled = new int[1];
        glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            //编译失败，执行：打印日志、删除链接到着色器程序的着色器对象、返回错误值
            Log.e(TAG, glGetShaderInfoLog(shader));
            glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    /*********************** 着色器、程序 ************************/
    public String loadShaderSource(String fname) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        return strBld.toString();
    }

    public FloatBuffer getFloatBuffer(float[] array) {
        //将顶点数据拷贝映射到 native 内存中，以便opengl能够访问
        FloatBuffer buffer = ByteBuffer
                .allocateDirect(array.length * 4)//直接分配 native 内存，不会被gc
                .order(ByteOrder.nativeOrder())//和本地平台保持一致的字节序（大/小头）
                .asFloatBuffer();//将底层字节映射到FloatBuffer实例，方便使用

        buffer.put(array)//将顶点拷贝到 native 内存中
                .position(0);//每次 put position 都会 + 1，需要在绘制前重置为0

        return buffer;
    }

}
