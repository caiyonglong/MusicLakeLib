package com.music.lake.musiclib;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.music.lake.musiclib.videocache.HttpProxyCacheServer;
import com.music.lake.musiclib.cache.CacheFileNameGenerator;
import com.music.lake.musiclib.listener.BindServiceCallBack;
import com.music.lake.musiclib.listener.MusicUrlRequest;
import com.music.lake.musiclib.player.PlayerControl;
import com.music.lake.musiclib.service.MusicServiceBinder;
import com.music.lake.musiclib.utils.MusicLibLog;

import java.io.File;

/**
 * Created by D22434 on 2017/9/20.
 */

public class MusicPlayerManager implements Application.ActivityLifecycleCallbacks {
    private final String TAG = "MusicPlayerManager";
    private MusicServiceBinder mBinder = null;
    private MusicPlayerConfig config;
    private Application application;
    private MusicUrlRequest request;
    private PlayerControl playerControl;
    private Boolean useExoPlayer;

    private volatile static MusicPlayerManager manager;

    private MusicPlayerManager() {
    }

    public static MusicPlayerManager getInstance() {
        if (manager == null) {
            synchronized (MusicPlayerManager.class) {
                if (manager == null) {
                    manager = new MusicPlayerManager();
                }
            }
        }
        return manager;
    }

    public void init(Application application, MusicPlayerConfig config) {
        this.application = application;
        this.request = config.request;
        this.useExoPlayer = config.useExoPlayer;
        playerControl = new PlayerControl(application);
        application.registerActivityLifecycleCallbacks(this);
    }

    public Context getAppContext() {
        return application.getApplicationContext();
    }

    public void initialize(Context context, BindServiceCallBack callBack) {
    }

    public void unInitialize() {
    }

    public final boolean isPlaybackServiceConnected() {
        return mBinder != null;
    }

    public Boolean isUseExoPlayer() {
        return useExoPlayer;
    }

    public static PlayerControl getControl() {
        return getInstance().playerControl;
    }

    /**
     * AndroidVideoCache缓存设置
     */
    private HttpProxyCacheServer proxy;
    private String musicFilelCacheDir = null;
    private boolean mHasCache;

    public boolean isHasCache() {
        return mHasCache;
    }

    public void setHasCache(boolean mHasCache) {
        this.mHasCache = mHasCache;
    }

    public static HttpProxyCacheServer getProxy() {
        return MusicPlayerManager.getInstance().proxy == null ? (MusicPlayerManager.getInstance().proxy = MusicPlayerManager.getInstance().newProxy()) : MusicPlayerManager.getInstance().proxy;
    }

    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer.Builder(application)
                .cacheDirectory(new File(musicFilelCacheDir))
                .fileNameGenerator(new CacheFileNameGenerator())
                .build();
    }

    public static int count = 0;
    public static int ActivityCount = 0;

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        ActivityCount++;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        ActivityCount--;
        if (ActivityCount == 0) {
            MusicLibLog.d(TAG, ">>>>>>>>>>>>>>>>>>>APP 关闭");
//            getControl().release();
        }
    }
}
