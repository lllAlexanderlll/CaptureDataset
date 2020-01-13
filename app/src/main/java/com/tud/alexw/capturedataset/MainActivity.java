package com.tud.alexw.capturedataset;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.segway.robot.sdk.vision.Vision;

public class MainActivity extends AppCompatActivity {

    private Vision mVision;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get Vision SDK instance
        mVision = Vision.getInstance();
        final Button button = (Button) findViewById(R.id.CaptureBtn);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText editText = (EditText)findViewById(R.id.placeLabelEditText);

                String roomLabel = editText.getText().toString();
                ImageCapturer imageCapturer =  new ImageCapturer(mVision, new AnnotatedImage());
                imageCapturer.captureImage(roomLabel, 0,0,0);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStop() {
        super.onStop();
        mVision.unbindService();
        finish();
    }
}
