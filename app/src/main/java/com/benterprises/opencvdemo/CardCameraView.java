package com.benterprises.opencvdemo;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.io.FileOutputStream;

/**
 * Created by Ben on 11/4/17.
 */

public class CardCameraView extends JavaCameraView implements Camera.PictureCallback {

    private static final String TAG = CardCameraView.class.getSimpleName();
    private String mPictureFileName;

    public CardCameraView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    // not called anymore
    public void takePicture(final String fileName) {
        Camera.Size first = mCamera.getParameters().getSupportedPreviewSizes().get(1);
        Log.i(TAG, "CardCameraView: " + first.width + "x" + first.height);
        disconnectCamera();
        connectCamera(first.width, first.height);
        this.mPictureFileName = fileName;
        mCamera.setPreviewCallback(null);
        mCamera.takePicture(null, null, this);
    }

    public void onPictureTaken(byte[] data, Camera camera) {
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mPictureFileName);

            fos.write(data);
            fos.close();

        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }
    }
}
