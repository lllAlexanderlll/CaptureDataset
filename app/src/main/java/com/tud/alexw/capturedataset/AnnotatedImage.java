package com.tud.alexw.capturedataset;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;


public class AnnotatedImage {

    private static final String TAG = "AnnotatedImage";

    String filename;
    long timeTaken;
    Bitmap bitmap;
    int posX, posY, headDirection;
    String roomLabel;
    String parentPath;

    public AnnotatedImage(){
        this.bitmap = null;
        this.parentPath = null;
        this.roomLabel = null;
        this.timeTaken = -1;
        this.posX = -1;
        this.posY = -1;
        this.headDirection = -1;
    }

    public void load(String path){
        int lastSlashIndex = path.lastIndexOf('/');
        this.parentPath = path.substring(0, lastSlashIndex - 1);
        this.filename = path.substring(lastSlashIndex, path.length() - 3);

        String[] segs = filename.split( Pattern.quote( "_" ) );
        if (segs.length == 5){
            this.timeTaken = Integer.parseInt(segs[0]);
            this.roomLabel = segs[1];
            this.posX = Integer.parseInt(segs[2]);
            this.posY = Integer.parseInt(segs[3]);
            this.headDirection = Integer.parseInt(segs[4]);
        }
        else{
            String errorMsg = "Cannot construct AnnotatedImage. Filename couldn't be parsed: " + filename;
            Log.e(TAG, errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        File f = new  File(path);
        if(f.exists()) {
            this.bitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
        }
        else{
            String errorMsg = "Cannot construct AnnotatedImage. Image does not exist: " + path;
            Log.e(TAG, errorMsg);
            throw new IllegalStateException(errorMsg);
        }

    }

    public void save(final Context context){

        if(timeTaken == -1 || posX == -1 || posY == -1 || headDirection == -1 || roomLabel == null || bitmap == null){
            String errorMsg = "Cannot save AnnotatedImage. Not fully initialised: " + this;
            Log.e(TAG, errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        else{
            this.filename = String.format("%d_%s_%d_%d_%d.png", timeTaken, roomLabel,posX, posY, headDirection);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AnnotatedImage.this.parentPath = context.getExternalFilesDir(null).getAbsolutePath();
                    File f = new File(getFilePath());
                    Log.d(TAG, "saveImage(): " + f.getAbsolutePath());
                    try {
                        FileOutputStream fOut = new FileOutputStream(f);
                        AnnotatedImage.this.bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                        fOut.flush();
                        fOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public void setBitmap(Bitmap bitmap) {
            this.timeTaken = System.currentTimeMillis();
            this.bitmap = bitmap;
    }

    public boolean isBitmapSet(){
        return null != bitmap;
    }

    public String getFilename() {
        return filename;
    }

    public String getFilePath(){
        return parentPath + "/" + filename;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public long getTimeTaken() {
        return timeTaken;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public int getHeadDirection() {
        return headDirection;
    }

    public String getRoomLabel() {
        return roomLabel;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public void setHeadDirection(int headDirection) {
        this.headDirection = headDirection;
    }

    public void setRoomLabel(String roomLabel) {
        this.roomLabel = roomLabel;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s(%d, %d, %d) time: %d, bitmap? %s", roomLabel, posX, posY, headDirection, timeTaken, isBitmapSet() ? "Yes" : "No");
    }
}
