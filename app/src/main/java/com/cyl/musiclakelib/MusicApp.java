package com.cyl.musiclakelib;

import android.app.Application;
import android.util.Log;

import com.music.lake.musiclib.MusicPlayerConfig;
import com.music.lake.musiclib.MusicPlayerManager;
import com.music.lake.musiclib.listener.BindServiceCallBack;

public class MusicApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initMusicPlayerService();
    }

    private void initMusicPlayerService() {
        MusicPlayerConfig config = new MusicPlayerConfig.Builder()
                .setUseExoPlayer(true)
                .create();
        MusicPlayerManager.Companion.getInstance().init(this, config);
    }
}
