package com.simon.upload.model;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.simon.upload.service.BrokenUploadService;

import java.util.ArrayList;

/**
 * Model for 各 Activity 畫面顯示的圖片 or 影片 file
 */
public class FileModel implements Parcelable {
    private Context context;
    private long id;
    private String path;
    private boolean selected = false;

    public FileModel(Context context) {
        this.context = context;
    }

    protected FileModel(Parcel in) {
        id = in.readLong();
        path = in.readString();
        selected = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(path);
        dest.writeByte((byte) (selected ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FileModel> CREATOR = new Creator<FileModel>() {
        @Override
        public FileModel createFromParcel(Parcel in) {
            return new FileModel(in);
        }

        @Override
        public FileModel[] newArray(int size) {
            return new FileModel[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * 從 android media store 資料庫取得所有 video 資訊清單, 並加入 file model list
     */
    public ArrayList<FileModel> getVideoModelList() {
        ArrayList<FileModel> list = new ArrayList<>();
        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        Cursor cursor = context.getContentResolver().query(collection,
                new String[]{MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA, MediaStore.Video.Media.DATE_ADDED},
                null, null, MediaStore.Video.Media.DATE_ADDED + " ASC");
        while (cursor.moveToNext()) {
            FileModel model = new FileModel(context);
            model.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)));
            model.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)));
            list.add(model);
        }
        cursor.close();
        return removeUploadingFileModel(list);
    }

    /**
     * 刪除 android media store 資料庫 video row
     */
    public int deleteVideo(long id) {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            uri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        String where = MediaStore.Video.Media._ID + "=?";
        return context.getContentResolver().delete(uri, where, new String[]{String.valueOf(id)});
    }

    /**
     * 從 android media store 資料庫取得所有 image 資訊清單, 並加入 file model list
     */
    public ArrayList<FileModel> getImageModelList() {
        ArrayList<FileModel> list = new ArrayList<>();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        Cursor cursor = context.getContentResolver().query(collection,
                new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED},
                null, null, MediaStore.Images.Media.DATE_ADDED + " ASC");
        while (cursor.moveToNext()) {
            FileModel model = new FileModel(context);
            model.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)));
            model.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)));
            list.add(model);
        }
        cursor.close();
        return removeUploadingFileModel(list);
    }

    /**
     * 刪除 android media store 資料庫 image row
     */
    public int deleteImage(long id) {
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        String where = MediaStore.Images.Media._ID + "=?";
        return context.getContentResolver().delete(uri, where, new String[]{String.valueOf(id)});
    }

    /**
     * 顯示清單 remove 正在上傳的清單
     * */
    private ArrayList<FileModel> removeUploadingFileModel(ArrayList<FileModel> src) {
        ArrayList<FileModel> list = new ArrayList<>();
        for (FileModel model : src) {
            int count = (int) BrokenUploadService.uploadFileList.stream().filter(o -> o.getId() == model.getId()).count();
            if (count==0) {
                list.add(model);
            }
        }
        return list;
    }

}
