package com.tud.alexw.capturedataset;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.base.log.Logger;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.vision.Vision;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private Vision mVision;
    private Head mHead;
    private boolean isVisionBound = false;
    private static final String TAG = "MainActivity";
    private ImageCapturer mImageCapturer;
    private AnnotatedImage mAnnotatedImage;

    private Button takePictureButton;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private ImageReader imageReaderV2;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

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

        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        final Button captureButton = (Button) findViewById(R.id.CaptureBtn);
        captureButton.setOnClickListener(new View.OnClickListener() {
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
                        mHead.resetOrientation();
                        int[] pitchValues = {0, 45};
                        int[] yawValues = {0, 45};
                        for(int i = 0; i < yawValues.length; i++) {
                            Utils.moveHead(mHead, yawValues[i], pitchValues[i]);
                            takePicture(roomLabel, X, Y, yawValues[i], pitchValues[i]);
                            if (mAnnotatedImage.getBitmap() != null) {
                                mAnnotatedImage.saveImageToExternalStorage(getApplicationContext());
                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        ImageView imageView = (ImageView) findViewById(R.id.imageView);
                                        TextView textViewFilename = (TextView) findViewById(R.id.textViewFilename);
                                        imageView.setImageBitmap(mAnnotatedImage.getBitmap());
                                        textViewFilename.setText(mAnnotatedImage.getFilename());

                                    }
                                });

                            }
                        }
                        mHead.resetOrientation();
                        mHead.setWorldPitch(Utils.degreeToRad(45));
                    }
                }).start();
            }
        });
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.i(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
            Log.e(TAG, "Error encountered");
        }
    };

    protected void takePicture(String roomLabel, int posX, int posY, int yaw, int pitch) {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        mAnnotatedImage.setRoomLabel(roomLabel);
        mAnnotatedImage.setPosX(posX);
        mAnnotatedImage.setPosY(posY);
        mAnnotatedImage.setYaw(yaw);
        mAnnotatedImage.setPitch(pitch);

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            imageReaderV2 = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(imageReaderV2.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReaderV2.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        Log.i(TAG, "Image is available!");
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        mAnnotatedImage.setBitmap(BitmapFactory.decodeByteArray(bytes,0,bytes.length));
//                        save(bytes);
                        mAnnotatedImage.saveImageToExternalStorage(getApplicationContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            };
            imageReaderV2.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Log.i(TAG, "Capture completed");
                    createCameraPreview();
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    Log.e(TAG, "Capturing failed");
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e(TAG, "Configuration failed");
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {

                    if (null == cameraDevice) {
                        Log.e(TAG, "Camera is already closed!");
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
//                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, "Configuration failed");
                }

                @Override
                public void onReady(@NonNull CameraCaptureSession session) {
                    super.onReady(session);
                    Log.i(TAG, "Camera device is ready");
                }

                @Override
                public void onActive(@NonNull CameraCaptureSession session) {
                    super.onActive(session);
                    Log.i(TAG, "Camera device is active");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.i(TAG, "open camera called");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
            Log.i(TAG, "Camera " + cameraId + " opened");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        Log.i(TAG, "close camera called!");
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!mHead.isBind()){
            mHead.bindService(getApplicationContext(), mServiceBindListenerHead);
        }
        mVision.bindService(this, mBindStateListenerVision);
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
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
        closeCamera();
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
            mHead.setWorldPitch(Utils.degreeToRad(45));
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "Head onUnbind() called with: reason = [" + reason + "]");
        }
    };

}
