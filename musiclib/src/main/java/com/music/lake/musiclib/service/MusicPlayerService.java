package com.music.lake.musiclib.service;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.media.MediaBrowserServiceCompat;

import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.music.lake.musiclib.MusicPlayerManager;
import com.music.lake.musiclib.bean.BaseMusicInfo;
import com.music.lake.musiclib.listener.MusicPlayEventListener;
import com.music.lake.musiclib.listener.MusicRequestCallBack;
import com.music.lake.musiclib.listener.MusicUrlRequest;
import com.music.lake.musiclib.manager.AudioAndFocusManager;
import com.music.lake.musiclib.manager.MediaQueueManager;
import com.music.lake.musiclib.manager.PlaybackManager;
import com.music.lake.musiclib.media.library.BrowseTreeKt;
import com.music.lake.musiclib.notification.NotifyManager;
import com.music.lake.musiclib.playback.PlaybackListener;
import com.music.lake.musiclib.player.BaseLakePlayer;
import com.music.lake.musiclib.player.MusicExoPlayer;
import com.music.lake.musiclib.player.MusicMediaPlayer;
import com.music.lake.musiclib.utils.CommonUtils;
import com.music.lake.musiclib.utils.Constants;
import com.music.lake.musiclib.utils.MusicLibLog;
import com.music.lake.musiclib.utils.SystemUtils;
import com.music.lake.musiclib.utils.ToastUtils;
import com.music.lake.musiclib.widgets.appwidgets.StandardWidget;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.music.lake.musiclib.media.library.BrowseTreeKt.UAMP_EMPTY_ROOT;
import static com.music.lake.musiclib.notification.NotifyManager.ACTION_CLOSE;
import static com.music.lake.musiclib.notification.NotifyManager.ACTION_IS_WIDGET;
import static com.music.lake.musiclib.notification.NotifyManager.ACTION_LYRIC;
import static com.music.lake.musiclib.notification.NotifyManager.ACTION_MUSIC_NOTIFY;
import static com.music.lake.musiclib.notification.NotifyManager.ACTION_NEXT;
import static com.music.lake.musiclib.notification.NotifyManager.ACTION_PLAY_PAUSE;
import static com.music.lake.musiclib.notification.NotifyManager.ACTION_PREV;
import static com.music.lake.musiclib.notification.NotifyManager.ACTION_SHUFFLE;

/**
 * 作者：yonglong on 2020/2/29
 * 邮箱：643872807@qq.com
 * 版本：3.0 播放service
 */
public class MusicPlayerService extends MediaBrowserServiceCompat implements PlaybackListener {
    private static final String TAG = "MusicPlayerService";

    public static final String ACTION_SERVICE = "com.cyl.music_lake.service";// 广播标志

    public static final String PLAY_STATE_CHANGED = "com.cyl.music_lake.play_state";// 播放暂停广播

    public static final String DURATION_CHANGED = "com.cyl.music_lake.duration";// 播放时长

    public static final String TRACK_ERROR = "com.cyl.music_lake.error";
    public static final String SHUTDOWN = "com.cyl.music_lake.shutdown";
    public static final String REFRESH = "com.cyl.music_lake.refresh";

    public static final String PLAY_QUEUE_CLEAR = "com.cyl.music_lake.play_queue_clear"; //清空播放队列
    public static final String PLAY_QUEUE_CHANGE = "com.cyl.music_lake.play_queue_change"; //播放队列改变

    public static final String META_CHANGED = "com.cyl.music_lake.meta_changed";//状态改变(歌曲替换)
    public static final String SCHEDULE_CHANGED = "com.cyl.music_lake.schedule";//定时广播

    public static final String CMD_TOGGLE_PAUSE = "toggle_pause";//按键播放暂停
    public static final String CMD_NEXT = "next";//按键下一首
    public static final String CMD_PREVIOUS = "previous";//按键上一首
    public static final String CMD_PAUSE = "pause";//按键暂停
    public static final String CMD_PLAY = "play";//按键播放
    public static final String CMD_STOP = "stop";//按键停止
    public static final String CMD_FORWARD = "forward";//按键停止
    public static final String CMD_REWIND = "reward";//按键停止
    public static final String SERVICE_CMD = "cmd_service";//状态改变
    public static final String FROM_MEDIA_BUTTON = "media";//状态改变
    public static final String CMD_NAME = "name";//状态改变
    public static final String UNLOCK_DESKTOP_LYRIC = "unlock_lyric"; //音量改变增加

    public static final int AUDIO_FOCUS_CHANGE = 12; //音频焦点改变
    public static final int VOLUME_FADE_DOWN = 13; //音量改变减少
    public static final int VOLUME_FADE_UP = 14; //音量改变增加

    private int mServiceStartId = -1;

    /**
     * 错误次数，超过最大错误次数，自动停止播放
     */
    private int playErrorTimes = 0;
    private int MAX_ERROR_TIMES = 1;

    private BaseLakePlayer mPlayer = null;
    public PowerManager.WakeLock mWakeLock;
    private PowerManager powerManager;
    private TimerTask mPlayerTask;
    private Timer mPlayerTimer;

    public BaseMusicInfo mNowPlayingMusic = null;

    private int mNowPlayingIndex = -1;
    private int mNextPlayPos = -1;
    private String mPlaylistId = Constants.PLAYLIST_QUEUE_ID;

    //广播接收者
    ServiceReceiver mServiceReceiver;
    BecomingNoisyReceiver mBecomingNoisyReceiver;
    StandardWidget mStandardWidget;
    HeadsetPlugInReceiver mHeadsetPlugInReceiver;
    IntentFilter intentFilter;

