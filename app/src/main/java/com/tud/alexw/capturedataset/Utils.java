package com.tud.alexw.capturedataset;

import android.content.Context;
import android.util.Log;

import com.segway.robot.sdk.locomotion.head.Head;

import java.io.File;
import java.io.IOException;

public class Utils {
    public static String TAG = "Utils";

    /**
     * Checks if storage structure i.e. folders are created. If not creates it. Throws IO exception, if folder couldn't be created
     * @param context Application context required for storage location
     * @return if storage structure is created after calling this method
     * @throws IOException
     */
    public static boolean isStorageStructureCreated(Context context) throws IOException {
        // Get the directory for the app's private pictures directory.
        File file = context.getExternalFilesDir(null);
        if(!file.exists()){
            if (!file.mkdirs()) {
                Log.e(TAG, "Directory not created");
                throw new IOException(TAG + "Couldn't create folder! ");
            }
            else{
                Log.i(TAG ,"Directory created: " + file.getAbsolutePath());
                return false;
            }
        }
        else{
            Log.i(TAG ,"Exists already!: " + file.getAbsolutePath());
            return true;
        }
    }

    /**
     * Transforms degrees to radian
     * @param degree degree value
     * @return corresponding radian value
     */
    public static float degreeToRad(int degree){
        return (float) (degree * Math.PI/180);
    }

    /**
     * Transforms radian to degree
     * @param rad radian value
     * @return corresponding degree value
     */
    public static int radToDegree(float rad){
        return (int) (rad* 180/Math.PI);
    }

    /**
     * Compares two degrees in a soft way (5° deviation allowed). Soft comparison, since robot head measurements "wiggle" a little. High value of 5° set for fast head movement.
     * @param deg1 degree value to compare
     * @param deg2 degree value to compare
     * @return whether the two values are close
     */
    public static boolean isClose(int deg1, int deg2){
        boolean result = Math.abs(deg1 - deg2) < 5;
        if(!result){
            Log.v(TAG, String.format("%d° != %d°", deg1, deg2));
        }
        return result;
    }


}
