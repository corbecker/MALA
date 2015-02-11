package com.corbinbecker.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import com.corbinbecker.activities.DolchListActivity;
import com.corbinbecker.models.RectangleCorner;

import java.util.ArrayList;

/**
 * Created by corbinbecker
 * Code modified from: http://adblogcat.com/a-camera-preview-with-a-bounding-box-like-google-goggles/
 */
public class RectangleView extends View {

    //field declarations
    private Paint paint;
    private Canvas canvas;

    private Paint topRectangleLine;
    private Paint bottomRectangleLine;
    private Paint leftRectangleLine;
    private Paint rightRectangleLine;

    //corners of the resizable rectangle (From model)
    private RectangleCorner topLeftCorner, topRightCorner, bottomLeftCorner, bottomRightCorner;

    int groupId = -1;
    private int cornerID = 0;

    //an array list for holding the rectangle corners
    private ArrayList<RectangleCorner> rectangleCorners = new ArrayList<>();

    //constructors
    public RectangleView(Context context) {
        super(context);
        onInit(context);
    }

    public RectangleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        onInit(context);
    }

    public RectangleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        onInit(context);
    }

    public void onInit(Context context) {

        //get size and width of screen
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        //new canvas and paint objects and set the rectangel to be focusable do it
        //can be moved
        canvas = new Canvas();
        paint = new Paint();
        setFocusable(true);
        RectangleCorner.count = 0;

        //set starting points of the rectangles corners
        topLeftCorner = new RectangleCorner(context, new Point(100, 120));
        bottomLeftCorner = new RectangleCorner(context, new Point(100, 200));
        bottomRightCorner = new RectangleCorner(context, new Point(500, 200));
        topRightCorner = new RectangleCorner(context, new Point(500, 120));

        //clear the array list and then add all the corners
        rectangleCorners.clear();
        rectangleCorners.add(0, topLeftCorner);
        rectangleCorners.add(1, bottomLeftCorner);
        rectangleCorners.add(2, bottomRightCorner);
        rectangleCorners.add(3, topRightCorner);

        //paint the lines connecting each of the corners
        topRectangleLine = new Paint();
        bottomRectangleLine = new Paint();
        leftRectangleLine = new Paint();
        rightRectangleLine = new Paint();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //center value to offset lines to start at the center of the corner icons
        int center = topLeftCorner.getCenter();

        //top line on rectangle
        canvas.drawLine(topLeftCorner.getX() + center, topLeftCorner.getY() + center,
                topRightCorner.getX() + center, topRightCorner.getY() + center, topRectangleLine);
        //right line on rectangle
        canvas.drawLine(topRightCorner.getX() + center, topRightCorner.getY() + center,
                bottomRightCorner.getX() + center, bottomRightCorner.getY() + center, rightRectangleLine);
        //bottom line on rectangle
        canvas.drawLine(bottomLeftCorner.getX() + center, bottomLeftCorner.getY() + center,
                bottomRightCorner.getX() + center, bottomRightCorner.getY() + center, bottomRectangleLine);
        //left line on rectangle
        canvas.drawLine(topLeftCorner.getX() + center, topLeftCorner.getY() + center,
                bottomLeftCorner.getX() + center, bottomLeftCorner.getY() + center, leftRectangleLine);

        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.parseColor("#55000000"));
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(5);

        //draw the canvas translucent white
        canvas.drawPaint(paint);
        paint.setColor(Color.parseColor("#55ffffff"));

        //draw the ractangle
        canvas.drawRect(
                bottomLeftCorner.getX() + topRightCorner.getCenter(),
                topRightCorner.getY() + bottomRightCorner.getCenter(),
                topRightCorner.getX() + bottomRightCorner.getCenter(),
                bottomLeftCorner.getY() + topRightCorner.getCenter(), paint);

        //for each corner in the array list draw them
        for (RectangleCorner corner : rectangleCorners) {
            canvas.drawBitmap(corner.getBitmap(), corner.getX(), corner.getY(), new Paint());
        }

    }

    //touch event listener for each of the rectangle corners
    public boolean onTouchEvent(MotionEvent touchEvent) {

        //get the type of touch event
        int action = touchEvent.getAction();

        //get the exact location of the touch event
        int X = (int) touchEvent.getX();
        int Y = (int) touchEvent.getY();


        switch (action) {

            case MotionEvent.ACTION_DOWN:
                cornerID = -1;
                groupId = -1;


                for (RectangleCorner corner : rectangleCorners) {
                    //get center point of the corner
                    int centerX = corner.getX() + corner.getWidthOfCorner();
                    int centerY = corner.getY() + corner.getHeightOfCorner();

                    //calculate midpoint of the rectangle corner
                    double dist = Math.sqrt(Math.pow(centerX - X, 2)
                            + Math.pow(centerY - Y, 2));

                    //if touch point was less than the width of the corner then it must be touching it
                    if (dist < corner.getWidthOfCorner()) {

                        cornerID = corner.getID();

                        //setting corner groups for pulling corresponding corners when being moved
                        if (cornerID == 1 || cornerID == 3) {
                            groupId = 2;

                        } else {
                            groupId = 1;

                        }
                        invalidate();
                    }
                    invalidate();

                }
                break;

            case MotionEvent.ACTION_MOVE:

                if (cornerID > -1) {
                    //set new location of the corners
                    rectangleCorners.get(cornerID).setX(X);
                    rectangleCorners.get(cornerID).setY(Y);

                    //move the top and bottom and left and right corners with each other
                    //makes adjusting the reactangle simpler
                    if (groupId == 1) {
                        rectangleCorners.get(1).setX(rectangleCorners.get(0).getX());
                        rectangleCorners.get(1).setY(rectangleCorners.get(2).getY());
                        rectangleCorners.get(3).setX(rectangleCorners.get(2).getX());
                        rectangleCorners.get(3).setY(rectangleCorners.get(0).getY());

                        //draw the new rectangle
                        canvas.drawRect(
                                topLeftCorner.getX(),
                                topLeftCorner.getY(),
                                bottomRightCorner.getX(),
                                bottomRightCorner.getY(),
                                paint);
                    } else {
                        rectangleCorners.get(0).setX(rectangleCorners.get(1).getX());
                        rectangleCorners.get(0).setY(rectangleCorners.get(3).getY());
                        rectangleCorners.get(2).setX(rectangleCorners.get(3).getX());
                        rectangleCorners.get(2).setY(rectangleCorners.get(1).getY());
                        //draw the new rectangle
                        canvas.drawRect(
                                bottomLeftCorner.getX(),
                                bottomLeftCorner.getY(),
                                topRightCorner.getX(),
                                topRightCorner.getY(),
                                paint);
                    }

                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:

                break;
        }
        invalidate();

        return true;
    }

    /*
    Methods for returning the top left and bottom right points
    so the image on the camera preview screen can be cropped
     */
    public Point getTopLeftPoint() {
        return new Point(topLeftCorner.getX(),topLeftCorner.getY());
    }


    public Point getBottomRightPoint() {
        return new Point(bottomRightCorner.getX(), bottomRightCorner.getY());
    }

}
