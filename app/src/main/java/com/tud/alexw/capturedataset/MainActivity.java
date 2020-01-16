package com.tud.alexw.capturedataset;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.vision.Vision;

public class MainActivity extends AppCompatActivity {

    private Vision mVision;
    private Head mHead;
    private boolean isVisionBound = false;
    private static final String TAG = "MainActivity";
    private ImageCapturer mImageCapturer;
    private AnnotatedImage mAnnotatedImage;

    private float degreeToRad(int degree){
        return (float) (degree * Math.PI/180);
    }

    private int radToDegree(float rad){
        return (int) (rad* 180/Math.PI);
    }

    private boolean isClose(int deg1, int deg2){
        boolean result = deg1 == deg2;
        if(!result){
            Log.v(TAG, String.format("%d° != %d°", deg1, deg2));
        }
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get Vision SDK instance
        mVision = Vision.getInstance();
        mHead = Head.getInstance();

        mVision.bindService(this, mBindStateListenerVision);
        mHead.bindService(getApplicationContext(), mServiceBindListenerHead);
        mImageCapturer =  new ImageCapturer(mVision);
        mAnnotatedImage = new AnnotatedImage();


        final Button button = (Button) findViewById(R.id.CaptureBtn);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Button 'Capture' clicked");
                EditText inputPlaceLabel = (EditText)findViewById(R.id.inputPlaceLabel);
                final String roomLabel = inputPlaceLabel.getText().toString();

                EditText inputX = (EditText)findViewById(R.id.inputX);
                final int X = Integer.parseInt(inputX.getText().toString());

                EditText inputY = (EditText)findViewById(R.id.inputY);
                final int Y = Integer.parseInt(inputY.getText().toString());

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        //Note range head: -141° - 142,5°
                        int startDirection_deg = -140;
                        float startDirection_rad = degreeToRad(startDirection_deg);
                        mHead.setHeadJointYaw(startDirection_rad);
                        while (!isClose(radToDegree(mHead.getHeadJointYaw().getAngle()), startDirection_deg)) {
                            Log.v(TAG, String.format("waiting for startDirection_rad"));
                        }
                        int nViews = 8;
                        for (int viewCount = 0; viewCount <= nViews; viewCount++) {
                            Log.d(TAG, "" + viewCount);
                            int setDirection_deg = viewCount * 35;
                            float setDirection_rad = startDirection_rad + degreeToRad(setDirection_deg);
                            mHead.setHeadJointYaw(setDirection_rad);
                            while (!isClose(radToDegree(mHead.getHeadJointYaw().getAngle()), startDirection_deg + setDirection_deg)) {
                                Log.v(TAG, String.format("waiting for setDirection_rad"));
                            }
                            mAnnotatedImage = mImageCapturer.captureImage(roomLabel, X, Y, startDirection_deg + setDirection_deg);
                            if (mAnnotatedImage.getBitmap() != null) {
                                mAnnotatedImage.save(getApplicationContext());
                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        ImageView imageView = (ImageView)findViewById(R.id.imageView);
                                        TextView textViewFilename = (TextView)findViewById(R.id.textViewFilename);
                                        imageView.setImageBitmap(mAnnotatedImage.getBitmap());
                                        textViewFilename.setText(mAnnotatedImage.getFilename());

                                    }
                                });

                            }
                        }
                    }
                }).start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!mHead.isBind()){
            mHead.bindService(getApplicationContext(), mServiceBindListenerHead);
        }
        mVision.bindService(this, mBindStateListenerVision);
    }

    @Override
    protected void onStop() {
        mImageCapturer.stop();
        mVision.unbindService();
        mHead.unbindService();
        super.onStop();
        finish();
    }

    @Override
    protected void onDestroy() {
        mVision.unbindService();
        mHead.unbindService();
        super.onDestroy();
    }

    ServiceBinder.BindStateListener mBindStateListenerVision = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "Vision onBind() called");
            mImageCapturer.start();
            isVisionBound = true;
            Button button = (Button) findViewById(R.id.CaptureBtn);
            while(!(isVisionBound && mImageCapturer.gotBitmap())){
                Log.v(TAG, String.format("Waiting for bitmap: %b isVisionBound: %b", isVisionBound, mImageCapturer.gotBitmap()));
            }
            button.setEnabled(isVisionBound);
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "Vision onUnbind() called with: reason = [" + reason + "]");
        }
    };

    private ServiceBinder.BindStateListener mServiceBindListenerHead = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            int mode = Head.MODE_SMOOTH_TACKING;
            mHead.setMode(mode);
            Log.d(TAG, "Head onBind() called. " + (mode > Head.MODE_SMOOTH_TACKING ? "lock orientation" : "smooth tracking") + " mode");
            mHead.resetOrientation();
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "Head onUnbind() called with: reason = [" + reason + "]");
        }
    };

}
