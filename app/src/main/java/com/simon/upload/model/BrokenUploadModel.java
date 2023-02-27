package com.simon.upload.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.simon.upload.event.BlockIndexChangeEvent;
import com.simon.upload.listener.BlockIndexChangeListener;
import com.simon.upload.sqlite.BrokenUploadSqliteOpenHelper;

import java.io.File;

/**
 * Model for 檔案續傳服務
 * 提供檔案續傳資訊
 */
public class BrokenUploadModel {

    private final Context context;
    private final File file;
    private int blockSize = 8 * 1024 * 1024;
    private int blockIndex = 0;
    private byte[] cache;
    private BlockIndexChangeListener blockIndexChangeListener;

    public BrokenUploadModel(Context context, File file) {
        this.context = context;
        this.file = file;
        init();
    }

    private void init() {
        try (BrokenUploadSqliteOpenHelper obj = new BrokenUploadSqliteOpenHelper(context)) {
            SQLiteDatabase db = obj.getWritableDatabase();
            String sql = "select * from " + BrokenUploadSqliteOpenHelper._TableName + " where file=?";
            try (Cursor cursor = db.rawQuery(sql, new String[]{file.getName()})) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    blockSize = cursor.getInt(3);
                    blockIndex = cursor.getInt(4);
                } else {
                    ContentValues values = new ContentValues();
                    values.put("file", file.getName());
                    values.put("path", file.toString());
                    values.put("fileSize", file.length());
                    values.put("blockSize", blockSize);
                    values.put("blockIndex", blockIndex);
                    db.insert(BrokenUploadSqliteOpenHelper._TableName, null, values);
                }
            }
        }

    }

    public File getFile() {
        return file;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public byte[] getCache() {
        return cache;
    }

    public int getBlockCount() {
        int blockCount;
        if (file.length() % blockSize == 0) {
            blockCount = (int) (file.length() / blockSize);
        } else {
            blockCount = (int) (file.length() / blockSize) + 1;
        }
        return blockCount;
    }

    public void setBlockIndex(int blockIndex) {
        boolean different = blockIndex != this.blockIndex;
        this.blockIndex = blockIndex;
        if (different) applyBlockIndexChangeEvent(new BlockIndexChangeEvent(this));
    }

    public void setCache(byte[] cache) {
        this.cache = cache;
    }

    public void setOnBlockIndexChangeListener(BlockIndexChangeListener listener) {
        this.blockIndexChangeListener = listener;
    }

    private void applyBlockIndexChangeEvent(BlockIndexChangeEvent event) {
        this.blockIndexChangeListener.onBlockIndexChangeEvent(event);
    }

}
