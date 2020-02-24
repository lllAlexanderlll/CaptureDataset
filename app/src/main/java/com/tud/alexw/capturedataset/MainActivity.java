package com.tud.alexw.capturedataset;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.tud.alexw.capturedataset.capture.APictureCapturingService;
import com.tud.alexw.capturedataset.capture.PictureCapturingListener;
import com.tud.alexw.capturedataset.capture.PictureCapturingServiceImpl;
import com.tud.alexw.capturedataset.head.MoveHead;
import com.tud.alexw.capturedataset.head.MoveHeadListener;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements PictureCapturingListener, MoveHeadListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private Head mHead;

    private static final String TAG = "MainActivity";

    private AnnotatedImage mAnnotatedImage;

    private static final String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1;

    private ImageView imageView;
    private EditText inputPlaceLabel;
    private EditText inputX;
    private EditText inputY;
    private EditText inputBaseYaw;

    private long captureTime_ms;
    private int loopCounter = 0;

    //The capture service
    private APictureCapturingService pictureService;

    private MoveHead moveHead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();
        // get Vision SDK instance
        mHead = Head.getInstance();
        mHead.bindService(getApplicationContext(), mServiceBindListenerHead);

        mAnnotatedImage = new AnnotatedImage();

        captureTime_ms = 0;

        imageView = (ImageView) findViewById(R.id.imageView);
        inputPlaceLabel = (EditText) findViewById(R.id.inputPlaceLabel);
        inputX = (EditText) findViewById(R.id.inputX);
        inputY = (EditText) findViewById(R.id.inputY);
        inputBaseYaw = (EditText) findViewById(R.id.inputBaseYaw);

        pictureService = PictureCapturingServiceImpl.getInstance(this);
        boolean saveImages = false;
        pictureService.setDoSaveImage(saveImages);
        if(!saveImages){
            findViewById(R.id.savingFlag).setVisibility(View.VISIBLE);
        }
        pictureService.startCapturing(this, mAnnotatedImage);

//        int[] pitchValues = {   0,   0,   0,   0,  0,  0,  0, 35,  35,  35,  35, 35, 35, 35, 145, 145, 145, 145, 145, 174, 174, 174, 174, 174};
//        int[] yawValues = {     0, -30, -60, -90, 90, 60, 30,  0, -30, -60, -90, 90, 60, 30,   0, -30, -60,  60,  30,   0, -30, -60,  60,  30};
//        int[] pitchValues = {   0, 35, 145, 174};
//        int[] yawValues = {     0,  0,   0,   0};
            int[] pitchValues = {   0, 45};
            int[] yawValues = {     0, 0};
        moveHead = new MoveHead(mHead, this, yawValues, pitchValues);


        final Button captureButton = (Button) findViewById(R.id.CaptureBtn);
        captureButton.setOnClickListener(v -> {
            Log.d(TAG, "Button 'Capture' clicked");
            mAnnotatedImage.setPosY(Integer.parseInt(inputY.getText().toString()));
            mAnnotatedImage.setPosX(Integer.parseInt(inputX.getText().toString()));
            mAnnotatedImage.setRoomLabel(inputPlaceLabel.getText().toString());

            mHead.resetOrientation();
            moveHead.next();
        });
    }

    @Override
    public void onHeadMovementDone(int yaw, int pitch) {
        Log.i(TAG, String.format("Head movement (%d, %d) done" , yaw, pitch));
        mAnnotatedImage.setYaw(yaw + Integer.parseInt(inputBaseYaw.getText().toString()));
        mAnnotatedImage.setPitch(pitch);
        captureTime_ms = System.currentTimeMillis();
        pictureService.capture();
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            if(System.currentTimeMillis() - captureTime_ms > 2000){
                Log.e(TAG, "Needed to restart capturing after time limit reached!");
                pictureService.capture();
            }
        }, 2000);
    }

    @Override
    public void onAllHeadMovementsDone(){
        Log.i(TAG, "No movements left! Capturing finished!");
        Log.e(TAG, "Start capturing all over again: Infinity loooooooop! #" + loopCounter++);
        pictureService.capture();
    }
    /**
     * Displaying the pictures taken.
     */
    @Override
    public void onCaptureDone(byte[] pictureData) {
        if (pictureData != null) {
            runOnUiThread(() -> showImage(pictureData));
            Log.i(TAG, String.format("Taking a photo took %d ms", System.currentTimeMillis() - captureTime_ms));
            captureTime_ms = System.currentTimeMillis();
            moveHead.next();
        }
    }

    @Override
    public void onCapturingFailed(){
        Log.e(TAG, "Capturing failed!");
        moveHead.retry();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mHead.isBind()) {
            mHead.bindService(getApplicationContext(), mServiceBindListenerHead);
        }
//        pictureService.startBackgroundThread();

    }

    @Override
    protected void onPause() {
//        pictureService.stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mHead.unbindService();
        pictureService.endCapturing();
        super.onStop();
        finish();
    }

    @Override
    protected void onDestroy() {
        mHead.unbindService();
        pictureService.endCapturing();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation/keyboard change
        super.onConfigurationChanged(newConfig);
    }

    private void showImage(byte[] pictureData) {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length, bmOptions);
        imageView.setImageBitmap(bitmap);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_CODE: {
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkPermissions();
                }
            }
        }
    }

    /**
     * checking  permissions at Runtime.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        final List<String> neededPermissions = new ArrayList<>();
        for (final String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }
        if (!neededPermissions.isEmpty()) {
            requestPermissions(neededPermissions.toArray(new String[]{}),
                    MY_PERMISSIONS_REQUEST_ACCESS_CODE);
        }
    }

    private ServiceBinder.BindStateListener mServiceBindListenerHead = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            int mode = Head.MODE_SMOOTH_TACKING;
            mHead.setMode(mode);
            Log.d(TAG, "Head onBind() called. " + (mode > Head.MODE_SMOOTH_TACKING ? "lock orientation" : "smooth tracking") + " mode");
            mHead.resetOrientation();
            mHead.setWorldPitch(Utils.degreeToRad(45));
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "Head onUnbind() called with: reason = [" + reason + "]");
        }
    };

}
