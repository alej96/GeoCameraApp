package com.example.geocamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.Arrays;

public class CameraActivity extends AppCompatActivity {

    TextureView textureView;

    CameraDevice cameraDevice;
    String cameraID;
    Size imageDimensions;

    CaptureRequest.Builder captureRequestBuilder;
    CameraCaptureSession cameraSession;
    Handler backgroundHandler;
    HandlerThread handlerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        textureView = (TextureView) findViewById(R.id.textureCamera);

        textureView.setSurfaceTextureListener(surfaceTextureListener);

    }

    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {

            try {
                openCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private void openCamera() throws CameraAccessException {

        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);


        cameraID = cameraManager.getCameraIdList()[0];

        CameraCharacteristics cc = cameraManager.getCameraCharacteristics(cameraID);
        StreamConfigurationMap cMap = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        imageDimensions = cMap.getOutputSizes(SurfaceTexture.class)[0];


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            //Security Exception, to check missing permissions
            return;
        }
        cameraManager.openCamera(cameraID, stateCallback, null);

    }
            CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    try {
                        startCameraPreview();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    cameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    cameraDevice.close();
                    cameraDevice = null;
                }
            };

    private void startCameraPreview() throws CameraAccessException {

        SurfaceTexture texture = textureView.getSurfaceTexture();
        texture.setDefaultBufferSize(imageDimensions.getWidth(), imageDimensions.getHeight());
        Surface surface = new Surface(texture);
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

        captureRequestBuilder.addTarget(surface);

        cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                if(cameraDevice == null){
                    return;

                }
                cameraSession = cameraCaptureSession;
                try {
                    updatePreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

            }
        }, null);
    }

    private void updatePreview() throws CameraAccessException {

        if(cameraDevice == null){
            return;
        }

        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        cameraSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);

    }

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();

        if(textureView.isAvailable()){
            try {
                openCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }else{
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    private void startBackgroundThread() {

        handlerThread = new HandlerThread("Camera Background");
        handlerThread.start();
        //Strat thread for camera
        backgroundHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    protected void onPause() {

        try {
            stopBackgroundThread();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    private void stopBackgroundThread() throws InterruptedException {

        handlerThread.quitSafely();
        handlerThread.join();

        backgroundHandler = null;
        handlerThread = null;

    }
}




