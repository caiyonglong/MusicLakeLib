package com.cyl.musiclakelib;

import android.app.Application;

import com.music.lake.musiclib.MusicPlayerConfig;
import com.music.lake.musiclib.MusicPlayerManager;

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
        MusicPlayerManager.getInstance().init(this, config);
    }
}
