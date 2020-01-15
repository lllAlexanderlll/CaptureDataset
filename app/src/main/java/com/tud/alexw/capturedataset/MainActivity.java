package com.tud.alexw.capturedataset;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.segway.robot.algo.Pose2D;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.vision.Vision;


public class MainActivity extends AppCompatActivity {

    private Vision mVision;
    private Head mHead;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get Vision SDK instance
        mVision = Vision.getInstance();
        mHead = Head.getInstance();

        mVision.bindService(this, mBindStateListenerVision);
        mHead.bindService(getApplicationContext(), mServiceBindListenerHead);
        mHead.setHeadJointYaw(0);
//        mHead.resetOrientation();

        final Button button = (Button) findViewById(R.id.CaptureBtn);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Button 'Capture' clicked");
                EditText inputPlaceLabel = (EditText)findViewById(R.id.inputPlaceLabel);
                String roomLabel = inputPlaceLabel.getText().toString();

                EditText inputX = (EditText)findViewById(R.id.inputX);
                int X = Integer.parseInt(inputX.getText().toString());

                EditText inputY = (EditText)findViewById(R.id.inputY);
                int Y = Integer.parseInt(inputY.getText().toString());

                ImageView imageView = (ImageView)findViewById(R.id.imageView);
                TextView textViewFilename = (TextView)findViewById(R.id.textViewFilename);

                AnnotatedImage annotatedImage;

                mHead.setHeadJointYaw(0);
                for(int headDirection_angle = 0; headDirection_angle < 360; headDirection_angle += 60){
                    mHead.setIncrementalYaw((float) Math.PI / 3); //TODO: Does it work that way? Otherwise test in sample app
                    ImageCapturer imageCapturer =  new ImageCapturer(mVision);
                    annotatedImage = imageCapturer.captureImage(roomLabel, X, Y, headDirection_angle);
                    imageView.setImageBitmap(annotatedImage.getBitmap());
                    textViewFilename.setText(annotatedImage.getFilename());
                }
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
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "Vision onUnbind() called with: reason = [" + reason + "]");
        }
    };

    private ServiceBinder.BindStateListener mServiceBindListenerHead = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            mHead.setMode(Head.MODE_ORIENTATION_LOCK);
            Log.d(TAG, "Head onBind() called. Smooth tracking active.");
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "Head onUnbind() called with: reason = [" + reason + "]");
        }
    };
}
