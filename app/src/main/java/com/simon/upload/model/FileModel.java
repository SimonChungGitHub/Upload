package com.simon.upload.model;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

/**
 * Model for 各 Activity 畫面顯示的圖片 or 影片 file
 */
public class FileModel implements Parcelable {
    private long id;
    private String path;
    private boolean selected = false;

    public FileModel() {
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
     * 刪除 android media store 資料庫 video row
     */
    public int deleteVideo(Context context, long id) {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            uri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        String where = MediaStore.Video.Media._ID + "=?";
        return context.getContentResolver().delete(uri, where, new String[]{String.valueOf(id)});
    }

    /**
     * 刪除 android media store 資料庫 image row
     */
    public int deleteImage(Context context, long id) {
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        String where = MediaStore.Images.Media._ID + "=?";
        return context.getContentResolver().delete(uri, where, new String[]{String.valueOf(id)});
    }


}
