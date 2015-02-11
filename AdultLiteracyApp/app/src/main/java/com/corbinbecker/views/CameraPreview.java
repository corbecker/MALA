package com.corbinbecker.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.corbinbecker.activities.DolchListActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by corbinbecker
 * This View class is required to create a custom instance of a camera preview
 * using the hardware.CAMERA. Extends SurfaceView for displaying the live camera preview
 *
 * Code inspired from official android docs: http://developer.android.com/guide/topics/media/camera.html
 * and here: http://adblogcat.com/a-camera-preview-with-a-bounding-box-like-google-goggles/
 *
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    //SurfaceHolder for controlling/monitoring surface parameters
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private Camera.Parameters params;
    private byte[] mBuffer;
    private String DIR = Environment.getExternalStorageDirectory().toString() + "/MALA/";

    public CameraPreview(Context context, Camera camera) {
        super(context);
        initCamera(camera);
    }

    public void initCamera(Camera camera){
        this.camera = camera;
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //set camera to continually auto-focus
        Camera.Parameters params = camera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(params);
    }

    public void startPreview(SurfaceHolder surfaceHolder){
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            Log.d(DolchListActivity.TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        startPreview(holder);
        updateBufferSize();
        camera.addCallbackBuffer(mBuffer); // where we'll store the image data
        camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            public synchronized void onPreviewFrame(byte[] data, Camera c) {

                if (camera != null) { // there was a race condition when onStop() was called..
                    camera.addCallbackBuffer(mBuffer); // it was consumed by the call, add it back
                }
            }
        });
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

    }

    private void updateBufferSize() {
        mBuffer = null;
        System.gc();
        // prepare a buffer for copying preview data to
        int h = camera.getParameters().getPreviewSize().height;
        int w = camera.getParameters().getPreviewSize().width;
        int bitsPerPixel = ImageFormat.getBitsPerPixel(camera.getParameters().getPreviewFormat());
        mBuffer = new byte[w * h * bitsPerPixel / 8];
    }


    public void getImage(int x, int y, int width, int height) {

        System.gc();
        Camera.Size s = getCameraParameters().getPreviewSize();

        YuvImage yuvimage = new YuvImage(mBuffer, ImageFormat.NV21, s.width, s.height, null);

        File file = new File(DIR + "ocrImage.jpg");

        FileOutputStream filecon;
        try {
            filecon = new FileOutputStream(file);
            yuvimage.compressToJpeg(new Rect(x, y, width, height), 100, filecon); // make JPG
            filecon.flush();
            filecon.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Camera.Parameters getCameraParameters() {
        return camera.getParameters();
    }


}
