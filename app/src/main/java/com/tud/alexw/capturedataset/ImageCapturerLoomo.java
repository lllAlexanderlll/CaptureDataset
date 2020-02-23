package com.tud.alexw.capturedataset;

import android.graphics.Bitmap;
import android.util.Log;

import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.frame.Frame;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

public class ImageCapturerLoomo {

    private static final String TAG = "ImageCapturerLoomo";
    private Vision mVision;
    private StreamInfo mColorInfo;
    private AnnotatedImage mAnnotatedImage;
    private Bitmap mBitmap;

    public ImageCapturerLoomo(Vision mVision) {
        this.mVision = mVision;
        this.mAnnotatedImage = new AnnotatedImage();
        this.mBitmap = null;
    }

    public synchronized AnnotatedImage captureImage(String roomLabel, int posX, int posY, int yaw, int pitch) {
//        Log.d(TAG, "captureImage() called");

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
    public synchronized void stop() {
        Log.d(TAG, "stop() called");
        mVision.stopListenFrame(StreamType.COLOR);
        mVision.stopListenFrame(StreamType.DEPTH);
    }

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
