package com.tud.alexw.capturedataset;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements PictureCapturingListener, MoveHeadListener, ActivityCompat.OnRequestPermissionsResultCallback{

    private Head mHead;

    private static final String TAG = "MainActivity";

    private AnnotatedImage mAnnotatedImage;

    private static final String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1;

    private ImageView imageView;
    private TextView textViewFilename;

    //The capture service
    private APictureCapturingService pictureService;

    private MoveHead moveHead;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissions();
        //remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        // get Vision SDK instance
        mHead = Head.getInstance();
        mHead.bindService(getApplicationContext(), mServiceBindListenerHead);

        mAnnotatedImage = new AnnotatedImage();

        imageView = (ImageView) findViewById(R.id.imageView);
        textViewFilename = (TextView) findViewById(R.id.textViewFilename);

        pictureService = PictureCapturingServiceImpl.getInstance(this);


        final Button captureButton = (Button) findViewById(R.id.CaptureBtn);
        captureButton.setOnClickListener(v -> {
                Log.d(TAG, "Button 'Capture' clicked");
                EditText inputPlaceLabel = (EditText)findViewById(R.id.inputPlaceLabel);
                final String roomLabel = inputPlaceLabel.getText().toString();

                EditText inputX = (EditText)findViewById(R.id.inputX);
                final int X = Integer.parseInt(inputX.getText().toString());

                EditText inputY = (EditText)findViewById(R.id.inputY);
                final int Y = Integer.parseInt(inputY.getText().toString());

                mHead.resetOrientation();
                int[] pitchValues = {0, 45};
                int[] yawValues = {0, 0};
                for(int i = 0; i < yawValues.length; i++) {
                    moveHead.next();
//                  takePicture(roomLabel, X, Y, yawValues[i], pitchValues[i]);
//                    if (mAnnotatedImage.getBitmap() != null) {
//                        mAnnotatedImage.saveImageToExternalStorage(getApplicationContext());
//                        runOnUiThread(() -> {
//
//                            imageView.setImageBitmap(mAnnotatedImage.getBitmap());
//                            textViewFilename.setText(mAnnotatedImage.getFilename());
//
//                        });
//
//                    }
                }
                mHead.resetOrientation();
                mHead.setWorldPitch(Utils.degreeToRad(45));
        });
    }

    @Override
    public void onHeadMovementDone() {
        pictureService.startCapturing(this);
    }

    /**
     * Displaying the pictures taken.
     */
    @Override
    public void onCaptureDone(String pictureUrl, byte[] pictureData) {
        if (pictureData != null && pictureUrl != null) {
            runOnUiThread(() -> {
                final Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
                final int nh = (int) (bitmap.getHeight() * (512.0 / bitmap.getWidth()));
                final Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true);
                imageView.setImageBitmap(scaled);
            });
        }
        //TODO: Start next head move
        moveHead.next();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(!mHead.isBind()){
            mHead.bindService(getApplicationContext(), mServiceBindListenerHead);
        }
        pictureService.startBackgroundThread();

    }

    @Override
    protected void onPause() {
        pictureService.stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mHead.unbindService();
        super.onStop();
        finish();
    }

    @Override
    protected void onDestroy() {
        mHead.unbindService();
        super.onDestroy();
    }

    private void showToast(final String text) {
        runOnUiThread(() ->
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * We've finished taking pictures from all phone's cameras
     */
    @Override
    public void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken) {
        if (picturesTaken != null && !picturesTaken.isEmpty()) {
            showToast("Done capturing all photos!");
            return;
        }
        showToast("No camera detected!");
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