    public Bitmap coverBitmap;

    //    private MediaSessionManager mediaSessionManager;
    private AudioAndFocusManager audioAndFocusManager;

    private NotifyManager notifyManager;

    private boolean isRunningForeground = false;
    private boolean isMusicPlaying = false;
    //暂时失去焦点，会再次回去音频焦点
    private boolean mPausedByTransientLossOfFocus = false;

    //准备好直接播放
    private boolean playWhenReady = true;

    protected MediaSessionCompat mediaSession;
    protected MediaControllerCompat mediaController;
    protected MediaSessionConnector mediaSessionConnector;

    //播放缓存进度
    private int percent = 0;

    boolean mServiceInUse = false;
    //工作线程和Handler
    private MusicPlayerHandler mHandler;
    private HandlerThread mWorkThread;
    //主线程Handler
    private Handler mMainHandler;

    private static MusicPlayerService instance;

    private MusicUrlRequest musicUrlRequest;

    private PlaybackManager playbackManager;

    //歌词定时器
    private Timer lyricTimer;

    public static MusicPlayerService getInstance() {
        return instance;
    }

    private List<MusicPlayEventListener> playbackListeners = new ArrayList<>();

    @Override
    public void onCompletionNext() {
        next(true);
    }

    @Override
    public void onCompletionEnd() {
        if (MediaQueueManager.INSTANCE.getLoopMode() == MediaQueueManager.PLAY_MODE_REPEAT) {
            seekTo(0, false);
            play();
        } else {
            next(true);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        MusicLibLog.e(TAG, "PREPARE_ASYNC_UPDATE Loading ... " + percent);
        this.percent = percent;
    }

    @Override
    public void onPrepared() {
        MusicLibLog.e(TAG, "PLAYER_PREPARED");
        //执行prepared之后 准备完成，更新总时长
        //准备完成，可以播放
        isMusicPlaying = true;
        notifyManager.updateNotification(isMusicPlaying, false, null);
        notifyChange(PLAY_STATE_CHANGED);
    }

    @Override
    public void onError() {
        ToastUtils.show("歌曲播放地址异常，请切换其他歌曲");
//        playErrorTimes++;
//        next(true);
    }

    @Override
    public void onPlaybackProgress(long position, long duration, long buffering) {

    }

    @Override
    public void onLoading(boolean isLoading) {
        for (int i = 0; i < playbackListeners.size(); i++) {
            playbackListeners.get(i).onLoading(isLoading);
        }
    }

    @Override
    public void onPlayerStateChanged(boolean isMusicPlaying) {
        this.isMusicPlaying = isMusicPlaying;
        MusicLibLog.d(TAG, "onPlayerStateChanged " + isMusicPlaying + " playWhenReady=" + playWhenReady);
        if (!playWhenReady) {
            playWhenReady = true;
        }
        notifyChange(PLAY_STATE_CHANGED);
        notifyManager.updateNotification(isMusicPlaying, true, null);
        for (int i = 0; i < playbackListeners.size(); i++) {
            playbackListeners.get(i).onPlayerStateChanged(isMusicPlaying);
        }
    }

    public class MusicPlayerHandler extends Handler {
        private final WeakReference<MusicPlayerService> mService;
        private float mCurrentVolume = 1.0f;

        public MusicPlayerHandler(final MusicPlayerService service, final Looper looper) {
            super(looper);
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MusicPlayerService service = mService.get();
            synchronized (mService) {
                switch (msg.what) {
                    case VOLUME_FADE_DOWN:
                        mCurrentVolume -= 0.05f;
                        if (mCurrentVolume > 0.2f) {
                            sendEmptyMessageDelayed(VOLUME_FADE_DOWN, 10);
                        } else {
                            mCurrentVolume = 0.2f;
                        }
                        mMainHandler.post(() -> {
                            service.mPlayer.setVolume(mCurrentVolume);
                        });
                        break;
                    case VOLUME_FADE_UP:
                        mCurrentVolume += 0.01f;
                        if (mCurrentVolume < 1.0f) {
                            sendEmptyMessageDelayed(VOLUME_FADE_UP, 10);
                        } else {
                            mCurrentVolume = 1.0f;
                        }
                        mMainHandler.post(() -> {
                            service.mPlayer.setVolume(mCurrentVolume);
                        });
                        break;
                    case AUDIO_FOCUS_CHANGE:
                        switch (msg.arg1) {
                            case AudioManager.AUDIOFOCUS_LOSS://失去音频焦点
                            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT://暂时失去焦点
                                if (service.isPlaying()) {
                                    mPausedByTransientLossOfFocus =
                                            msg.arg1 == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
                                }
                                mMainHandler.post(service::pause);
                                break;
                            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                                removeMessages(VOLUME_FADE_UP);
                                sendEmptyMessage(VOLUME_FADE_DOWN);
                                break;
                            case AudioManager.AUDIOFOCUS_GAIN://重新获取焦点
                                //重新获得焦点，且符合播放条件，开始播放
                                if (!service.isPlaying()
                                        && mPausedByTransientLossOfFocus) {
                                    mPausedByTransientLossOfFocus = false;
                                    mCurrentVolume = 0f;
                                    service.mPlayer.setVolume(mCurrentVolume);
                                    mMainHandler.post(service::play);
                                } else {
                                    removeMessages(VOLUME_FADE_DOWN);
                                    sendEmptyMessage(VOLUME_FADE_UP);
                                }
                                break;
                            default:
                        }
                        break;
                }
            }
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        MusicLibLog.e(TAG, "onCreate");
        instance = this;
        //初始化参数
        initConfig();
        //初始化广播
        initReceiver();
        //初始化电话监听服务
        initTelephony();
        //初始化音乐播放服务
        initMediaPlayer();
        //初始化通知
        initNotify();
    }

    /**
     * 初始化并激活 MediaSession
     */
    private void setupMediaSession() {
        Intent sessionIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent sessionActivityPendingIntent =
                PendingIntent.getActivity(this, 0, sessionIntent, 0);
        //        第二个参数 tag: 这个是用于调试用的,随便填写即可
        mediaSession = new MediaSessionCompat(this, "MusicService");
        mediaSession.setSessionActivity(sessionActivityPendingIntent);
        //指明支持的按键信息类型
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );
        mediaController = new MediaControllerCompat(this, mediaSession);
        mediaController.registerCallback(new MediaControllerCallback());
        setSessionToken(mediaSession.getSessionToken());
    }


    /**
     * 参数配置，AudioManager、锁屏
     */
    @SuppressLint("InvalidWakeLockTag")
    private void initConfig() {

        //初始化主线程Handler
        mMainHandler = new Handler(Looper.getMainLooper());

        //初始化工作线程
        mWorkThread = new HandlerThread("MusicPlayerThread");
        mWorkThread.start();

        mHandler = new MusicPlayerHandler(this, mWorkThread.getLooper());

        //电源键
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PlayerWakelockTag");

        audioAndFocusManager = new AudioAndFocusManager(this, mHandler);

        //初始化和设置MediaSessionCompat
        setupMediaSession();
    }


    /**
     * 释放通知栏;
     */
    private void releaseServiceUiAndStop() {
        if (isPlaying()) {
            return;
        }

        MusicLibLog.d(TAG, "Nothing is playing anymore, releasing notification");

        notifyManager.close();

        mediaSession.setCallback(null);
        mediaSession.setActive(false);
        mediaSession.release();

        if (!mServiceInUse) {
//            savePlayQueue(false);
            stopSelf(mServiceStartId);
        }
    }

    /**
     * 重新加载当前进度
     */
//    public void reloadPlayQueue() {
//        mPlaylist.clear();
//        mHistoryPos.clear();
//        mPlaylist = PlayQueueLoader.INSTANCE.getPlayQueue();
//        mNowPlayingIndex = SPUtils.getPlayPosition();
//        if (mNowPlayingIndex >= 0 && mNowPlayingIndex < mPlaylist.size()) {
//            mPlayingMusic = mPlaylist.get(mNowPlayingIndex);
//            updateNotification(true);
//            seekTo(SPUtils.getPosition(), true);
//            notifyChange(META_CHANGED);
//        }
//        notifyChange(PLAY_QUEUE_CHANGE);
//    }

    /**
     * 初始化电话监听服务
     */
    private void initTelephony() {
        TelephonyManager telephonyManager = (TelephonyManager) this
                .getSystemService(Context.TELEPHONY_SERVICE);// 获取电话通讯服务
        telephonyManager.listen(new ServicePhoneStateListener(),
                PhoneStateListener.LISTEN_CALL_STATE);// 创建一个监听对象，监听电话状态改变事件
    }

    /**
     * 初始化音乐播放服务
     */
    private void initMediaPlayer() {
        if (MusicPlayerManager.getInstance().isUseExoPlayer()) {
            mPlayer = new MusicExoPlayer(this);
        } else {
            mPlayer = new MusicMediaPlayer(this);
        }
        mPlayer.setPlayBackListener(this);
//        mPlayerTask = new TimerTask() {
//            public void run() {
//                mMainHandler.post(() -> {
//                    for (int i = 0; i < playbackListeners.size(); i++) {
//                        playbackListeners.get(i).onPlaybackProgress(getPlayingPosition(), getDuration(), getBufferedPercentage());
//                    }
//                });
//            }
////        };
//        mPlayerTimer = new Timer();
//        mPlayerTimer.schedule(mPlayerTask, 0, 400);
        playbackManager = new PlaybackManager(this, mMainHandler, this);
        mediaSession.setCallback(playbackManager.getMediaSessionCallback(), mHandler);
        mediaSession.setActive(true);
    }

    /**
     * 初始化广播
     */
    private void initReceiver() {
        //实例化过滤器，设置广播
        intentFilter = new IntentFilter(ACTION_SERVICE);
        mServiceReceiver = new ServiceReceiver();
        mBecomingNoisyReceiver = new BecomingNoisyReceiver(this, mediaSession.getSessionToken());
        mStandardWidget = new StandardWidget();
        mHeadsetPlugInReceiver = new HeadsetPlugInReceiver();
        intentFilter.addAction(ACTION_MUSIC_NOTIFY);
        intentFilter.addAction(ACTION_NEXT);
        intentFilter.addAction(ACTION_PREV);
        intentFilter.addAction(META_CHANGED);
        intentFilter.addAction(SHUTDOWN);
        intentFilter.addAction(ACTION_PLAY_PAUSE);
        //注册广播
        registerReceiver(mServiceReceiver, intentFilter);
        mBecomingNoisyReceiver.register();
        registerReceiver(mHeadsetPlugInReceiver, intentFilter);
        registerReceiver(mStandardWidget, intentFilter);
    }

    /**
     * 启动Service服务，执行onStartCommand
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MusicLibLog.d(TAG, "Got new intent " + intent + ", startId = " + startId);
        mServiceStartId = startId;
        mServiceInUse = true;
        if (intent != null) {
            final String action = intent.getAction();
            if (SHUTDOWN.equals(action)) {
                MusicLibLog.e("即将关闭音乐播放器");
//                mShutdownScheduled = true;
                releaseServiceUiAndStop();
                return START_NOT_STICKY;
            }
            handleCommandIntent(intent);
        }
        return START_NOT_STICKY;
    }

    /**
     * Returns the "root" media ID that the client should request to get the list of
     * [MediaItem]s to browse/play.
     */
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        MusicLibLog.d(TAG, "onGetRoot:clientPackageName=" + clientPackageName + " clientUid=" + clientUid);
        /*
         * By default, all known clients are permitted to search, but only tell unknown callers
         * about search if permitted by the [BrowseTree].
         */
        boolean isKnownCaller = true;// new PackageValidator().isKnownCaller(clientPackageName, clientUid);
//        Bundle rootExtras = new Bundle();
//        rootExtras.putBoolean(MEDIA_SEARCH_SUPPORTED, isKnownCaller || browseTree.searchableByUnknownCaller);
//        rootExtras.putBoolean(CONTENT_STYLE_SUPPORTED, true);
//        rootExtras.putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID);
//        rootExtras.putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST);

        if (isKnownCaller) {
            // The caller is allowed to browse, so return the root.
            return new BrowserRoot(BrowseTreeKt.UAMP_BROWSABLE_ROOT, rootHints);
        } else {
            /**
             * Unknown caller. There are two main ways to handle this:
             * 1) Return a root without any content, which still allows the connecting client
             * to issue commands.
             * 2) Return `null`, which will cause the system to disconnect the app.
             *
             * UAMP takes the first approach for a variety of reasons, but both are valid
             * options.
             */
            return new BrowserRoot(UAMP_EMPTY_ROOT, rootHints);
        }
    }

