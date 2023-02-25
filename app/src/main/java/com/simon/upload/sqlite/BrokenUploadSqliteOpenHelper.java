package com.simon.upload.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class BrokenUploadSqliteOpenHelper extends SQLiteOpenHelper {

    private final static int _DBVersion = 1;
    private final static String _DBName = "BrokenUpload.db";
    public final static String _TableName = "FileInfo";

    public BrokenUploadSqliteOpenHelper(@Nullable Context context) {
        super(context, _DBName, null, _DBVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL = "CREATE TABLE IF NOT EXISTS " + _TableName + "( " +
                "file TEXT PRIMARY KEY," +
                "path TEXT," +
                "fileSize LONG," +
                "blockSize INTEGER," +
                "blockIndex INTEGER" +
                ");";
        sqLiteDatabase.execSQL(SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        final String SQL = "DROP TABLE " + _TableName;
        sqLiteDatabase.execSQL(SQL);
    }
}