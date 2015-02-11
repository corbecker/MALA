package com.corbinbecker.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.corbinbecker.views.CameraPreview;
import com.corbinbecker.views.RectangleView;

/*
Created by Corbin Becker. Displays the camera preview and send the results to
the word showcase activity
 */
public class OCRActivity extends ActionBarActivity implements Camera.PictureCallback {

    private Camera camera;
    private CameraPreview cameraPreview;
    private RectangleView rectangleView;
    public static String DIR;
    private int screenHeight, screenWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set directory name for image files and tess data
        DIR = Environment.getExternalStorageDirectory().toString() + "/MALA/";

        //begin citation: http://stackoverflow.com/questions/25723331/display-and-hide-navigationbar-and-actionbar-onclickandroid
        if (Build.VERSION.SDK_INT < 16) {
            //Hide status bar
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (Build.VERSION.SDK_INT > 16) {
            //Hide status bar
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
        //hide action bar
        getSupportActionBar().hide();

        //end citation

        /*
        Inspired by tutorial: http://adblogcat.com/a-camera-preview-with-a-bounding-box-like-google-goggles/
         */
        rectangleView = new RectangleView(this);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenHeight = displaymetrics.heightPixels;
        screenWidth = displaymetrics.widthPixels;
        //end citation
    }

    /*
    Begin citation: http://adblogcat.com/a-camera-preview-with-a-bounding-box-like-google-goggles/
     */
    public Double[] getRatio() {
        Camera.Size s = cameraPreview.getCameraParameters().getPreviewSize();
        double heightRatio = (double) s.height / (double) screenHeight;
        double widthRatio = (double) s.width / (double) screenWidth;
        return new Double[]{heightRatio, widthRatio};
    }
    //end citation


    public void onCaptureButtonClick(View view) {
        camera.takePicture(null, null, this);
    }

    public void getCamera(){
        camera = getCameraInstance();
        cameraPreview = new CameraPreview(this, camera);
        FrameLayout camFrameLayoutView = (FrameLayout) findViewById(R.id.camera_preview);
        camFrameLayoutView.addView(cameraPreview);
        rectangleView = (RectangleView) findViewById(R.id.dragRect);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {


            Thread getPictureThread = new Thread(new Runnable() {
                public void run() {

                    //begin citation: http://adblogcat.com/a-camera-preview-with-a-bounding-box-like-google-goggles/
                    //accounts for the ratio of the camera preview and rectangle to save the correct part of the
                    //image (I.e. only the part inside the resizable rectangle
                    Double[] ratio = getRatio();
                    int left = (int) (ratio[1] * (double) rectangleView.getTopLeftPoint().x);

                    int top = (int) (ratio[0] * (double) rectangleView.getTopLeftPoint().y);

                    int right = (int) (ratio[1] * (double) rectangleView.getBottomRightPoint().x);

                    int bottom = (int) (ratio[0] * (double) rectangleView.getBottomRightPoint().y);

                    cameraPreview.getImage(left, top, right, bottom);
                    //end citation

                    //display word showcase with results from OCR
                    Intent display = new Intent(getApplicationContext(), WordShowcase.class);
                    display.putExtra("path", DIR + "ocrImage.jpg");
                    display.putExtra("parent", "OCR");
                    startActivity(display);
                }
            });
            getPictureThread.start();

    }

    //release camera if paused
    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setContentView(R.layout.rect_activity_layout);
        if(camera == null){
            getCamera();
        }
    }


    /**
     * A safe way to get an instance of the Camera object.
     * Sourced from google recommendations: http://developer.android.com/guide/topics/media/camera.html
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
    //end citation


    public void onBackButtonClick(View view) {
        //go back to main screen when arrow is clicked
        NavUtils.navigateUpFromSameTask(OCRActivity.this);
    }


}