    /**
     * Returns (via the [result] parameter) a list of [MediaItem]s that are child
     * items of the provided [parentMediaId]. See [BrowseTree] for more details on
     * how this is build/more details about the relationships.
     */
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        MusicLibLog.d(TAG, "onLoadChildren:parentId = " + parentId + " result:" + result.toString());
//        // If the media source is ready, the results will be set synchronously here.
//        val resultsSent = mediaSource.whenReady { successfullyInitialized ->
//            if (successfullyInitialized) {
//                val children = browseTree[parentMediaId]?.map { item ->
//                        MediaItem(item.description, item.flag)
//                }
//                result.sendResult(children)
//            } else {
//                mediaSession.sendSessionEvent(NETWORK_FAILURE, null)
//                result.sendResult(null)
//            }
//        }
//
//        // If the results are not ready, the service must "detach" the results before
//        // the method returns. After the source is ready, the lambda above will run,
//        // and the caller will be notified that the results are ready.
//        //
//        // See [MediaItemFragmentViewModel.subscriptionCallback] for how this is passed to the
//        // UI/displayed in the [RecyclerView].
//        if (!resultsSent) {
//            result.detach()
//        }
    }

    /**
     * 下一首
     */
    public void next(Boolean isAuto) {
        synchronized (this) {
            mNowPlayingIndex = MediaQueueManager.INSTANCE.getNextPosition(isAuto);
            MusicLibLog.e(TAG, "next: " + mNowPlayingIndex);
            stop(false);
            playCurrentAndNext();
        }
    }

    /**
     * 上一首
     */
    public void prev() {
        synchronized (this) {
            mNowPlayingIndex = MediaQueueManager.INSTANCE.getPreviousPosition();
            MusicLibLog.e(TAG, "prev: " + mNowPlayingIndex);
            stop(false);
            playCurrentAndNext();
        }
    }

    /**
     * 播放当前歌曲
     */
    private void playCurrentAndNext() {
        synchronized (this) {
            MusicLibLog.e(TAG, "playCurrentAndNext: " + mNowPlayingIndex + "-" + MediaQueueManager.INSTANCE.getMPlaylist().size());
            if (mNowPlayingIndex >= MediaQueueManager.INSTANCE.getMPlaylist().size() || mNowPlayingIndex < 0) {
                return;
            }
            mNowPlayingMusic = MediaQueueManager.INSTANCE.getNowPlayingMusic();
            mPlayer.setMusicInfo(mNowPlayingMusic);
            //更新当前歌曲
            isMusicPlaying = false;
            //检查歌曲播放地址或者专辑
            if (musicUrlRequest != null) {
                checkPlayOnValid();
            } else {
                playErrorTimes = 0;
                mPlayer.playWhenReady = playWhenReady;
                mPlayer.setDataSource(mNowPlayingMusic.getUri());
            }
            notifyChange(META_CHANGED);
            //更新播放播放状态
            notifyChange(PLAY_STATE_CHANGED);

            MediaQueueManager.INSTANCE.getMHistoryPos().add(mNowPlayingIndex);
//            mediaSessionManager.updateMetaData(mNowPlayingMusic);
//            audioAndFocusManager.requestAudioFocus();

            final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
            sendBroadcast(intent);

            if (mPlayer.isInitialized()) {
                mHandler.removeMessages(VOLUME_FADE_DOWN);
                mHandler.sendEmptyMessage(VOLUME_FADE_UP); //组件调到正常音量
            }
        }
    }

    /**
     * 检查歌曲播放地址是否正常
     */
    private void checkPlayOnValid() {
        musicUrlRequest.checkNonValid(mNowPlayingMusic, new MusicRequestCallBack() {
            @Override
            public void onMusicBitmap(@NotNull Bitmap bitmap) {
                coverBitmap = bitmap;
                notifyManager.updateNotification(isMusicPlaying, true, bitmap);
            }

            @Override
            public void onMusicValid(@NotNull String url) {
                MusicLibLog.e(TAG, "checkNonValid-----" + url);
                mNowPlayingMusic.setUri(url);
                playErrorTimes = 0;
                mPlayer.playWhenReady = playWhenReady;
                notifyManager.updateNotification(isMusicPlaying, true, null);
                mPlayer.setDataSource(url);
            }

            @Override
            public void onActionDirect() {
                playErrorTimes = 0;
                mPlayer.playWhenReady = playWhenReady;
                mPlayer.setDataSource(mNowPlayingMusic.getUri());
            }
        });
    }

    /**
     * 异常播放，自动切换下一首
     */
    private void checkPlayErrorTimes() {
        if (playErrorTimes > MAX_ERROR_TIMES) {
            pause();
        } else {
            playErrorTimes++;
            ToastUtils.show("播放地址异常，自动切换下一首");
            next(false);
        }
    }

    /**
     * 停止播放
     *
     * @param remove_status_icon
     */
    public void stop(boolean remove_status_icon) {
        if (remove_status_icon && mPlayer != null && mPlayer.isInitialized()) {
            mPlayer.stop();
        }

        if (remove_status_icon) {
            notifyManager.close();
        }

        if (remove_status_icon) {
            isMusicPlaying = false;
        }
    }

    /**
     * 根据位置播放音乐
     *
     * @param position
     */
    public void playMusic(int position) {
        if (position >= MediaQueueManager.INSTANCE.getMPlaylist().size() || position == -1) {
            mNowPlayingIndex = MediaQueueManager.INSTANCE.getNextPosition(true);
        } else {
            mNowPlayingIndex = position;
        }
        if (mNowPlayingIndex == -1)
            return;
        playCurrentAndNext();
    }

    /**
     * 音乐播放
     */
    public void play() {
        if (mPlayer.isInitialized()) {
            mPlayer.start();
            isMusicPlaying = true;
            notifyChange(PLAY_STATE_CHANGED);
            audioAndFocusManager.requestAudioFocus();
            mHandler.removeMessages(VOLUME_FADE_DOWN);
            mHandler.sendEmptyMessage(VOLUME_FADE_UP); //组件调到正常音量
            notifyManager.updateNotification(isMusicPlaying, false, null);
        } else {
            playCurrentAndNext();
        }
    }

    public int getAudioSessionId() {
        synchronized (this) {
            return mPlayer.getAudioSessionId();
        }
    }

    /**
     * 【在线音乐，搜索的音乐】加入播放队列并播放音乐
     *
     * @param baseMusicInfo
     */
    public void play(BaseMusicInfo baseMusicInfo) {
        if (baseMusicInfo == null) return;
        if (mNowPlayingIndex == -1 || MediaQueueManager.INSTANCE.getMPlaylist().size() == 0) {
            MediaQueueManager.INSTANCE.getMPlaylist().add(baseMusicInfo);
            mNowPlayingIndex = 0;
        } else if (mNowPlayingIndex < MediaQueueManager.INSTANCE.getMPlaylist().size()) {
            MediaQueueManager.INSTANCE.getMPlaylist().add(mNowPlayingIndex, baseMusicInfo);
        } else {
            MediaQueueManager.INSTANCE.getMPlaylist().add(MediaQueueManager.INSTANCE.getMPlaylist().size(), baseMusicInfo);
        }
        //发送播放列表改变
        notifyChange(PLAY_QUEUE_CHANGE);
        MusicLibLog.e(TAG, baseMusicInfo.toString());
        mNowPlayingMusic = baseMusicInfo;
        playCurrentAndNext();
    }

    /**
     * 下一首播放
     *
     * @param baseMusicInfo 设置的歌曲
     */
    public void nextPlay(BaseMusicInfo baseMusicInfo) {
        if (MediaQueueManager.INSTANCE.getMPlaylist().size() == 0) {
            play(baseMusicInfo);
        } else if (mNowPlayingIndex < MediaQueueManager.INSTANCE.getMPlaylist().size()) {
            MediaQueueManager.INSTANCE.getMPlaylist().add(mNowPlayingIndex + 1, baseMusicInfo);
            //发送播放列表改变
            notifyChange(PLAY_QUEUE_CHANGE);
        }
    }

    /**
     * 切换歌单播放
     * 1、歌单不一样切换，不一样不切换
     * 2、相同歌单只切换歌曲
     * 3、相同歌曲不重新播放
     *
     * @param baseMusicInfoList 歌单
     * @param id                歌曲位置id
     * @param pid               歌单id
     */
    public void play(List<BaseMusicInfo> baseMusicInfoList, int id, String pid) {
        MusicLibLog.d(TAG, "musicList = " + baseMusicInfoList.size() + " id = " + id + " pid = " + pid + " mPlaylistId =" + mPlaylistId);
        if (baseMusicInfoList.size() <= id) return;

        if (mPlaylistId.equals(pid) && id == mNowPlayingIndex) return;

        setPlayQueue(baseMusicInfoList);

        mNowPlayingIndex = id;

        playCurrentAndNext();
    }

    private void updatePlaylist(List<BaseMusicInfo> baseMusicInfoList, int id, String pid) {
        MusicLibLog.d(TAG, "musicList = " + baseMusicInfoList.size() + " id = " + id + " pid = " + pid + " mPlaylistId =" + mPlaylistId);
        if (baseMusicInfoList.size() <= id) return;
        if (mPlaylistId.equals(pid) && id == mNowPlayingIndex) return;
        setPlayQueue(baseMusicInfoList);

        mNowPlayingIndex = id;

        if (mNowPlayingIndex < MediaQueueManager.INSTANCE.getMPlaylist().size()) {
            mNowPlayingMusic = MediaQueueManager.INSTANCE.getMPlaylist().get(mNowPlayingIndex);
        }
        playCurrentAndNext();
    }


    /**
     * 播放暂停
     */
    public void playPause() {
        if (isPlaying()) {
            pause();
        } else {
            if (mPlayer.isInitialized()) {
                play();
            } else {
                playCurrentAndNext();
            }
        }
    }

    /**
     * 暂停播放
     */
    public void pause() {
        MusicLibLog.d(TAG, "Pausing playback");
        mPausedByTransientLossOfFocus = false;
        synchronized (this) {
            mHandler.removeMessages(VOLUME_FADE_UP);
            mHandler.sendEmptyMessage(VOLUME_FADE_DOWN);

            if (isPlaying()) {
                isMusicPlaying = false;
                notifyChange(PLAY_STATE_CHANGED);
                notifyManager.updateNotification(isMusicPlaying, false, null);
                TimerTask task = new TimerTask() {
                    public void run() {
                        MusicLibLog.d(TAG, "TimerTask ");
                        final Intent intent = new Intent(
                                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
                        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
                        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
                        sendBroadcast(intent); //由系统接收,通知系统audio_session将关闭,不再使用音效

                        mMainHandler.post(() -> mPlayer.pause());
                    }
                };
                Timer timer = new Timer();
                timer.schedule(task, 200);
            }
        }
    }

    /**
     * 是否正在播放音乐
     *
     * @return 是否正在播放音乐
     */
    public boolean isPlaying() {
        return isMusicPlaying;
    }

    /**
     * 跳到输入的进度
     */
    public void seekTo(long pos, boolean isInit) {
        MusicLibLog.e(TAG, "seekTo " + pos * getDuration() / 100);
        if (mPlayer != null && mPlayer.isInitialized() && mNowPlayingMusic != null) {
            mPlayer.seekTo(pos * getDuration() / 100);
            MusicLibLog.e(TAG, "seekTo 成功");
        } else if (isInit) {
//            playCurrentAndNext();
//            mPlayer.seek(pos);
//            mPlayer.pause();
            MusicLibLog.e(TAG, "seekTo 失败");
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        MusicLibLog.e(TAG, "onUnbind");
        mServiceInUse = false;
//        savePlayQueue(false);
        releaseServiceUiAndStop();
        stopSelf(mServiceStartId);
        return true;
    }

    /**
     * 保存播放队列
     *
     * @param full 是否存储
     */
    private void savePlayQueue(boolean full) {
        if (mNowPlayingMusic != null) {
            //保存歌曲id
            CommonUtils.saveCurrentSongId(mNowPlayingMusic.getMid());
        }
        //保存歌曲id
        CommonUtils.setPlayPosition(mNowPlayingIndex);

        MusicLibLog.e(TAG, "save 保存歌曲位置=" + mNowPlayingIndex);
    }

    /**
     * 从歌单移除歌曲
     */
    public void removeFromQueue(int position) {
        try {
            MusicLibLog.e(TAG, position + "---" + mNowPlayingIndex + "---" + MediaQueueManager.INSTANCE.getMPlaylist().size());
            if (position == mNowPlayingIndex) {
                MediaQueueManager.INSTANCE.getMPlaylist().remove(position);
                if (MediaQueueManager.INSTANCE.getMPlaylist().size() == 0) {
                    clearQueue();
                } else {
                    playMusic(position);
                }
            } else if (position > mNowPlayingIndex) {
                MediaQueueManager.INSTANCE.getMPlaylist().remove(position);
            } else {
                MediaQueueManager.INSTANCE.getMPlaylist().remove(position);
                MusicLibLog.e(TAG, position + "--remove-" + mNowPlayingIndex + "---" + MediaQueueManager.INSTANCE.getMPlaylist().size());
                mNowPlayingIndex = mNowPlayingIndex - 1;
                MusicLibLog.e(TAG, position + "--remove-" + mNowPlayingIndex + "---" + MediaQueueManager.INSTANCE.getMPlaylist().size());
            }
            notifyChange(PLAY_QUEUE_CLEAR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        MusicLibLog.e(TAG, position + "---" + mNowPlayingIndex + "---" + MediaQueueManager.INSTANCE.getMPlaylist().size());
    }

    /**
     * 获取正在播放的歌曲[本地|网络]
     */
    public void clearQueue() {
        mNowPlayingMusic = null;
        isMusicPlaying = false;
        mNowPlayingIndex = -1;
        MediaQueueManager.INSTANCE.getMPlaylist().clear();
        MediaQueueManager.INSTANCE.getMHistoryPos().clear();
//        savePlayQueue(true);
        stop(true);
        notifyChange(META_CHANGED);
        notifyChange(PLAY_STATE_CHANGED);
        notifyChange(PLAY_QUEUE_CLEAR);
    }


    /**
     * 获取总时长
     */
    public long getDuration() {
        if (mPlayer != null && mPlayer.isInitialized() && mPlayer.isPrepared()) {
            return mPlayer.duration();
        }
        return 0;
    }

    /**
     * 获取缓冲时长 0-100
     */
    public int getBufferedPercentage() {
        if (mPlayer != null && mPlayer.isInitialized() && mPlayer.isPrepared()) {
            return mPlayer.bufferedPercentage();
        }
        return 0;
    }

    /**
     * 是否准备播放
     *
     * @return
     */
    public boolean isPrepared() {
        if (mPlayer != null) {
            return mPlayer.isPrepared();
        }
        return false;
    }

    /**
     * 发送更新广播
     *
     * @param what 发送更新广播
     */
    private void notifyChange(final String what) {
        MusicLibLog.d(TAG, "notifyChange: what = " + what);
        switch (what) {
            case META_CHANGED:
                mediaSession.setMetadata(new MediaMetadataCompat.Builder().putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "ddd").build());
                updateWidget(META_CHANGED);
                break;
            case PLAY_STATE_CHANGED:
                MusicLibLog.d(TAG, " notifyChange =" + isMusicPlaying);
                updateWidget(ACTION_PLAY_PAUSE);
                updatePlaybackState();
                break;
            case PLAY_QUEUE_CLEAR:
            case PLAY_QUEUE_CHANGE:
                updateWidget(PLAY_QUEUE_CHANGE);
                for (int i = 0; i < playbackListeners.size(); i++) {
                    playbackListeners.get(i).onUpdatePlayList(MediaQueueManager.INSTANCE.getMPlaylist());
                }
                break;
        }
    }

    /**
     * 更新播放状态， 播放／暂停／拖动进度条时调用
     */
    void updatePlaybackState() {
        MusicLibLog.d(TAG, "isPlaying=  " + isPlaying());
//        if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
//        mediaSession.setPlaybackState(
//                PlaybackStateCompat.Builder()
//                        .setActions(MEDIA_SESSION_ACTIONS)
//                        .setState(state, currentPosition, 1f)
//                        .build()
//        )
    }

    /**
     * 更新桌面小控件
     */
    private void updateWidget(String action) {
//        Intent intent = new Intent(action);
//        intent.putExtra(ACTION_IS_WIDGET, true);
//        intent.putExtra(PLAY_STATE_CHANGED, isPlaying());
//        intent.putExtra(MediaQueueManager.PLAY_MODE, getLoopMode());
//        sendBroadcast(intent);
    }

    /**
     * 获取标题
     *
     * @return
     */
    public String getTitle() {
        if (mNowPlayingMusic != null) {
            return mNowPlayingMusic.getTitle();
        }
        return null;
    }

    /**
     * 获取歌手专辑
     *
     * @return
     */
    public String getArtistName() {
        if (mNowPlayingMusic != null) {
            return mNowPlayingMusic.getArtist();
//            return ConvertUtils.getArtistAndAlbum(mPlayingMusic.getArtist(), mPlayingMusic.getAlbum());
        }
        return null;
    }

    /**
     * 获取专辑名
     *
     * @return
     */
    private String getAlbumName() {
        if (mNowPlayingMusic != null) {
            return mNowPlayingMusic.getArtist();
        }
        return null;
    }

    /**
     * 获取当前音乐
     *
     * @return
     */
    public BaseMusicInfo getPlayingMusic() {
        if (mNowPlayingMusic != null) {
            return mNowPlayingMusic;
        }
        return null;
    }


    /**
     * 设置播放队列
     *
     * @param playQueue 播放队列
     */
    public void setPlayQueue(List<BaseMusicInfo> playQueue) {
        MediaQueueManager.INSTANCE.clear();
        MediaQueueManager.INSTANCE.getMPlaylist().addAll(playQueue);
        notifyChange(PLAY_QUEUE_CHANGE);
    }

    /**
     * 获取当前音乐在播放队列中的位置
     *
     * @return 当前音乐在播放队列中的位置
     */
    public int getPlayPosition() {
        if (mNowPlayingIndex >= 0) {
            return mNowPlayingIndex;
        } else return 0;
    }

    /**
     * 初始化通知栏
     */
    private void initNotify() {
        notifyManager = new NotifyManager(this);
        notifyManager.setBasePlayerImpl(mPlayer);
        if (SystemUtils.isJellyBeanMR1()) {
            notifyManager.setShowWhen(false);
        }
        if (SystemUtils.isLollipop()) {
            //线控
            isRunningForeground = true;
            androidx.media.app.NotificationCompat.MediaStyle style = new androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(1, 0, 2, 3, 4);
            notifyManager.setStyle(style);
        }
        notifyManager.setupNotification();
    }

    public String getAudioId() {
        if (mNowPlayingMusic != null) {
            return mNowPlayingMusic.getMid();
        } else {
            return null;
        }
    }


    /**
     * 电话监听
     */
    private class ServicePhoneStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            MusicLibLog.d(TAG, "TelephonyManager state=" + state + ",incomingNumber = " + incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_OFFHOOK:   //接听状态
                case TelephonyManager.CALL_STATE_RINGING:   //响铃状态
                    pause();
                    break;
            }
        }
    }


    /**
     * Service broadcastReceiver 监听service中广播
     */
    private class ServiceReceiver extends BroadcastReceiver {

//        public ServiceReceiver() {
//            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
//        }

        @Override
        public void onReceive(Context context, Intent intent) {
            MusicLibLog.d(TAG, "onReceive " + intent.getAction());
//            if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
//                LogUtil.e(TAG, "屏幕熄灭进入锁屏界面");
//            }
            if (!intent.getBooleanExtra(ACTION_IS_WIDGET, false)) {
                handleCommandIntent(intent);
            }
        }
    }


    /**
     * 处理各种广播
     */
    private void handleCommandIntent(Intent intent) {
        final String action = intent.getAction();
        final String command = SERVICE_CMD.equals(action) ? intent.getStringExtra(CMD_NAME) : action;
        MusicLibLog.d(TAG, "handleCommandIntent: action = " + action + ", command = " + command);
        if (PLAY_STATE_CHANGED.equals(action)) {
            playbackManager.pausePlay();
            return;
        }
        if (command == null) return;
        switch (command) {
            case ACTION_MUSIC_NOTIFY:
                updateWidget(ACTION_MUSIC_NOTIFY);
                break;
            case ACTION_LYRIC:
                updateWidget(ACTION_LYRIC);
                break;
            case CMD_NEXT:
            case ACTION_NEXT:
                next(false);
                break;
            case CMD_PREVIOUS:
            case ACTION_PREV:
                prev();
                break;
            case CMD_TOGGLE_PAUSE:
            case ACTION_PLAY_PAUSE:
                playbackManager.pausePlay();
                break;
            case ACTION_CLOSE:
                stop(true);
                stopSelf();
                releaseServiceUiAndStop();
                System.exit(0);
                break;
            case CMD_PAUSE:
                pause();
                break;
            case ACTION_SHUFFLE:
                MediaQueueManager.INSTANCE.updateLoopMode();
                notifyChange(PLAY_STATE_CHANGED);
                break;
            case CMD_PLAY:
                play();
                break;
            case CMD_STOP:
                pause();
                mPausedByTransientLossOfFocus = false;
                seekTo(0, false);
                releaseServiceUiAndStop();
                break;
            case UNLOCK_DESKTOP_LYRIC:
                break;
            default:
                break;
        }
    }

    /**
     * 接受[MediaSessionCompat]状态改变回调
     * - 新建或更新服务的通知
     * - 注册/注销 [AudioManager.ACTION_AUDIO_BECOMING_NOISY] 广播
     * - Calls [Service.startForeground] and [Service.stopForeground].
     */
    private class MediaControllerCallback extends MediaControllerCompat.Callback {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            MusicLibLog.d(
                    TAG, "onPlaybackStateChanged state =${state.playbackState} ${control.isPlaying()}}"
            );
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            MusicLibLog.d(TAG, "onMetadataChanged metadata = ${metadata.size()}");
        }
    }

    /**
     * 耳机插入广播接收器
     */
    public class HeadsetPlugInReceiver extends BroadcastReceiver {
        public HeadsetPlugInReceiver() {
            if (Build.VERSION.SDK_INT >= 21) {
                intentFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);
            } else {
                intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra("state")) {
                //通过判断 "state" 来知道状态
                final boolean isPlugIn = intent.getExtras().getInt("state") == 1;
                MusicLibLog.e(TAG, "耳机插入状态 ：" + isPlugIn);
            }
        }
    }

    /**
     * 耳机拔出、来电监听广播接收器
     * <p>
     * Helper class for listening for when headphones are unplugged (or the audio
     * will otherwise cause playback to become "noisy").
     */
    private class BecomingNoisyReceiver extends BroadcastReceiver {
        final BluetoothAdapter bluetoothAdapter;
        private MediaControllerCompat controller;
        private boolean registered = false;
        private Context context;
        private IntentFilter noisyIntentFilter = new IntentFilter();

        public BecomingNoisyReceiver(Context context, MediaSessionCompat.Token token) {
            this.context = context;
            noisyIntentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY); //有线耳机拔出变化
            noisyIntentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED); //蓝牙耳机连接变化
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        public void register() {
            if (!registered) {
                context.registerReceiver(this, noisyIntentFilter);
                registered = true;
            }
        }

        public void unregister() {
            if (registered) {
                context.unregisterReceiver(this);
                registered = true;
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isRunningForeground) {
                //当前是正在运行的时候才能通过媒体按键来操作音频
                switch (intent.getAction()) {
                    case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                        MusicLibLog.e("蓝牙耳机插拔状态改变");
                        if (bluetoothAdapter != null &&
                                BluetoothProfile.STATE_DISCONNECTED == bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) &&
                                isPlaying()) {
                            //蓝牙耳机断开连接 同时当前音乐正在播放 则将其暂停
                            pause();
                        }
                        break;
                    case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                        MusicLibLog.e("有线耳机插拔状态改变");
                        if (isPlaying()) {
                            //有线耳机断开连接 同时当前音乐正在播放 则将其暂停
                            pause();
                        }
                        break;

                }
            }
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onDestroy() {
        super.onDestroy();
        MusicLibLog.e(TAG, "onDestroy");
//        disposable.dispose();
        // Remove any sound effects
        final Intent audioEffectsIntent = new Intent(
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(audioEffectsIntent);
//        savePlayQueue(false);

        coverBitmap = null;
        //释放mPlayer
        if (mPlayer != null) {
            mPlayer.stop();
            isMusicPlaying = false;
            mPlayer.release();
            mPlayer = null;
        }

        // 释放Handler资源
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }

        // 释放工作线程资源
        if (mWorkThread != null && mWorkThread.isAlive()) {
            mWorkThread.quitSafely();
            mWorkThread.interrupt();
            mWorkThread = null;
        }

        audioAndFocusManager.abandonAudioFocus();
        notifyManager.close();

        //注销广播
        unregisterReceiver(mServiceReceiver);
        mBecomingNoisyReceiver.unregister();
        unregisterReceiver(mHeadsetPlugInReceiver);
        unregisterReceiver(mStandardWidget);

        if (mWakeLock.isHeld())
            mWakeLock.release();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }
}
