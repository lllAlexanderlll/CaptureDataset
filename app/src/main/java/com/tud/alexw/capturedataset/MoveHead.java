package com.tud.alexw.capturedataset;

import android.util.Log;

import com.segway.robot.sdk.locomotion.head.Head;

import static com.tud.alexw.capturedataset.Utils.degreeToRad;
import static com.tud.alexw.capturedataset.Utils.isClose;
import static com.tud.alexw.capturedataset.Utils.radToDegree;

public class MoveHead {

    private String TAG = "MoveHead";
    private MoveHeadListener moveHeadListener;
    private int[] yaws, pitchs;
    private Head mHead;

    MoveHead(Head head, int[] yaws, int[] pitchs){
        this.pitchs = pitchs;
        this.yaws = yaws;
        mHead = head;
    }

    public void next(){
        //TODO;
        throw new IllegalStateException("Not implemented yet");
    }

    private void moveHead(int yaw_deg, int pitch_deg){
        if(yaw_deg > 144 || yaw_deg < -144 || pitch_deg < 0 || pitch_deg > 174){
            Log.e(TAG, String.format("Yaw: %d not in [-144, 144] or pitch: %d not in [0, 174]", yaw_deg, pitch_deg));
            return;
        }

        mHead.setHeadJointYaw(degreeToRad(yaw_deg));
        mHead.setWorldPitch(degreeToRad(pitch_deg));
        Log.i(TAG,String.format("Current motor pitch and yaw values: %f, %f", mHead.getHeadJointYaw().getAngle(), mHead.getWorldPitch().getAngle()));
        Log.i(TAG,String.format("Set motor pitch and yaw values: %f, %f", degreeToRad(yaw_deg), degreeToRad(pitch_deg)));
        while (
                !(isClose(radToDegree(mHead.getHeadJointYaw().getAngle()), yaw_deg) &&
                        isClose(radToDegree(mHead.getWorldPitch().getAngle()), pitch_deg))
        ) {
            Log.v(TAG, String.format("Waiting for Head to turn from (%d, %d) to (%d, %d)", radToDegree(mHead.getHeadJointYaw().getAngle()), radToDegree(mHead.getWorldPitch().getAngle()), yaw_deg, pitch_deg));
        }
        moveHeadListener.onHeadMovementDone();
    }
}
