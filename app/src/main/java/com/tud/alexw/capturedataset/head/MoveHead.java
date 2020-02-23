package com.tud.alexw.capturedataset.head;

import android.util.Log;

import com.segway.robot.sdk.locomotion.head.Head;
import com.tud.alexw.capturedataset.Utils;

import static com.tud.alexw.capturedataset.Utils.degreeToRad;
import static com.tud.alexw.capturedataset.Utils.isClose;
import static com.tud.alexw.capturedataset.Utils.radToDegree;

public class MoveHead {

    private String TAG = "MoveHead";
    private MoveHeadListener moveHeadListener;
    private int[] yaws, pitches;
    private Head mHead;
    private int counter = 0;

    public MoveHead(Head head, MoveHeadListener listener, int[] yaws, int[] pitches){
        this.pitches = pitches;
        this.yaws = yaws;
        mHead = head;
        moveHeadListener = listener;
    }

    private void reset(){
        counter = 0;
    }

    public void next(){
        if(counter < yaws.length){
            moveHead(yaws[counter], pitches[counter]);
            moveHeadListener.onHeadMovementDone(yaws[counter], pitches[counter]);
            counter++;
        }
        else{
            reset();
            mHead.resetOrientation();
            mHead.setWorldPitch(Utils.degreeToRad(45));
        }
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
    }
}
