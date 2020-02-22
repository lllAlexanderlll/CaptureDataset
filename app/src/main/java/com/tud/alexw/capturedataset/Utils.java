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

    public static void moveHead(Head head, int yaw_deg, int pitch_deg){
        if(yaw_deg > 144 || yaw_deg < -144 || pitch_deg < 0 || pitch_deg > 174){
            Log.e(TAG, String.format("Yaw: %d not in [-144, 144] or pitch: %d not in [0, 174]", yaw_deg, pitch_deg));
            return;
        }

        head.setHeadJointYaw(degreeToRad(yaw_deg));
        head.setWorldPitch(degreeToRad(pitch_deg));
        Log.i(TAG,String.format("Current motor pitch and yaw values: %f, %f", head.getHeadJointYaw().getAngle(), head.getWorldPitch().getAngle()));
        Log.i(TAG,String.format("Set motor pitch and yaw values: %f, %f", degreeToRad(yaw_deg), degreeToRad(pitch_deg)));
        while (
                !(isClose(radToDegree(head.getHeadJointYaw().getAngle()), yaw_deg) &&
                        isClose(radToDegree(head.getWorldPitch().getAngle()), pitch_deg))
        ) {
            Log.i(TAG, String.format("Waiting for Head to turn from (%d, %d) to (%d, %d)", radToDegree(head.getHeadJointYaw().getAngle()), radToDegree(head.getWorldPitch().getAngle()), yaw_deg, pitch_deg));
        }
    }
}
