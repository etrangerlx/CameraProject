package com.rainbow.camera2demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Bundle;
//import android.os.PersistableBundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private NativeCamera2 democam2 = new NativeCamera2();
    private SurfaceView cameraView;

    private int Front_BACK = 1;
    public static final int REQUEST_CAMERA = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cameraView = (SurfaceView) findViewById(R.id.cameraview);
        cameraView.getHolder().setFormat(PixelFormat.RGB_888);
        cameraView.getHolder().addCallback(this);

        Button buttonSwitchCamera = (Button) findViewById(R.id.buttonSwitchCamera);
        buttonSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Front_BACK = 1 - Front_BACK;
            }
        });
        Button buttonChangeCamera = (Button) findViewById(R.id.buttonChangeCamera);
        buttonChangeCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                democam2.NativeCamera2Uninit();
                democam2.NativeCameraLens(Front_BACK);
                democam2.NativeCamera2Init();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
        democam2.NativeCamera2Init();
    }

    @Override
    public void onPause() {
        super.onPause();
        democam2.NativeCamera2Uninit();
    }
    @Override
    public void onStop() {
        super.onStop();
        democam2.NativeCamera2Uninit();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i("Surface:", "surfaceCreated");
        democam2.NativeCamera2GetSurface(holder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i("Surface:", String.valueOf(format) + ":" + String.valueOf(width) + "x" + String.valueOf(height));
        democam2.NativeCamera2GetSurface(holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("Surface:", "surfaceDestroyed");
        democam2.NativeCamera2Uninit();
    }
}
