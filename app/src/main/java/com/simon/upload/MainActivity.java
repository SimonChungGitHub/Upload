package com.simon.upload;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.simon.upload.model.FileListModel;
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
                fileModelList = fileListModel.getImageList();
                adapter.notifyDataSetChanged();
            } else if (currentActivity == Video_Activity) {
                fileModelList = fileListModel.getVideoList();
                adapter.notifyDataSetChanged();
            }
        }
    }

    protected final int Image_Activity = 0;
    protected final int Video_Activity = 1;
    protected int currentActivity;
    private BrokenUploadBroadcast brokenUploadBroadcast = new BrokenUploadBroadcast();
    protected SharedPreferences preferences;
    protected int columns = 3;
    protected PointF startPoint = new PointF();
    protected ConstraintLayout thisLayout;
    protected Snackbar snackbar;
    protected FileListModel fileListModel = new FileListModel(this);
    protected ArrayList<FileModel> fileModelList;
    protected BaseAdapter adapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // shortcut
        if (getIntent().getAction() != null && getIntent().getCategories().contains("android.shortcut.camera")) {
            startCameraActivity();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE}, Constants.PERMISSION_WRITE_EXTERNAL_STORAGE);
                return;
            }
        }

        NotificationChannel channel = new NotificationChannel(Constants.notificationChannel, "BrokenUploadService 通知通道", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
        registerReceiver(brokenUploadBroadcast, new IntentFilter("com.umc.camera.BrokenUpload"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        columns = Integer.parseInt(preferences.getString("picture_display_qty", "3"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(brokenUploadBroadcast);
        brokenUploadBroadcast = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_setting) {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
        } else if (id == R.id.action_camera) {
            startCameraActivity();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.PERMISSION_WRITE_EXTERNAL_STORAGE) {
            if ((grantResults.length > 0) && (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.uninstall)
                        .setMessage(R.string.are_you_sure)
                        .setNegativeButton(R.string.yes, (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", getPackageName(), null));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        })
                        .setPositiveButton(R.string.no, (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                Constants.PERMISSION_WRITE_EXTERNAL_STORAGE))
                        .show();
            }
        }

        if (requestCode == Constants.PERMISSION_CAMERA) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, Constants.PERMISSION_CAMERA);
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.stop_camera_function)
                            .setMessage("此APP有段時間未使用,系統已關閉相機權限,請重新開啟相機權限!!")
                            .setNegativeButton(R.string.close, (dialog, which) -> dialog.cancel())
                            .show();
                }
            } else {
                startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE));
            }
        }
    }

    protected void startCameraActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, Constants.PERMISSION_CAMERA);
        } else {
            startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE));
            overridePendingTransition(R.anim.slide_in_bottom, android.R.anim.fade_out);
        }
    }

}