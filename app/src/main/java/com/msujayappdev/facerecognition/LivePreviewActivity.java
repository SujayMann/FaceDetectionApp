/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.msujayappdev.facerecognition;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCapture;

import com.google.android.gms.common.annotation.KeepName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Live preview demo for ML Kit APIs. */
@KeepName
public final class LivePreviewActivity extends AppCompatActivity
    implements OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {
  private static final String OBJECT_DETECTION = "Object Detection";
  private static final String OBJECT_DETECTION_CUSTOM = "Custom Object Detection";
  private static final String CUSTOM_AUTOML_OBJECT_DETECTION =
      "Custom AutoML Object Detection (Flower)";
  private static final String FACE_DETECTION = "Face Detection";
  private static final String BARCODE_SCANNING = "Barcode Scanning";
  private static final String IMAGE_LABELING = "Image Labeling";
  private static final String IMAGE_LABELING_CUSTOM = "Custom Image Labeling (Birds)";
  private static final String CUSTOM_AUTOML_LABELING = "Custom AutoML Image Labeling (Flower)";
  private static final String POSE_DETECTION = "Pose Detection";
  private static final String SELFIE_SEGMENTATION = "Selfie Segmentation";
  private static final String TEXT_RECOGNITION_LATIN = "Text Recognition Latin";
  private static final String TEXT_RECOGNITION_CHINESE = "Text Recognition Chinese";
  private static final String TEXT_RECOGNITION_DEVANAGARI = "Text Recognition Devanagari";
  private static final String TEXT_RECOGNITION_JAPANESE = "Text Recognition Japanese";
  private static final String TEXT_RECOGNITION_KOREAN = "Text Recognition Korean";
  private static final String FACE_MESH_DETECTION = "Face Mesh Detection (Beta)";

  private static final String TAG = "LivePreviewActivity";

  private CameraSource cameraSource = null;
  private CameraSourcePreview preview;
  private GraphicOverlay graphicOverlay;
  private String selectedModel = OBJECT_DETECTION;

  ImageView flipCameraImg, captureImage, imageDone;
  private boolean isFrontFacing = true;
  Bitmap bitmap;
  Boolean faceDetected = false;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate");

    setContentView(R.layout.activity_vision_live_preview);

    flipCameraImg = findViewById(R.id.flipCameraImg);
    captureImage = findViewById(R.id.captureImage);
    imageDone = findViewById(R.id.imageDone);

    flipCameraImg.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        isFrontFacing = !isFrontFacing;
        toggleCamera();
      }
    });

    captureImage.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if(faceDetected){
          bitmap = loadBitmapFrontView(graphicOverlay);
          saveToGallery(bitmap);
          imageDone.setVisibility(View.VISIBLE);
        }
      }
    });

    preview = findViewById(R.id.preview_view);
    if (preview == null) {
      Log.d(TAG, "Preview is null");
    }
    graphicOverlay = findViewById(R.id.graphic_overlay);
    if (graphicOverlay == null) {
      Log.d(TAG, "graphicOverlay is null");
    }
    createCameraSource(selectedModel);
  }

  public static Bitmap loadBitmapFrontView(View view){
    Bitmap b = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(b);
    view.draw(c);
    return b;
  }

  private void saveToGallery(Bitmap bitmap) {
    File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    String timeStamp = new SimpleDateFormat("yyyyMMdd.HHmmss", Locale.getDefault()).format(new Date());
    String fileName = "IMG." + timeStamp + ".jpg";
    File imageFile = new File(storageDir, fileName);
    try{
      FileOutputStream outputStream = new FileOutputStream(imageFile);
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
      outputStream.flush();
      outputStream.close();

      Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
      mediaScanIntent.setData(Uri.fromFile(imageFile));
      sendBroadcast(mediaScanIntent);
      Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();

    }catch (Exception e){
      Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
    }
  }
  private void toggleCamera(){
    Log.d(TAG, "self facing");
    if(cameraSource!=null){
      if(isFrontFacing){
        cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
      }
      else{
        cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
      }
    }
    preview.stop();
    startCameraSource();
}
  @Override
  public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    // An item was selected. You can retrieve the selected item using
    // parent.getItemAtPosition(pos)
    selectedModel = parent.getItemAtPosition(pos).toString();
    Log.d(TAG, "Selected model: " + selectedModel);
    preview.stop();
    createCameraSource(selectedModel);
    startCameraSource();
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // Do nothing.
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    Log.d(TAG, "Set facing");
    if (cameraSource != null) {
      if (isChecked) {
        cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
      } else {
        cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
      }
    }
    preview.stop();
    startCameraSource();
  }

  private void createCameraSource(String model) {
    // If there's no existing cameraSource, create one.
    if (cameraSource == null) {
      cameraSource = new CameraSource(this, graphicOverlay);
    }
    // Initialize camera source
    Log.i(TAG, "Using Face Detector Processor");
    cameraSource.setMachineLearningFrameProcessor(new FaceDetectorProcessor(this, new OnFaceDetectedListener() {
      @Override
      public void onFaceDetected(Boolean isDetected) {
        if(isDetected){
          captureImage.setImageResource(R.drawable.baseline_camera_white);
          faceDetected = true;
        }
        else{
          captureImage.setImageResource(R.drawable.baseline_camera_grey);
          imageDone.setVisibility(View.INVISIBLE);
          faceDetected = false;
        }
      }
    }));
  }

  /**
   * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
   * (e.g., because onResume was called before the camera source was created), this will be called
   * again when the camera source is created.
   */
  private void startCameraSource() {
    if (cameraSource != null) {
      try {
        if (preview == null) {
          Log.d(TAG, "resume: Preview is null");
        }
        if (graphicOverlay == null) {
          Log.d(TAG, "resume: graphOverlay is null");
        }
        preview.start(cameraSource, graphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        cameraSource.release();
        cameraSource = null;
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    createCameraSource(selectedModel);
    startCameraSource();
  }

  /** Stops the camera. */
  @Override
  protected void onPause() {
    super.onPause();
    preview.stop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (cameraSource != null) {
      cameraSource.release();
    }
  }
}
