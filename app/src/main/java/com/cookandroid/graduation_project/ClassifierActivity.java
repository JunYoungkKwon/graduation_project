/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cookandroid.graduation_project;

import static java.lang.System.currentTimeMillis;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;

import com.cookandroid.graduation_project.env.BorderedText;
import com.cookandroid.graduation_project.tflite.Classifier;
import com.cookandroid.graduation_project.tflite.Classifier.Device;

import java.io.IOException;
import java.util.List;

public class ClassifierActivity extends com.cookandroid.graduation_project.CameraActivity implements OnImageAvailableListener {
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final float TEXT_SIZE_DIP = 10;
  private Bitmap rgbFrameBitmap = null;
  private Integer sensorOrientation;
  private Classifier classifier;
  private BorderedText borderedText;
  private String email;

  @Override
  protected int getLayoutId() {
    return R.layout.tfe_ic_camera_connection_fragment;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    recreateClassifier(getDevice(), getNumThreads());
    if (classifier == null) {
      return;
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();

    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);

    Intent intent2 = getIntent();
    email = intent2.getStringExtra("email");

  }



  @Override
  protected void processImage() {
    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            if (classifier != null) {
              final long startTime = SystemClock.uptimeMillis();
              final List<Classifier.Recognition> results =
                  classifier.recognizeImage(rgbFrameBitmap, sensorOrientation);

              if(timeForInference + 3000 < currentTimeMillis()){
                timeForInference = currentTimeMillis();
                runOnUiThread(
                        new Runnable() {
                          @Override
                          public void run() {

                            showResultsInBottomSheet(results, email);
                          }
                        });
                  }
                }
                readyForNextImage();
              }
            });
  }

  @Override
  protected void onInferenceConfigurationChanged() {
    if (rgbFrameBitmap == null) {
      // Defer creation until we're getting camera frames.
      return;
    }
    final Device device = getDevice();
    final int numThreads = getNumThreads();
    runInBackground(() -> recreateClassifier(device, numThreads));
  }

  private void recreateClassifier(Device device, int numThreads) {
    if (classifier != null) {
      classifier.close();
      classifier = null;
    }
    try {

      classifier = Classifier.create(this, device, numThreads);
    } catch (IOException e) {
    }
  }
}