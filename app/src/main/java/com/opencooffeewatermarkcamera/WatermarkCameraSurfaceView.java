package com.opencooffeewatermarkcamera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;

import com.opencooffeewatermarkcamera.utils.Utils;

public class WatermarkCameraSurfaceView extends SurfaceView {

    //private static final String LOG_TAG = WatermarkCameraSurfaceView.class.getSimpleName();

    // Maximum allowed duration of a "click", in milliseconds.
    private static final int MAX_CLICK_DURATION = 1000;

    // Maximum movement distance allowed during a "click", in DP.
    private static final int MAX_CLICK_DISTANCE = 10;

    private GestureDetector gestureDetector;

    private long pressStartTime;
    private float pressedX, pressedY;

    public WatermarkCameraSurfaceView(Context context) {
        super(context);
    }

    public WatermarkCameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // New gesture detector.
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public WatermarkCameraSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        // We deliberately forget about child measurements because they act as a wrapper for the SurfaceView that focuses the camera's preview instead of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        switch (motionEvent.getAction()) {

            case MotionEvent.ACTION_DOWN:

                pressStartTime = System.currentTimeMillis();
                pressedX = motionEvent.getX();
                pressedY = motionEvent.getY();

                // REQUIRED A BREAK.
                break;

            case MotionEvent.ACTION_UP:

                long pressDuration = System.currentTimeMillis() - pressStartTime;

                WatermarkCamera context = (WatermarkCamera) getContext();

                if (pressDuration < MAX_CLICK_DURATION && Utils.onTouchDistance(context, pressedX, pressedY, motionEvent.getX(), motionEvent.getY()) < MAX_CLICK_DISTANCE) {
                    context.onTouch();
                }

                // REQUIRED A BREAK.
                break;

        }

        return gestureDetector.onTouchEvent(motionEvent);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        // On down event.
        @Override
        public boolean onDown(MotionEvent motionEvent) {

            ((WatermarkCamera) getContext()).cameraScreenTouch();

            return true;
        }

        // Double-pulse event.
        @Override
        public boolean onDoubleTap(MotionEvent motionEvent) {

            ((WatermarkCamera) getContext()).doubleTap();

            return true;
        }

    }

}