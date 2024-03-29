package com.music.lake.musiclib.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.music.lake.musiclib.R;
import com.music.lake.musiclib.manager.MediaQueueManager;
import com.music.lake.musiclib.player.BaseLakePlayer;
import com.music.lake.musiclib.utils.MusicLibLog;

/**
 * Created by master on 2018/5/14.
 * 通知栏管理类
 */
public class NotifyManager {
    private static final int NOTIFICATION_ID = 123789;
    private long mNotificationPostTime = 0;
    private static final String TAG = "NotifyManager";

    /**
     * 通知栏
     */
    public static final String ACTION_MUSIC_NOTIFY = "com.cyl.music_lake.notify";//通知栏广播标志
    public static final String ACTION_LYRIC = "com.cyl.music_lake.notify.lyric";// 歌词广播标志
    public static final String ACTION_NEXT = "com.cyl.music_lake.notify.next";// 下一首广播标志
    public static final String ACTION_PREV = "com.cyl.music_lake.notify.prev";// 上一首广播标志
    public static final String ACTION_PLAY_PAUSE = "com.cyl.music_lake.notify.play_state";// 播放暂停广播
    public static final String ACTION_CLOSE = "com.cyl.music_lake.notify.close";// 关闭
    public static final String ACTION_SHUFFLE = "com.cyl.music_lake.notify.shuffle";// 关闭

    public static final String ACTION_IS_WIDGET = "ACTION_IS_WIDGET";// 是否是桌面小控件

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;

    private androidx.media.app.NotificationCompat.MediaStyle mediaStyle;
    private boolean showWhen;

    private Notification mNotification;

    private Service mService;
    private Context mContext;
    private BaseLakePlayer baseLakePlayerImpl;

    public NotifyManager(Service service) {
        this.mService = service;
        this.mContext = service;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Notification
    //////////////////////////////////////////////////////////////////////////*/

    private void resetNotification() {
        mNotificationBuilder = createNotification();
    }

    private NotificationCompat.Builder createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, initChannelId())
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_music)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(retrievePlaybackAction(ACTION_MUSIC_NOTIFY))
                .setContentTitle(baseLakePlayerImpl.getTitle())
                .setContentText(baseLakePlayerImpl.getArtistName())
                .setWhen(mNotificationPostTime)
                .addAction(R.drawable.ic_skip_previous,
                        "",
                        retrievePlaybackAction(ACTION_PREV));
        if (baseLakePlayerImpl.isPlaying()) {
            builder.addAction(R.drawable.ic_pause, "",
                    retrievePlaybackAction(ACTION_PLAY_PAUSE));
        } else {
            builder.addAction(R.drawable.ic_play, "",
                    retrievePlaybackAction(ACTION_PLAY_PAUSE));
        }
        builder.addAction(R.drawable.ic_skip_next,
                "",
                retrievePlaybackAction(ACTION_NEXT))
                .addAction(R.drawable.ic_lyric,
                        "",
                        retrievePlaybackAction(ACTION_LYRIC))
                .addAction(R.drawable.ic_clear,
                        "",
                        retrievePlaybackAction(ACTION_CLOSE))
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mContext, PlaybackStateCompat.ACTION_STOP));
        builder.setShowWhen(showWhen);
        if (mediaStyle != null) {
            builder.setStyle(mediaStyle);
        }
        return builder;
    }

    public void setupNotification() {
        if (baseLakePlayerImpl == null) return;
        mNotificationManager = (NotificationManager) mService.getSystemService(mService.NOTIFICATION_SERVICE);
        if (mNotificationPostTime == 0) {
            mNotificationPostTime = System.currentTimeMillis();
        }
        resetNotification();
        mNotification = mNotificationBuilder.build();
    }

    public synchronized void updateNotification(boolean isPlaying, boolean isChange, Bitmap bitmap) {
        MusicLibLog.d(TAG, "updateNotification() isChange：" + isChange + "， isPlaying = [" + isPlaying + "]");
        if (mNotificationBuilder == null) return;
        if (isChange) {
            baseLakePlayerImpl.mNowPlayingMusic = MediaQueueManager.INSTANCE.getNowPlayingMusic();
            MusicLibLog.d(TAG, "updateNotification() getTitle：" + baseLakePlayerImpl.getTitle()
                    + "， getArtistName = " + baseLakePlayerImpl.getArtistName());
            if (bitmap != null) {
                mNotificationBuilder.setLargeIcon(bitmap);
            }
            mNotificationBuilder.setContentTitle(baseLakePlayerImpl.getTitle());
            mNotificationBuilder.setContentText(baseLakePlayerImpl.getArtistName());
            mNotificationBuilder.setTicker(baseLakePlayerImpl.getTitle() + "-" + baseLakePlayerImpl.getArtistName());
        }
        if (isPlaying)
            mNotificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_pause, "",
                    retrievePlaybackAction(ACTION_PLAY_PAUSE)));
        else
            mNotificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_play, "",
                    retrievePlaybackAction(ACTION_PLAY_PAUSE)));
        mNotification = mNotificationBuilder.build();
        //前台服务
        mService.startForeground(NOTIFICATION_ID, mNotification);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    public void close() {
        if (mNotificationManager != null) mNotificationManager.cancel(NOTIFICATION_ID);
        mService.stopForeground(true);
    }

    private PendingIntent retrievePlaybackAction(final String action) {
        Intent intent = new Intent(action);
        return PendingIntent.getBroadcast(mContext, 0, intent, 0);
    }

    /**
     * 创建Notification ChannelID
     *
     * @return 频道id
     */
    private String initChannelId() {
        // 通知渠道的id
        String id = "music_lake_01";
        // 用户可以看到的通知渠道的名字.
        CharSequence name = "音乐湖";
        // 用户可以看到的通知渠道的描述
        String description = "通知栏播放控制";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel;
            mChannel = new NotificationChannel(id, name, importance);
            mChannel.setDescription(description);
            mChannel.enableLights(false);
            mChannel.enableVibration(false);
            //最后在notificationmanager中创建该通知渠道
            mNotificationManager.createNotificationChannel(mChannel);
        }
        return id;
    }

    public void setBasePlayerImpl(BaseLakePlayer baseLakePlayerImpl) {
        this.baseLakePlayerImpl = baseLakePlayerImpl;
    }

    public void setShowWhen(boolean showWhen) {
        this.showWhen = showWhen;
    }

    public void setStyle(androidx.media.app.NotificationCompat.MediaStyle style) {
        this.mediaStyle = style;
    }
}
