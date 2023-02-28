package com.simon.upload.utils;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;

/**
 * system 及 app 深色模式判斷, supporting dark-mode on target android 10 or above
 */
public class DarkMode {
    private final Context context;

    public DarkMode(Context context) {
        this.context = context;
    }

    /**
     * 判斷系統顯示畫面是 深色模式 or 淺色模式
     */
    public boolean system() {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES;
    }

    /**
     * 判斷 app 顯示畫面是 深色模式 or 淺色模式
     */
    public boolean app() {
        int uiMode = context.getResources().getConfiguration().uiMode;
        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

}
