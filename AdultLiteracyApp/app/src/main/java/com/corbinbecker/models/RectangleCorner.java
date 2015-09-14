package com.corbinbecker.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

import com.corbinbecker.activities.R;

/**
 * Created by corbinbecker
 *  Sourced from: http://adblogcat.com/a-camera-preview-with-a-bounding-box-like-google-goggles/
 *  Class for the corners of the rectangle view over the OCR camera preview
 */
public class RectangleCorner {

    private Bitmap bitmap;
    private Point point;
    private int id;
    public static int count = 0;
    private int center;

    public RectangleCorner(Context context, Point point){
        this.id = count++;
        this.point = point;
        this.bitmap = BitmapFactory.decodeResource(context.getResources(),
        R.drawable.corners);
        Drawable drawable = context.getResources().getDrawable(R.drawable.corners);
        this.center = drawable.getMinimumWidth()/2;
    }

    public int getCenter(){
        return center;
    }


    public int getWidthOfCorner() {
        return bitmap.getWidth();
    }

    public int getHeightOfCorner() {
        return bitmap.getHeight();
    }

    public Bitmap getBitmap() {

        return bitmap;
    }

    public int getX() {
        return point.x;
    }

    public int getY() {
        return point.y;
    }

    public int getID() {
        return id;
    }

    public void setX(int x) {
        point.x = x;
    }

    public void setY(int y) {
        point.y = y;
    }
}
