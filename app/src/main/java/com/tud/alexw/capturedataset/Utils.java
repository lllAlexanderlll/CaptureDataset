package com.tud.alexw.capturedataset;

import android.content.Context;
import android.util.Log;

import com.segway.robot.sdk.locomotion.head.Head;

import java.io.File;
import java.io.IOException;

public class Utils {
    public static String TAG = "Utils";


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

    public static float degreeToRad(int degree){
        return (float) (degree * Math.PI/180);
    }

    public static int radToDegree(float rad){
        return (int) (rad* 180/Math.PI);
    }

    public static boolean isClose(int deg1, int deg2){
        boolean result = Math.abs(deg1 - deg2) < 5;
        if(!result){
            Log.v(TAG, String.format("%d° != %d°", deg1, deg2));
        }
        return result;
    }


}
