package com.tud.alexw.capturedataset;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;


public class AnnotatedImage {

    private static final String TAG = "AnnotatedImage";

    String filename;
    long timeTaken;
    Bitmap bitmap;
    int posX, posY, yaw, pitch;
    String roomLabel;
    String parentPath;

    public AnnotatedImage(){
        this.bitmap = null;
        this.parentPath = null;
        this.roomLabel = null;
        this.timeTaken = -1;
        this.posX = -1;
        this.posY = -1;
        this.yaw = -1;
        this.pitch = -1;
    }

    public void decodeFilename(String path){
        int lastSlashIndex = path.lastIndexOf('/');
        this.parentPath = path.substring(0, lastSlashIndex - 1);
        this.filename = path.substring(lastSlashIndex, path.length() - 3);

        String[] segs = filename.split( Pattern.quote( "_" ) );
        if (segs.length == 5){
            this.timeTaken = Integer.parseInt(segs[0]);
            this.roomLabel = segs[1];
            this.posX = Integer.parseInt(segs[2]);
            this.posY = Integer.parseInt(segs[3]);
            this.yaw = Integer.parseInt(segs[4]);
        }
        else{
            String errorMsg = "Cannot construct AnnotatedImage. Filename couldn't be parsed: " + filename;
            Log.e(TAG, errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }

    public String encodeFilename(){
        if(timeTaken == -1 || yaw == -1 || roomLabel == null){
            String errorMsg = "Cannot save AnnotatedImage. Not fully initialised: " + this;
            Log.e(TAG, errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        else {
            String timeFormatted = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Timestamp(timeTaken));
            return String.format("%s_%s_%d_%d_%d_%d.jpg", timeFormatted, roomLabel, posX, posY, yaw, pitch);
        }
    }

    public void load(String path){
        decodeFilename(path);

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

    public void saveImageToExternalStorage(Context context) {
        filename = encodeFilename();

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + File.separator
                + "lab_reverse"
                + File.separator;
        try {
            File directory = new File(path);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File file = new File(directory, filename);
            file.createNewFile();

            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

            outputStream.flush();
            outputStream.getFD().sync();
            outputStream.close();

            MediaScannerConnection.scanFile(context,
                    new String[] { file.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void setTimeTaken(long timeTaken) {
        this.timeTaken = timeTaken;
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

    public int getYaw() {
        return yaw;
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

    public void setYaw(int headDirection) {
        this.yaw = headDirection;
    }

    public void setRoomLabel(String roomLabel) {
        this.roomLabel = roomLabel.replace(' ', '_');
    }

    public int getPitch() {
        return pitch;
    }

    public void setPitch(int pitch) {
        this.pitch = pitch;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s(%d, %d, %d) time: %d, bitmap? %s", roomLabel, posX, posY, yaw, timeTaken, isBitmapSet() ? "Yes" : "No");
    }
}
