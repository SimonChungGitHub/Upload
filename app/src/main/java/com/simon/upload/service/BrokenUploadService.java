package com.simon.upload.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.simon.upload.R;
import com.simon.upload.model.BrokenUploadModel;
import com.simon.upload.model.FileModel;
import com.simon.upload.sqlite.BrokenUploadSqliteOpenHelper;
import com.simon.upload.utils.Constants;
import com.simon.upload.utils.DarkMode;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * 檔案續傳服務
 * 支援多檔案以同步方式上傳
 * 前台服務
 * 上傳期間不允許再 call startService()
 */
public class BrokenUploadService extends Service {

    private class NetworkConnectChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (networkConnected() && !isUploading) {
                new Thread(() -> startUpload(model)).start();
            }
        }
    }

    private class NotificationCancelReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            cancelUpload = true;
            remoteViews.setTextViewText(R.id.notification_upload_title, "上傳取消");
            remoteViews.setViewVisibility(R.id.notification_upload_content, View.GONE);
            remoteViews.setViewVisibility(R.id.notification_upload_progress, View.GONE);
            remoteViews.setViewVisibility(R.id.notification_upload_button, View.GONE);
            manager.notify(0, notification);
        }
    }

    public static boolean serviceProcessing = false;
    public static ArrayList<FileModel> uploadFileList = new ArrayList<>();
    private final DarkMode darkMode = new DarkMode(this);
    private NetworkConnectChangedReceiver netWorkStateReceiver;
    private NotificationCancelReceiver notificationCancelReceiver;
    private String url;
    private RemoteViews remoteViews;
    private NotificationManager manager;
    private Notification notification;
    private boolean cancelUpload = false;
    private BrokenUploadModel model;
    private int uploadFileCount = 0;
    private boolean isUploading = true;

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        url = preferences.getString("url", "http://192.168.0.238/okhttp/api/values/FileUpload").replace("FileUpload", "CacheFileUpload");
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (netWorkStateReceiver == null) {
            netWorkStateReceiver = new NetworkConnectChangedReceiver();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        registerReceiver(netWorkStateReceiver, filter);

        if (notificationCancelReceiver == null) {
            notificationCancelReceiver = new NotificationCancelReceiver();
        }
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("com.umc.camera.NOTIFICATION_BROADCAST");
        registerReceiver(notificationCancelReceiver, filter2);

        Intent intent = new Intent("com.umc.camera.NOTIFICATION_BROADCAST");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        remoteViews = new RemoteViews(getPackageName(), R.layout.notification_broken_upload_service);
        remoteViews.setTextViewText(R.id.notification_upload_button, getString(android.R.string.cancel));
        remoteViews.setOnClickPendingIntent(R.id.notification_upload_button, pendingIntent);

        notification = new NotificationCompat.Builder(this, Constants.notificationChannel)
                .setSmallIcon(R.drawable.ic_baseline_cloud_upload_24)
                .setWhen(System.currentTimeMillis())
                .setContent(remoteViews)
                .setAutoCancel(false)
                .build();

        if (darkMode.system()) {
            remoteViews.setTextColor(R.id.notification_upload_title, Color.WHITE);
            remoteViews.setTextColor(R.id.notification_upload_content, Color.WHITE);
            remoteViews.setTextColor(R.id.notification_upload_button, Color.WHITE);
        } else {
            remoteViews.setTextColor(R.id.notification_upload_title, Color.BLACK);
            remoteViews.setTextColor(R.id.notification_upload_content, Color.BLACK);
            remoteViews.setTextColor(R.id.notification_upload_button, Color.BLACK);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            manager.cancel(0);
            serviceProcessing = true;
            uploadFileList = intent.getParcelableArrayListExtra("list");
            new Thread(() -> {
                for (FileModel m : uploadFileList) {
                    if (cancelUpload) {
                        stopSelf();
                        return;
                    }
                    uploadFileCount++;
                    model = new BrokenUploadModel(this, Paths.get(m.getPath()).toFile());
                    if (model.getFile().exists()) {
                        startUpload(model);
                    }
                }
            }).start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(netWorkStateReceiver);
        unregisterReceiver(notificationCancelReceiver);
        this.netWorkStateReceiver = null;
        this.notificationCancelReceiver = null;
        serviceProcessing = false;
        uploadFileList.clear();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private boolean networkConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connMgr.getAllNetworks();
        for (Network network : networks) {
            NetworkInfo info = connMgr.getNetworkInfo(network);
            if (info.isConnected()) {
                return true;
            }
        }
        return false;
    }

    protected void startUpload(BrokenUploadModel model) {
        isUploading = true;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(model.getFile(), "r");
             BrokenUploadSqliteOpenHelper obj = new BrokenUploadSqliteOpenHelper(this)) {
            SQLiteDatabase db = obj.getWritableDatabase();
            boolean lastBlockCount = false;
            for (int i = model.getBlockIndex(); i < model.getBlockCount(); i++) {
                model.setBlockIndex(i);
                if (cancelUpload) {
                    stopSelf();
                    return;
                }
                if (model.getBlockIndex() == model.getBlockCount() - 1) lastBlockCount = true;
                remoteViews.setTextViewText(R.id.notification_upload_title, "正在上傳 " + uploadFileCount + "/" + uploadFileList.size());
                remoteViews.setTextViewText(R.id.notification_upload_content, model.getFile().getName() + " / " + model.getFile().length() / 1024 / 1024 + "MB");
                remoteViews.setProgressBar(R.id.notification_upload_progress, model.getBlockCount(), model.getBlockIndex() + 1, false);
                startForeground(Constants.notificationUploadId, notification);
                ContentValues values = new ContentValues();
                values.put("blockIndex", model.getBlockIndex());
                db.update(BrokenUploadSqliteOpenHelper._TableName, values, "file=?", new String[]{model.getFile().getName()});

                //該讀取方式請勿變更, 避免上傳後檔案無法使用
                long offset = (long) model.getBlockIndex() * model.getBlockSize();
                randomAccessFile.seek(offset);
                byte[] cache = new byte[model.getBlockSize()];
                int length = randomAccessFile.read(cache);
                if (length < model.getBlockSize()) {
                    cache = Arrays.copyOf(cache, length);
                }
                model.setCache(cache);

                HashMap<String, String> map = httpConnection(model);
                if (map.isEmpty()) {
                    remoteViews.setTextViewText(R.id.notification_upload_title, "上傳停止, 等待恢復網路連線後繼續上傳");
                    startForeground(Constants.notificationUploadId, notification);
                    break;
                } else {
                    int responseCode = Integer.parseInt(Objects.requireNonNull(map.get("responseCode")));
                    boolean result = Boolean.parseBoolean(map.get("result"));
                    if (responseCode != 200) {
                        remoteViews.setTextViewText(R.id.notification_upload_title, "主機異常, code " + responseCode);
                        manager.notify(0, notification);
                        stopSelf();
                        break;
                    } else {
                        if (result) {
                            if (lastBlockCount) {
                                Files.deleteIfExists(model.getFile().toPath());
                                db.delete(BrokenUploadSqliteOpenHelper._TableName, "file=?", new String[]{model.getFile().getName()});
                                if (uploadFileCount == uploadFileList.size()) {
                                    sendBroadcast(new Intent("com.umc.camera.BrokenUpload"));
                                    remoteViews.setTextViewText(R.id.notification_upload_title, "上傳完成");
                                    remoteViews.setViewVisibility(R.id.notification_upload_content, View.GONE);
                                    remoteViews.setViewVisibility(R.id.notification_upload_progress, View.GONE);
                                    remoteViews.setViewVisibility(R.id.notification_upload_button, View.GONE);
                                    manager.notify(0, notification);
                                    stopSelf();
                                }
                                break;
                            }
                        } else {
                            remoteViews.setTextViewText(R.id.notification_upload_title, "上傳失敗, web api return false");
                            manager.notify(0, notification);
                            stopSelf();
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e("startUpload", e.toString());
        } finally {
            isUploading = false;
        }
    }

    private HashMap<String, String> httpConnection(@NonNull BrokenUploadModel fileInfo) {
        HashMap<String, String> map = new HashMap<>();
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.MINUTES)
                    .readTimeout(5, TimeUnit.MINUTES)
                    .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                    .build();

            RequestBody postBody = RequestBody.create(MediaType.parse("application/octet-stream"), fileInfo.getCache());
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("dept", "temp")
                    .addFormDataPart("blockSize", String.valueOf(fileInfo.getBlockSize()))
                    .addFormDataPart("blockIndex", String.valueOf(fileInfo.getBlockIndex()))
                    .addFormDataPart("fileSize", String.valueOf(fileInfo.getFile().length()))
                    .addFormDataPart("file", fileInfo.getFile().getName(), postBody)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String result = Objects.requireNonNull(response.body()).string();
                map.put("responseCode", String.valueOf(response.code()));
                map.put("result", result);
            }
        } catch (IOException e) {
            Log.e("httpConnection", e.toString());
        }
        return map;
    }


}