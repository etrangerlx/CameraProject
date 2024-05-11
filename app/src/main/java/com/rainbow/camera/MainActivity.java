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
        Button SurfViewBtn = findViewById(R.id.SurfView);
        Button TextViewBtn = findViewById(R.id.TView);
        Button GLSurfViewBtn = findViewById(R.id.GLSurfView);
        SurfViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Camera1SurfaceActivity.class);
                startActivity(intent);
            }
        });
        TextViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Camera1TextureViewActivity.class);
                startActivity(intent);
            }
        });
        GLSurfViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Camera1GLSurfaceviewActivity.class);
                startActivity(intent);
            }
        });
    }
}