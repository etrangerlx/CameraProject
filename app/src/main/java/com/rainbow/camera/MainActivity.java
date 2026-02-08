package com.rainbow.camera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    public String[] PermissionList = {
            Manifest.permission.CAMERA,
            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int j = 0; j < PermissionList.length; j++) {
            if (ActivityCompat.checkSelfPermission(this, PermissionList[j]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,PermissionList, 1);
                break;
            }
        }

        // Camera1 API buttons
        Button camera1SurfViewBtn = findViewById(R.id.Camera1SurfView);
        Button camera1TextViewBtn = findViewById(R.id.Camera1TView);
        Button camera1GLSurfViewBtn = findViewById(R.id.Camera1GLSurfView);

        // Camera2 API buttons
        Button camera2SurfViewBtn = findViewById(R.id.Camera2SurfView);
        Button camera2TextViewBtn = findViewById(R.id.Camera2TView);
        Button camera2GLSurfViewBtn = findViewById(R.id.Camera2GLSurfView);

        // Camera1 listeners
        camera1SurfViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Camera1SurfaceActivity.class);
                startActivity(intent);
            }
        });

        camera1TextViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Camera1TextureViewActivity.class);
                startActivity(intent);
            }
        });

        camera1GLSurfViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Camera1GLSurfaceviewActivity.class);
                startActivity(intent);
            }
        });

        // Camera2 listeners
        camera2SurfViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Camera2SurfaceActivity.class);
                startActivity(intent);
            }
        });

        camera2TextViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Camera2TextureViewActivity.class);
                startActivity(intent);
            }
        });

        camera2GLSurfViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Camera2GLSurfaceviewActivity.class);
                startActivity(intent);
            }
        });
    }
}