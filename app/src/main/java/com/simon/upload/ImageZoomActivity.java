package com.simon.upload;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.material.snackbar.Snackbar;
import com.simon.upload.model.FileModel;
import com.simon.upload.utils.CountingRequestBody;
import com.simon.upload.utils.RemoveICCProfile;
import com.simon.upload.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImageZoomActivity extends MainActivity {
    private TextView count;
    private ImageView imageView;

    private View rootView;
    private PopupWindow popupWindow;

    private int index = -1;
    private final Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private final PointF mid = new PointF();
    private float dist = 1f;
    private boolean isZOOM = false;


    private final Handler handler = new Handler();
    private AlertDialog dialog;


    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(View.SYSTEM_UI_FLAG_FULLSCREEN);
        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_image_zoom);
        initView();
        fileModelList = fileModel.getImageModelList();
        Intent intent = getIntent();
        index = intent.getIntExtra("index", -1);
        if (savedInstanceState != null) {
            index = savedInstanceState.getInt("INDEX");
        }

        if (index != -1) {
            FileModel model = fileModelList.get(index);
            Bitmap bitmap1 = getRotatedBitmap(Paths.get(model.getPath()));
            setScaleTranslate(bitmap1);
            imageView.setImageBitmap(bitmap1);
            imageView.setImageMatrix(matrix);
            count.setText(index + 1 + "/" + fileModelList.size());
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_out, R.anim.scale_down);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("INDEX", index);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        index = savedInstanceState.getInt("INDEX");
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    private void initView() {
        thisLayout = findViewById(R.id.image_zoom);
        count = findViewById(R.id.count);
        imageView = findViewById(R.id.imageView);
        imageView.setOnTouchListener((view, motionEvent) -> {
            ImageView v = (ImageView) view;
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    startPoint.set((int) motionEvent.getX(), (int) motionEvent.getY());
                    mode = DRAG;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        matrix.set(savedMatrix);
                        float moveX = motionEvent.getX() - startPoint.x;
                        float moveY = motionEvent.getY() - startPoint.y;
                        boolean Vertical = Math.abs(moveX) <= Math.abs(moveY);

                        if (isZOOM) {
                            matrix.postTranslate(moveX, moveY);
                        } else if (Vertical && moveY > 0) {
                            matrix.postTranslate(0, moveY);
                        }
                    }
                    if (mode == ZOOM) {
                        float newDist = spacing(motionEvent);
                        if (newDist > 10f) {
                            matrix.set(savedMatrix);
                            float tScale = newDist / dist;
                            matrix.postScale(tScale, tScale, mid.x, mid.y);
                            isZOOM = true;
                        }
                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    dist = spacing(motionEvent);
                    if (dist > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(mid, motionEvent);
                        mode = ZOOM;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    float moveX = motionEvent.getX() - startPoint.x;
                    float moveY = motionEvent.getY() - startPoint.y;
                    boolean Horizontal = Math.abs(moveX) > Math.abs(moveY);
                    boolean Vertical = Math.abs(moveX) <= Math.abs(moveY);
                    if (mode == DRAG && !isZOOM) {
                        if (Vertical && moveY < -30) {
                            if (popupWindow == null) {
                                showPopup();
                            } else {
                                popupWindow.showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
                            }
                        } else if (moveX < -30 && Horizontal) {
                            index++;
                            if (index == fileModelList.size()) {
                                index--;
                            } else {
                                v.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
                            }
                        } else if (moveX > 30 && Horizontal) {
                            index--;
                            if (index < 0) {
                                index = 0;
                            } else {
                                v.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
                            }
                        } else if (moveY > 30 && Vertical) {
                            finish();
                            overridePendingTransition(R.anim.fade_out, R.anim.scale_down);
                        }
                        if (index >= 0 && index < fileModelList.size()) {
                            FileModel model = fileModelList.get(index);
                            Bitmap bitmap1 = getRotatedBitmap(Paths.get(model.getPath()));
                            setScaleTranslate(bitmap1);
                            v.setImageBitmap(bitmap1);
                            count.setText(index + 1 + "/" + fileModelList.size());
                        }
                    } else if (mode == ZOOM) {
                        float space = spacing(motionEvent);
                        if (space < dist / 2) {
                            isZOOM = false;
                        } else if (space > dist / 2 && space < dist) {
                            if (index >= 0 && index < fileModelList.size()) {
                                FileModel model = fileModelList.get(index);
                                Bitmap bitmap1 = getRotatedBitmap(Paths.get(model.getPath()));
                                setScaleTranslate(bitmap1);
                                v.setImageBitmap(bitmap1);
                                count.setText(index + 1 + "/" + fileModelList.size());
                            }
                        }
                    }
                    mode = NONE;
                    break;
            }
            v.setImageMatrix(matrix);
            return true;
        });
    }

    private void setScaleTranslate(Bitmap bitmap) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float scale = Math.min((float) dm.widthPixels / bitmap.getWidth(), (float) dm.heightPixels / bitmap.getHeight());
        matrix.setScale(scale, scale);
        PointF offset = center(bitmap, scale);
        matrix.postTranslate(offset.x, offset.y);
    }

    private PointF center(Bitmap bitmap, float scale) {
        try {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            float dx = (screenWidth - bitmap.getWidth() * scale) / 2f;
            float dy = (screenHeight - bitmap.getHeight() * scale) / 3f;
            return new PointF(dx, dy);
        } catch (Exception e) {
            return new PointF(0, 0);
        }
    }

    private Bitmap getRotatedBitmap(Path path) {
        Bitmap bitmap = getScaledBitmap(path.toString());
        float degree = Utils.getDegree(path);
        Matrix matrix = new Matrix();
        matrix.setRotate(degree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    private Bitmap getScaledBitmap(String imagePath) {
        // 取得原始圖檔的大小
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; //只抓長度和寬度
        BitmapFactory.decodeFile(imagePath, options);
        int width = options.outWidth;
        // 新圖的寬要小於等於這個值
        final int MAX_WIDTH = 1024;
        // 求出要縮小的 scale 值，必需是2的次方，ex: 1,2,4,8,16...
        int scale = 1;
        while (width > MAX_WIDTH) {
            width /= 2;
            scale *= 2;
        }
        // 使用 scale 值產生縮小的圖檔
        BitmapFactory.Options scaledOptions = new BitmapFactory.Options();
        scaledOptions.inSampleSize = scale;
        return BitmapFactory.decodeFile(imagePath, scaledOptions);
    }

    // 兩點的距離
    private float spacing(MotionEvent event) {
        try {
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(x * x + y * y);
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    // 兩點的中點
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @SuppressLint({"SetTextI18n", "InflateParams"})
    private void showPopup() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popupwindow_layout, null);
        popupWindow = new PopupWindow(this);
        popupWindow.setContentView(popupView);
        popupWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setAnimationStyle(R.style.PopupWindow);
        popupWindow.setOutsideTouchable(true);
        rootView = LayoutInflater.from(this).inflate(R.layout.activity_image_zoom, null);
        popupWindow.showAtLocation(rootView, Gravity.BOTTOM, 0, 0);

        Button btnDelete = popupView.findViewById(R.id.delete);
        btnDelete.setOnClickListener(view12 -> {
            popupWindow.dismiss();
            FileModel model = fileModelList.get(index);
            fileModel.deleteImage(model.getId());
            fileModelList = fileModel.getImageModelList();
            if (fileModelList.size() == 0) {
                finish();
                return;
            } else if (index == fileModelList.size()) {
                index--;
            }

            if (index >= 0 && index < fileModelList.size()) {
                FileModel model2 = fileModelList.get(index);
                Bitmap bitmap1 = getRotatedBitmap(Paths.get(model2.getPath()));
                setScaleTranslate(bitmap1);
                imageView.setImageBitmap(bitmap1);
                imageView.setImageMatrix(matrix);
                imageView.setAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_right));
                count.setText(index + 1 + "/" + fileModelList.size());
            }
        });

        Button btnUpload = popupView.findViewById(R.id.upload);
        btnUpload.setOnClickListener(v -> {
            popupWindow.dismiss();
            if (internetNotFound()) {
                Snackbar.make(thisLayout, "no internet", Snackbar.LENGTH_SHORT).show();
                return;
            }


//            if (BrokenUploadService.serviceProcessing) {
//                Snackbar.make(thisLayout, "service 正在執行上傳任務, 請稍後", Snackbar.LENGTH_SHORT).show();
//                return;
//            }
//            FileModel model = fileModelList.get(index);
//            ArrayList<FileModel> list = new ArrayList<>();
//            list.add(model);
//            Intent intent = new Intent(this, BrokenUploadService.class);
//            intent.putExtra("uploadType", "zoom");
//            intent.putParcelableArrayListExtra("list", list);
//            startForegroundService(intent);


            new Thread(() -> {
                FileModel model = fileModelList.get(index);
                ArrayList<FileModel> list = new ArrayList<>();
                list.add(model);
                startUpload(list);
            }).start();
        });
    }

    @SuppressLint("SetTextI18n")
    private void startUpload(ArrayList<FileModel> list) {
        int totalCount = list.size();

        String Ready = "Ready... ";
        String Uploading = "Uploading... ";

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPaddingRelative(20, 5, 20, 0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(linearLayout)
                .setCancelable(false);
        TextView uploading = new TextView(this);
        uploading.setText("Uploading");
        uploading.setTextColor(Color.RED);
        uploading.setTextSize(20f);
        ProgressBar progressbar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressbar.setProgress(0);
        progressbar.setMax(totalCount);



        handler.post(() -> {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            uploading.setText(Ready + progressbar.getProgress() + "/" + totalCount);
            linearLayout.addView(uploading);
            linearLayout.addView(progressbar);
            dialog = builder.create();
            dialog.show();
        });

        long start = new Date().getTime();
        for (FileModel model : list) {
            handler.post(() -> {
                progressbar.incrementProgressBy(1);
                uploading.setText(Ready + progressbar.getProgress() + "/" + totalCount);
            });

            //todo image加上浮水印
            new RemoveICCProfile(Paths.get(model.getPath()));
            uploading.setText(Ready + progressbar.getProgress() + "/" + totalCount);
        }

        handler.post(() -> {
            linearLayout.removeView(progressbar);
            progressbar.setProgress(0);
            progressbar.setMax(totalCount);
            uploading.setText(Uploading + progressbar.getProgress() + "/" + totalCount);
        });

        for (FileModel model : list) {
            ProgressBar subProgressbar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            subProgressbar.setProgress(0);
            handler.post(() -> linearLayout.addView(subProgressbar));

            File file = Paths.get(model.getPath()).toFile();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("dept", "temp")
                    .addFormDataPart("image", file.getName(),
                            RequestBody.create(MediaType.parse("image/jpeg"), file))
                    .build();

            final CountingRequestBody.Listener progressListener = (bytesRead, contentLength) -> {
                final int progress = (int) (((double) bytesRead / contentLength) * 100);
                handler.post(() -> subProgressbar.setProgress(progress));
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.MINUTES)
                    .readTimeout(5, TimeUnit.MINUTES)
                    .addNetworkInterceptor(chain -> {
                        Request originalRequest = chain.request();
                        if (originalRequest.body() == null) {
                            return chain.proceed(originalRequest);
                        }
                        Request progressRequest = originalRequest.newBuilder()
                                .method(originalRequest.method(),
                                        new CountingRequestBody(originalRequest.body(), progressListener))
                                .build();
                        return chain.proceed(progressRequest);
                    })
                    .build();
            String url = preferences.getString("url", "");
            Request request = new Request.Builder()
                    .header("Content-Type", "multipart/form-data")
                    .url(url)
                    .post(requestBody)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 204) {
                    //todo delete uploaded file
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            handler.post(() -> {
                linearLayout.removeView(subProgressbar);
                progressbar.incrementProgressBy(1);
                uploading.setText(Uploading + progressbar.getProgress() + "/" + totalCount);
            });
        }

        Log.e("aaa", (new Date().getTime() - start) + "---");
        handler.post(() -> {
            dialog.dismiss();
            linearLayout.removeAllViews();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            fileModelList = fileModel.getImageModelList();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
//            snackbar = Snackbar.make(thisLayout, "成功 " + successCount + "    失敗 " + (totalCount - successCount), Snackbar.LENGTH_INDEFINITE);
//            snackbar.show();
        });


    }


}