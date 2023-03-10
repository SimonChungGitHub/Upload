package com.simon.upload;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.simon.upload.model.FileModel;
import com.simon.upload.service.BrokenUploadService;
import com.simon.upload.utils.CreateThumbnail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class VideoActivity extends MainActivity implements View.OnClickListener {
    private SwipeRefreshLayout swipeRefreshLayout;
    private GridView gridView;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (snackbar != null) {
            snackbar.dismiss();
        }
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                startPoint.set((int) ev.getX(), (int) ev.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                float moveX = ev.getX() - startPoint.x;
                float moveY = ev.getY() - startPoint.y;
                boolean Horizontal = Math.abs(moveX) > Math.abs(moveY);
                if (Horizontal && moveX < -200) {
                    startActivity(new Intent(this, ImageActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
                    finish();
                    return true;
                } else if (Horizontal && moveX > 200) {
                    startActivity(new Intent(this, ImageActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, android.R.anim.fade_out);
                    finish();
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vedio);
        initView();
        currentActivity = Video_Activity;
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentActivity = Video_Activity;
        fileModelList = fileListModel.getVideoList();
        adapter = getAdapter();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            gridView.setNumColumns(columns);
        } else {
            gridView.setNumColumns(columns * 2);
        }
        gridView.setAdapter(adapter);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        ArrayList<FileModel> selectFiles = (ArrayList<FileModel>) fileModelList.stream().filter(FileModel::isSelected).collect(Collectors.toList());
        switch (view.getId()) {
            case R.id.video_button_delete:
                if (selectFiles.size() == 0) {
                    snackbar = Snackbar.make(thisLayout, "????????????", Snackbar.LENGTH_SHORT);
                    snackbar.show();
                    return;
                }
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setTitle("????????????")
                        .setMessage("??????????????????")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            selectFiles.forEach(o -> o.deleteVideo(this, o.getId()));
                            fileModelList = fileListModel.getVideoList();
                            adapter.notifyDataSetChanged();
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
                Dialog dialog = dialogBuilder.create();
                dialog.show();
                dialog.getWindow().setBackgroundDrawableResource(android.R.drawable.dialog_holo_light_frame);
                break;

            case R.id.video_button_upload:
                if (selectFiles.size() == 0) {
                    snackbar = Snackbar.make(thisLayout, "????????????", Snackbar.LENGTH_SHORT);
                    snackbar.show();
                    return;
                }
                if (BrokenUploadService.serviceProcessing) {
                    new AlertDialog.Builder(this).setMessage("???????????????, ?????????????????????????????????").show();
                    return;
                }
                fileModelList = (ArrayList<FileModel>) fileModelList.stream().filter(o -> !o.isSelected()).collect(Collectors.toList());
                adapter.notifyDataSetChanged();
                Intent intent = new Intent(this, BrokenUploadService.class);
                intent.putParcelableArrayListExtra("list", selectFiles);
                startForegroundService(intent);
                snackbar = Snackbar.make(thisLayout, "??????????????????", Snackbar.LENGTH_LONG);
                snackbar.show();
                break;
        }

    }

    private void initView() {
        thisLayout = findViewById(R.id.video);
        gridView = findViewById(R.id.video_gridview);
        gridView.setHorizontalSpacing(8);
        gridView.setVerticalSpacing(8);

        Button upload = findViewById(R.id.video_button_upload);
        Button delete = findViewById(R.id.video_button_delete);

        upload.setOnClickListener(this);
        delete.setOnClickListener(this);

        swipeRefreshLayout = findViewById(R.id.video_swipe);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!BrokenUploadService.serviceProcessing) {
                fileModelList = fileListModel.getVideoList();
                adapter.notifyDataSetChanged();
            }
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private BaseAdapter getAdapter() {
        return new BaseAdapter() {
            @Override
            public int getCount() {
                return fileModelList.size();
            }

            @Override
            public FileModel getItem(int position) {
                return fileModelList.get(position);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @SuppressLint({"ViewHolder", "SetTextI18n", "ClickableViewAccessibility"})
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final FileModel model = getItem(position);
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.image_picker_images, parent, false);

                ImageView imageView = convertView.findViewById(R.id.img_picker_image);
                TextView textView = convertView.findViewById(R.id.img_picker_filename);
                CheckBox checkBox = convertView.findViewById(R.id.img_picker_checkbox);

                CreateThumbnail thumbnail = new CreateThumbnail(getApplicationContext(), model);
                Bitmap bitmap = thumbnail.getRoundedCornerBitmap(thumbnail.getVideoBitmap(), 30);//

                imageView.setScaleType(ImageView.ScaleType.MATRIX);
                int screenLength = getResources().getDisplayMetrics().widthPixels;
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    screenLength = getResources().getDisplayMetrics().heightPixels;
                }

                float density = getResources().getDisplayMetrics().density;
                if (bitmap != null) {
                    int length = Math.min(bitmap.getWidth(), bitmap.getHeight());
                    float scale = (screenLength - 16 * density) / columns / length;
                    Matrix matrix = new Matrix();
                    matrix.postScale(scale, scale);
                    Bitmap b = Bitmap.createBitmap(bitmap, 0, 0, length, length, matrix, true);
                    imageView.setImageBitmap(b);
                }

                File file = Paths.get(model.getPath()).toFile();
                textView.setText(file.getName() + ", " + (file.length() / 1024) + "KB");
                checkBox.setChecked(model.isSelected());
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> model.setSelected(isChecked));
                convertView.setOnClickListener(v -> {
                    Log.e("aaa", "---------------");
                    Intent intent = new Intent(getApplicationContext(), VideoPlayActivity.class);
                    intent.putExtra("model", model);
                    startActivity(intent);

                });
                return convertView;
            }
        };
    }


}