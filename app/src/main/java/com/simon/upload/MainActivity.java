package com.simon.upload;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.simon.upload.model.FileModel;
import com.simon.upload.service.BrokenUploadService;
import com.simon.upload.utils.Constants;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private class BrokenUploadBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            BrokenUploadService.uploadFileList.clear();
            if (currentActivity == Image_Activity) {
                fileModelList = fileModel.getImageModelList();
                adapter.notifyDataSetChanged();
            } else if (currentActivity == Video_Activity) {
                fileModelList = fileModel.getVideoModelList();
                adapter.notifyDataSetChanged();
            }
        }
    }

    protected SharedPreferences preferences;
    protected int columns = 2;
    protected String url;
    protected PointF startPoint = new PointF();

    protected ConstraintLayout thisLayout;
    protected Snackbar snackbar;

    /**
     * ----------------------------------------
     */
    protected FileModel fileModel = new FileModel(this);
    protected ArrayList<FileModel> fileModelList;
    protected BaseAdapter adapter;
    private BrokenUploadBroadcast brokenUploadBroadcast = new BrokenUploadBroadcast();

    protected int Image_Activity = 0;
    protected int Video_Activity = 1;
    protected int currentActivity;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialParam();

        if (getIntent().getAction() != null && preferences.getBoolean("start_into_setting", false)) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (getIntent().getAction() != null && getIntent().getCategories().contains("android.shortcut.camera")) {
            openCamera();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE}, Constants.PERMISSION_WRITE_EXTERNAL_STORAGE);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !Environment.isExternalStorageManager()) {
            openSetting(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
        }

        NotificationChannel channel = new NotificationChannel(Constants.notificationChannel, "BrokenUploadService 通知通道", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        registerReceiver(brokenUploadBroadcast, new IntentFilter("com.umc.camera.BrokenUpload"));

    }

    private void initialParam() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        columns = Integer.parseInt(preferences.getString("picture_display_qty", "2"));
        url = preferences.getString("url", "");
    }

    @Override
    protected void onResume() {
        super.onResume();
        initialParam();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(brokenUploadBroadcast);
        brokenUploadBroadcast = null;
    }

    protected boolean isScreenOrientationPortrait() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_setting) {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
        } else if (id == R.id.action_camera) {
            openCamera();
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Activity activity = this;
        if (requestCode == Constants.PERMISSION_WRITE_EXTERNAL_STORAGE) {
            if ((grantResults.length > 0) && (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.uninstall)
                        .setMessage(R.string.are_you_sure)
                        .setNegativeButton(R.string.yes, (dialog, which) -> openSetting(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
                        .setPositiveButton(R.string.no, (dialog, which) -> ActivityCompat.requestPermissions(activity,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                Constants.PERMISSION_WRITE_EXTERNAL_STORAGE))
                        .show();
            }
        }
        if (requestCode == Constants.PERMISSION_CAMERA) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    Log.e("shouldShowRequestPermissionRationale", "true");
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, Constants.PERMISSION_CAMERA);
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.stop_camera_function)
                            .setMessage("此APP有段時間未使用,系統已關閉相機權限,請重新開啟相機權限!!")
                            .setNegativeButton(R.string.close, (dialog, which) -> dialog.cancel())
                            .show();
                }
            } else {
                //galaxy tab A8-x200 開啟camera閃退
//                Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                //修正galaxy tab A8-x200 開啟camera閃退
                Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
                startActivity(intent);
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void openSetting(String settingName) {
        Intent intent;
        switch (settingName) {
            case Settings.ACTION_APPLICATION_DETAILS_SETTINGS:
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null));
                break;
            case Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION:
                intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + settingName);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    protected void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, Constants.PERMISSION_CAMERA);
        } else {
//            Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_bottom, android.R.anim.fade_out);
        }
    }

    protected boolean internetNotFound() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            return true;
        } else {
            return !info.isAvailable();
        }
    }





}