package com.tud.alexw.capturedataset;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.frame.Frame;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * @author jacob
 * @date 5/7/18
 */

public class ImageCapturer {

    private static final String TAG = "ImageCapturer";
    private Vision mVision;
    private StreamInfo mColorInfo;
    private AnnotatedImage mAnnotatedImage;

    public ImageCapturer(Vision mVision) {
        this.mVision = mVision;
        this.mAnnotatedImage = new AnnotatedImage();
    }

    public synchronized AnnotatedImage captureImage(String roomLabel, int posX, int posY, int headDirection) {
        Log.d(TAG, "captureImage() called");

        //start image stream listener
        StreamInfo[] streamInfos = mVision.getActivatedStreamInfo();
        for (StreamInfo info : streamInfos) {
            if (info.getStreamType() == StreamType.COLOR) {
                    mColorInfo = info;
                    mVision.startListenFrame(StreamType.COLOR, mFrameListener);
                    mAnnotatedImage.setRoomLabel(roomLabel);
                    mAnnotatedImage.setPosX(posX);
                    mAnnotatedImage.setPosY(posY);
                    mAnnotatedImage.setHeadDirection(headDirection);
                    Log.i(TAG, "Image " + mAnnotatedImage + " captured: " + mAnnotatedImage.getFilePath());
                    return mAnnotatedImage;
            }
        }
        Log.wtf(TAG, "No camera active!"); // What a terrible failure
        return null;
    }

    /**
     * FrameListener instance for get raw image data form vision service
     */
    Vision.FrameListener mFrameListener = new Vision.FrameListener() {

        @Override
        public void onNewFrame(int streamType, Frame frame) {
            Bitmap mColorBitmap = Bitmap.createBitmap(mColorInfo.getWidth(), mColorInfo.getHeight(), Bitmap.Config.ARGB_8888);
            if (streamType == StreamType.COLOR) {
                    // draw color image to bitmap and display
                    mColorBitmap.copyPixelsFromBuffer(frame.getByteBuffer());
                    mAnnotatedImage.setBitmap(mColorBitmap);
                    if(mAnnotatedImage.isBitmapSet()){
                        mVision.stopListenFrame(StreamType.COLOR);
                    }
            }
        }
    };
}
