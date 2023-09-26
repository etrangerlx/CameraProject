package com.rainbow.camera2demo;

import android.view.Surface;

public class NativeCamera2 {

    public native boolean NativeCamera2Init();

    public native boolean NativeCamera2GetSurface(Surface surface);

    public native boolean NativeCamera2Uninit();

    public native boolean NativeCameraLens(int facing);

    static {
        System.loadLibrary("NativeCamera2");
    }
}
