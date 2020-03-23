package com.tud.alexw.capturedataset;

import android.graphics.Bitmap;
import android.util.Log;

import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.frame.Frame;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

/**
 * Deprecated class to capture colour images with Loomos RGBD camera
 */
@Deprecated
public class ImageCapturerLoomo {

    private static final String TAG = "ImageCapturerLoomo";
    private Vision mVision;
    private StreamInfo mColorInfo;
    private AnnotatedImage mAnnotatedImage;
    private Bitmap mBitmap;

    /**
     * activates loomos camera with Vision object of the robot SDK
     * @param mVision Vision object of the robot SDK
     */
    public ImageCapturerLoomo(Vision mVision) {
        this.mVision = mVision;
        this.mAnnotatedImage = new AnnotatedImage();
        this.mBitmap = null;
    }

    /**
     * Captures a single image and annotates it with given information
     * @param roomLabel place label
     * @param posX local capturing X coordinate
     * @param posY local capturing Y coordinate
     * @param yaw robots head yaw value in degree
     * @param pitch robots head pitch value in degree
     * @return annotated image i.e. image information and image itself
     */
    public synchronized AnnotatedImage captureImage(String roomLabel, int posX, int posY, int yaw, int pitch) {
        //start image stream listener
        StreamInfo[] streamInfos = mVision.getActivatedStreamInfo();
        for (StreamInfo info : streamInfos) {
            if (info.getStreamType() == StreamType.COLOR) {
                mColorInfo = info;
                mAnnotatedImage.setRoomLabel(roomLabel);
                mAnnotatedImage.setPosX(posX);
                mAnnotatedImage.setPosY(posY);
                mAnnotatedImage.setYaw(yaw);
                mAnnotatedImage.setPitch(pitch);
                if(mBitmap != null){
                    mAnnotatedImage.setBitmap(mBitmap);
                }
                Log.i(TAG, "Image [" + mAnnotatedImage + "] captured; path: " + mAnnotatedImage.getFilePath());
                return mAnnotatedImage;
            }
        }
        Log.wtf(TAG, "No camera active!"); // What a terrible failure
        return null;
    }

    /**
     * Starts capturing i.e. listing to incoming frames of colour image stream. To be called before captureImage
     */
    public synchronized void start() {
        Log.d(TAG, "start() called");
        StreamInfo[] streamInfos = mVision.getActivatedStreamInfo();
        for (StreamInfo info : streamInfos) {
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    mColorInfo = info;
                    mVision.startListenFrame(StreamType.COLOR, mFrameListener);
                    Log.d(TAG, "mVision.startListenFrame(StreamType.COLOR, mFrameListener) called");
                    break;
                case StreamType.DEPTH:
                    mVision.startListenFrame(StreamType.DEPTH, mFrameListener);
                    break;
            }
        }
    }

    /**
     * Stops capturing
     */
    public synchronized void stop() {
        Log.d(TAG, "stop() called");
        mVision.stopListenFrame(StreamType.COLOR);
        mVision.stopListenFrame(StreamType.DEPTH);
    }

    /**
     * Reports if an image was captured as a bitmap object
     * @return whether an image was captured as bitmap object
     */
    public boolean gotBitmap(){
        return mBitmap != null;
    }

    /**
     * FrameListener instance for get raw image data form vision service
     */
    Vision.FrameListener mFrameListener = new Vision.FrameListener() {

        @Override
        public void onNewFrame(int streamType, Frame frame) {
//            Bitmap mColorBitmap = Bitmap.createBitmap(mColorInfo.getWidth(), mColorInfo.getHeight(), Bitmap.Config.ARGB_8888);
            if (streamType == StreamType.COLOR) {
                // draw color image to bitmap and display
//                mColorBitmap.copyPixelsFromBuffer(frame.getByteBuffer());
//                mBitmap = mColorBitmap;
                Log.v(TAG, "Got frame as bitmap");
            }
        }
    };
}
