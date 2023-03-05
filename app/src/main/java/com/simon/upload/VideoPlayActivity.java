package com.simon.upload;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.simon.upload.model.FileModel;

import java.util.Objects;

public class VideoPlayActivity extends AppCompatActivity {
    private FileModel model;
    private VideoView videoView;
    private int currentPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(View.SYSTEM_UI_FLAG_FULLSCREEN);
        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_video_play);
        videoView = findViewById(R.id.videoView);

        if (savedInstanceState != null) {
            model = savedInstanceState.getParcelable("model");
            videoView.setVideoPath(model.getPath());
            currentPosition = savedInstanceState.getInt("position");
        } else {
            model = getIntent().getParcelableExtra("model");
            videoView.setVideoPath(model.getPath());
        }

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.seekTo(currentPosition);
        videoView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        currentPosition = videoView.getCurrentPosition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        videoView.pause();
        super.onSaveInstanceState(outState);
        outState.putParcelable("model", model);
        outState.putInt("position", currentPosition);
    }


}