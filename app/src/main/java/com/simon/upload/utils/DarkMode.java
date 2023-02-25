package com.simon.upload.utils;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;

/**
 * 深色模式判斷, 分為 system 及 app, supporting dark-mode on target android 10 or above
 */
public class DarkMode {
    private final Context context;

    public DarkMode(Context context) {
        this.context = context;
    }

    public boolean system() {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES;
    }

    public boolean app() {
        int uiMode = context.getResources().getConfiguration().uiMode;
        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

}
