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


/**
 * stores image information as Bitmap object and corresponding annotation info. De- and encodes filenames for images.
 */
public class AnnotatedImage {

    private static final String TAG = "AnnotatedImage";

    String filename;
    long timeTaken;
    Bitmap bitmap;
    int posX, posY, yaw, pitch;
    String roomLabel;
    String parentPath;

    /**
     * Creates an empty annotationImage with dummy annotations and nulls
     */
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

    /**
     * Decodes a given annotated image filename into annotations of the calling object
     * @param path filename or path to filename
     */
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

    /**
     * Transforms local pitch and yaw coordinates relative to the robots head to global coordinates
     */
    private void transformToGlobalCoordinates(){
        // convert local pitch and yaw to global measurements

        if(pitch > 90){
            if(pitch == 174){
                pitch = 0;
            }
            else{
                pitch -= 90;
            }
            yaw += 180;
        }
        yaw %= 360;
    }

    /**
     * Encodes annotations into image filename
     * @return the annotated image filename
     */
    public String encodeFilename(){
        transformToGlobalCoordinates();
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

    /**
     * Loads a bitmap from a given path
     * @param path the path
     */
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

    /**
     * Saves the image and annotations to external public storage
     * @param context
     */
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
