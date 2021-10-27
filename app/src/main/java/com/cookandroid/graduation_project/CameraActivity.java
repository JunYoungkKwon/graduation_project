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

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import com.cookandroid.graduation_project.env.ImageUtils;
import com.cookandroid.graduation_project.tflite.Classifier.Device;
import com.cookandroid.graduation_project.tflite.Classifier.Recognition;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public abstract class CameraActivity extends AppCompatActivity
        implements OnImageAvailableListener,
        Camera.PreviewCallback,
        View.OnClickListener,
        AdapterView.OnItemSelectedListener {

  private static final int PERMISSIONS_REQUEST = 1;

  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  protected int previewWidth = 0;
  protected int previewHeight = 0;
  private Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;

  public long timeForInference;

  private Device device = Device.CPU;
  private int numThreads = -1;
  private String email;
  int reportNum = 1;
  boolean state = false;

  private GpsTracker gpsTracker;

  private static final int GPS_ENABLE_REQUEST_CODE = 2001;
  private static final int PERMISSIONS_REQUEST_CODE = 100;
  private static final String[] PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

  private static DatabaseReference mDatabase;
  DateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
  int i = 1;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.tfe_ic_activity_camera);

    Intent intent1 = getIntent();

    email = intent1.getStringExtra("email");

    mDatabase = FirebaseDatabase.getInstance().getReference();

    if (hasPermission()) {
      setFragment();
    } else {
      requestPermission();
    }

    timeForInference = currentTimeMillis();

    if (!checkLocationServicesStatus()) {

      showDialogForLocationServiceSetting();
    }else {

      checkRunTimePermission();
    }


    //Toast.makeText(CameraActivity.this, "현재위치 \n위도 " + latitude + "\n경도 " + longitude, Toast.LENGTH_LONG).show();
    //Toast.makeText(CameraActivity.this, address , Toast.LENGTH_LONG).show();
  }

  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }

  protected int getLuminanceStride() {
    return yRowStride;
  }

  protected byte[] getLuminance() {
    return yuvBytes[0];
  }

  /** Callback for android.hardware.Camera API */
  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
      }
    } catch (final Exception e) {
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    yRowStride = previewWidth;

    imageConverter =
            new Runnable() {
              @Override
              public void run() {
                ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
              }
            };

    postInferenceCallback =
            new Runnable() {
              @Override
              public void run() {
                camera.addCallbackBuffer(bytes);
                isProcessingFrame = false;
              }
            };
    processImage();
  }

  /** Callback for Camera2 API */
  @Override
  public void onImageAvailable(final ImageReader reader) {

    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }


      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
              new Runnable() {
                @Override
                public void run() {
                  ImageUtils.convertYUV420ToARGB8888(
                          yuvBytes[0],
                          yuvBytes[1],
                          yuvBytes[2],
                          previewWidth,
                          previewHeight,
                          yRowStride,
                          uvRowStride,
                          uvPixelStride,
                          rgbBytes);
                }
              };

      postInferenceCallback =
              new Runnable() {
                @Override
                public void run() {
                  image.close();
                  isProcessingFrame = false;
                }
              };

      processImage();

    } catch (final Exception e) {
      Trace.endSection();
      return;
    }
    Trace.endSection();

  }

  @Override
  public synchronized void onStart() {
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    super.onResume();

    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  @Override
  public synchronized void onPause() {

    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
    }

    super.onPause();
  }

  @Override
  public synchronized void onStop() {
    super.onStop();
  }

  @Override
  public synchronized void onDestroy() {
    super.onDestroy();
  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @Override
  public void onRequestPermissionsResult(
          final int requestCode, final String[] permissions, final int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSIONS_REQUEST) {
      if (allPermissionsGranted(grantResults)) {
        setFragment();
      } else {
        requestPermission();
      }
    }
  }

  private static boolean allPermissionsGranted(final int[] grantResults) {
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                CameraActivity.this,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG)
                .show();
      }
      requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
    }
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
          CameraCharacteristics characteristics, int requiredLevel) {
    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return requiredLevel == deviceLevel;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return requiredLevel <= deviceLevel;
  }

  private String chooseCamera() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    try {
      for (final String cameraId : manager.getCameraIdList()) {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        final StreamConfigurationMap map =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
          continue;
        }

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        useCamera2API =
                (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                        || isHardwareLevelSupported(
                        characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        return cameraId;
      }
    } catch (CameraAccessException e) {
    }

    return null;
  }

  protected void setFragment() {
    String cameraId = chooseCamera();

    Fragment fragment;
    if (useCamera2API) {
      CameraConnectionFragment camera2Fragment =
              CameraConnectionFragment.newInstance(
                      new CameraConnectionFragment.ConnectionCallback() {
                        @Override
                        public void onPreviewSizeChosen(final Size size, final int rotation) {
                          previewHeight = size.getHeight();
                          previewWidth = size.getWidth();
                          CameraActivity.this.onPreviewSizeChosen(size, rotation);
                        }
                      },
                      this,
                      getLayoutId(),
                      getDesiredPreviewFrameSize());

      camera2Fragment.setCamera(cameraId);
      fragment = camera2Fragment;
    } else {
      fragment =
              new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
    }

    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
    }
  }

  //minyong
  @UiThread
  protected void showResultsInBottomSheet(List<Recognition> results,String email) {
    if (results != null) {
      Recognition recognition = results.get(0);
      Toast.makeText(this.getApplicationContext(), recognition.getTitle(), Toast.LENGTH_SHORT).show();
    }

    long now = System.currentTimeMillis();
    Date date = new Date(now);
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

    gpsTracker = new GpsTracker(CameraActivity.this);

    double latitude = gpsTracker.getLatitude();
    double longitude = gpsTracker.getLongitude();

    String address = getCurrentAddress(latitude, longitude);

    String time = simpleDateFormat.format(date);

    HashMap result = new HashMap<>();
    result.put("time", time);
    result.put("email", email);
    result.put("state", state);
    result.put("address", address);


    writeUser(Integer.toString(i++), time, email, state, address);
  }


  protected Device getDevice() {
    return device;
  }

  protected int getNumThreads() {
    return numThreads;
  }

  protected abstract void processImage();

  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

  protected abstract int getLayoutId();

  protected abstract Size getDesiredPreviewFrameSize();

  protected abstract void onInferenceConfigurationChanged();

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

  }

  @Override
  public void onClick(View v) {

  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // Do nothing.
  }

  private void writeUser(String s, String time, String email, boolean state, String address) {
    ReportData reportData =  new ReportData(time, email,  state, address);

    //데이터 저장
    mDatabase.child("reports").push().setValue(reportData)
            .addOnSuccessListener(new OnSuccessListener<Void>() { //데이터베이스에 넘어간 이후 처리
              @Override
              public void onSuccess(Void aVoid) {
                Toast.makeText(getApplicationContext(),"저장을 완료했습니다", Toast.LENGTH_LONG).show();
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(),"저장에 실패했습니다" , Toast.LENGTH_LONG).show();
              }
            });
  }


  @Override
  public void onRequestPermissionsResult(int permsRequestCode, @NonNull String[] permissions, @NonNull int[] grandResults) {

    super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults);

    if (permsRequestCode == PERMISSIONS_REQUEST) {
      if (allPermissionsGranted(grandResults)) {
        setFragment();
      } else {
        requestPermission();
      }
    }


    if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == PERMISSIONS.length) {

      // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

      boolean check_result = true;


      // 모든 퍼미션을 허용했는지 체크합니다.

      for (int result : grandResults) {
        if (result != PackageManager.PERMISSION_GRANTED) {
          check_result = false;
          break;
        }
      }


      if (check_result) {

        //위치 값을 가져올 수 있음
        ;
      } else {
        // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSIONS[0])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSIONS[1])) {

          Toast.makeText(CameraActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
          finish();


        } else {

          Toast.makeText(CameraActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

        }
      }

    }
  }

  void checkRunTimePermission(){

    //런타임 퍼미션 처리
    // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
    int hasFineLocationPermission = ContextCompat.checkSelfPermission(CameraActivity.this,
            Manifest.permission.ACCESS_FINE_LOCATION);
    int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(CameraActivity.this,
            Manifest.permission.ACCESS_COARSE_LOCATION);


    if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

      // 2. 이미 퍼미션을 가지고 있다면
      // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


      // 3.  위치 값을 가져올 수 있음



    } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

      // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
      if (ActivityCompat.shouldShowRequestPermissionRationale(CameraActivity.this, PERMISSIONS[0])) {

        // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
        Toast.makeText(CameraActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
        // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
        ActivityCompat.requestPermissions(CameraActivity.this, PERMISSIONS,
                PERMISSIONS_REQUEST_CODE);


      } else {
        // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
        // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
        ActivityCompat.requestPermissions(CameraActivity.this, PERMISSIONS,
                PERMISSIONS_REQUEST_CODE);
      }

    }

  }


  public String getCurrentAddress( double latitude, double longitude) {

    //지오코더... GPS를 주소로 변환
    Geocoder geocoder = new Geocoder(this, Locale.getDefault());

    List<Address> addresses;

    try {

      addresses = geocoder.getFromLocation(
              latitude,
              longitude,
              7);
    } catch (IOException ioException) {
      //네트워크 문제
      Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
      return "지오코더 서비스 사용불가";
    } catch (IllegalArgumentException illegalArgumentException) {
      Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
      return "잘못된 GPS 좌표";

    }



    if (addresses == null || addresses.size() == 0) {
      Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
      return "주소 미발견";

    }

    Address address = addresses.get(0);
    return address.getAddressLine(0).toString()+"\n";

  }


  //여기부터는 GPS 활성화를 위한 메소드들
  private void showDialogForLocationServiceSetting() {

    AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
    builder.setTitle("위치 서비스 비활성화");
    builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
            + "위치 설정을 수정하실래요?");
    builder.setCancelable(true);
    builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        Intent callGPSSettingIntent
                = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
      }
    });
    builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
      }
    });
    builder.create().show();
  }


  @Override
  protected void onActivityResult(int permsRequestCode, int resultCode, Intent data) {
    super.onActivityResult(permsRequestCode, resultCode, data);

    switch (permsRequestCode) {

      case GPS_ENABLE_REQUEST_CODE:

        //사용자가 GPS 활성 시켰는지 검사
        if (checkLocationServicesStatus()) {
          if (checkLocationServicesStatus()) {

            Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
            checkRunTimePermission();
            return;
          }
        }

        break;
    }
  }

  public boolean checkLocationServicesStatus() {
    LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
  }
}