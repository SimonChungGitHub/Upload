package com.simon.upload.model;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.simon.upload.service.BrokenUploadService;

import java.util.ArrayList;

public class FileListModel {
    private final Context context;
    private ArrayList<FileModel> imageList;
    private ArrayList<FileModel> videoList;

    public FileListModel(Context context) {
        this.context = context;
    }

    /**
     * 從 android media store 資料庫取得所有 image 資訊清單, 並加入 file model list
     */
    public ArrayList<FileModel> getImageList() {
        ArrayList<FileModel> list = new ArrayList<>();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        Cursor cursor = context.getContentResolver().query(collection,
                new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED},
                null, null, MediaStore.Images.Media.DATE_ADDED + " ASC");
        while (cursor.moveToNext()) {
            FileModel model = new FileModel();
            model.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)));
            model.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)));
            list.add(model);
        }
        cursor.close();
        imageList = removeUploadingFileModel(list);
        return imageList;
    }

    /**
     * 從 android media store 資料庫取得所有 video 資訊清單, 並加入 file model list
     */
    public ArrayList<FileModel> getVideoList() {
        ArrayList<FileModel> list = new ArrayList<>();
        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        Cursor cursor = context.getContentResolver().query(collection,
                new String[]{MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA, MediaStore.Video.Media.DATE_ADDED},
                null, null, MediaStore.Video.Media.DATE_ADDED + " ASC");
        while (cursor.moveToNext()) {
            FileModel model = new FileModel();
            model.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)));
            model.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)));
            list.add(model);
        }
        cursor.close();
        videoList = removeUploadingFileModel(list);
        return videoList;
    }

    /**
     * 顯示清單 remove 正在上傳的清單
     */
    private ArrayList<FileModel> removeUploadingFileModel(ArrayList<FileModel> src) {
        ArrayList<FileModel> list = new ArrayList<>();
        for (FileModel model : src) {
            int count = (int) BrokenUploadService.uploadFileList.stream().filter(o -> o.getId() == model.getId()).count();
            if (count == 0) {
                list.add(model);
            }
        }
        return list;
    }

    public void setImageList(ArrayList<FileModel> imageList) {
        this.imageList = imageList;
    }

    public void setVideoList(ArrayList<FileModel> videoList) {
        this.videoList = videoList;
    }
}
