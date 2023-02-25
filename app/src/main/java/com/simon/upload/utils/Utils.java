package com.simon.upload.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.exifinterface.media.ExifInterface;

import com.simon.upload.R;

import java.io.IOException;
import java.nio.file.Path;

public class Utils {
    public static boolean loginPass = false;
    public static long loginTimeReset;
    public static String user = "z13422";

    public static void login(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_login, null);
        EditText loginUser = view.findViewById(R.id.login_input_user);
        EditText loginPassword = view.findViewById(R.id.login_input_password);
        TextView loginMessage = view.findViewById(R.id.login_message);
        loginMessage.setText("請輸入帳號密碼");
        Button login = view.findViewById(R.id.login_button);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .show();

        login.setOnClickListener(v -> {
            String username = loginUser.getText().toString();
            String password = loginPassword.getText().toString();
            if (username.equals("simon") && password.equals("1212")) {
                loginPass = true;
                loginTimeReset = System.currentTimeMillis();
                dialog.dismiss();
            } else {
                loginPass = false;
                loginMessage.setText("登入失敗");
            }
        });
    }

    public static boolean notAllowFileAccessPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    public static int getDegree(Path path) {
        int degree = 0;
        try {
            ExifInterface exif = new ExifInterface(path.toFile());
            int num = exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);
            switch (num) {
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

}
