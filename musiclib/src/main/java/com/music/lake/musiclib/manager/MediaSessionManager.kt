package com.music.lake.musiclib.manager

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.music.lake.musiclib.bean.BaseMusicInfo
import com.music.lake.musiclib.listener.MusicPlayerController
import com.music.lake.musiclib.utils.MusicLibLog

/**
 * MediaSession管理类
 * 主要管理Android 5.0以后线控和蓝牙远程控制播放
 */
class MediaSessionManager(
    private val context: Context,
    private val mHandler: Handler,
    private val playbackManager: PlaybackManager
) {
    protected lateinit var mediaSession: MediaSessionCompat
    protected lateinit var mediaController: MediaControllerCompat
    val control = context as MusicPlayerController

    /**
     * 初始化并激活 MediaSession
     */
    private fun setupMediaSession() {
        val sessionIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        val sessionActivityPendingIntent =
            PendingIntent.getActivity(context, 0, sessionIntent, 0)
        //        第二个参数 tag: 这个是用于调试用的,随便填写即可
        mediaSession = MediaSessionCompat(context, "MusicService")
        mediaSession.setSessionActivity(sessionActivityPendingIntent)
        //指明支持的按键信息类型
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        mediaSession.setCallback(callback, mHandler)
        mediaSession.isActive = true
        mediaController = MediaControllerCompat(context, mediaSession)
        mediaController.registerCallback(MediaControllerCallback())
    }

    /**
     * 更新播放状态， 播放／暂停／拖动进度条时调用
     */
    fun updatePlaybackState() {
        MusicLibLog.d(TAG, "isPlaying=  $isPlaying")
        val state =
            if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(MEDIA_SESSION_ACTIONS)
                .setState(state, currentPosition, 1f)
                .build()
        )
    }

    private val currentPosition: Long
        private get() = control.getPlayingPosition() ?: -1

    /**
     * 是否在播放
     *
     * @return
     */
    protected val isPlaying: Boolean
        protected get() = control.isPlaying()

    /**
     * 更新正在播放的音乐信息，切换歌曲时调用
     */
    fun updateMetaData(songInfo: BaseMusicInfo?) {
        if (songInfo == null) {
            mediaSession.setMetadata(null)
            return
        }
        val metaDta = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, songInfo.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, songInfo.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, songInfo.album)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, songInfo.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, songInfo.duration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            metaDta.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, count)
        }
        mediaSession.setMetadata(metaDta.build())
    }

    private val count: Long
        private get() = control.getPlayList().size.toLong() ?: 0

    fun getMediaSessionToken(): MediaSessionCompat.Token {
        return mediaSession.sessionToken;
    }

    /**
     * 释放MediaSession，退出播放器时调用
     */
    fun release() {
        mediaSession.setCallback(null)
        mediaSession.isActive = false
        mediaSession.release()
    }

    /**
     * API 21 以上 耳机多媒体按钮监听 MediaSessionCompat.Callback
     */
    private val callback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        //        接收到监听事件，可以有选择的进行重写相关方法
        override fun onPlay() {
            MusicLibLog.d(TAG, "onPlay")
            control?.restorePlay()
        }

        override fun onPause() {
            MusicLibLog.d(TAG, "onPause")
            control?.pausePlay()
        }

        override fun onSkipToNext() {
            MusicLibLog.d(TAG, "onSkipToNext")
            control?.playNextMusic()
        }

        override fun onSkipToPrevious() {
            MusicLibLog.d(TAG, "onSkipToPrevious")
            control.playPrevMusic()
        }

        override fun onStop() {
            MusicLibLog.d(TAG, "onStop")
            control?.stopPlay()
        }

        override fun onSeekTo(pos: Long) {
            MusicLibLog.d(TAG, "onSeekTo $pos")
            control?.seekTo(pos)
        }

        override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
            super.onPlayFromUri(uri, extras)
            MusicLibLog.d(TAG, "onPlayFromUri $uri")
            val musicInfo = BaseMusicInfo().apply {
                this.uri = uri.toString()
            }
            control?.playMusic(musicInfo)
        }

    }

    /**
     * 接受[MediaSessionCompat]状态改变回调
     * - 新建或更新服务的通知
     * - 注册/注销 [AudioManager.ACTION_AUDIO_BECOMING_NOISY] 广播
     * - Calls [Service.startForeground] and [Service.stopForeground].
     */
    inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            super.onPlaybackStateChanged(state)
            MusicLibLog.d(
                TAG, "onPlaybackStateChanged state =${state.playbackState} ${control.isPlaying()}}"
            )
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            super.onMetadataChanged(metadata)
            MusicLibLog.d(
                TAG,
                "onMetadataChanged metadata = ${metadata.size()}"
            )
        }
    }

    companion object {
        private const val TAG = "MediaSessionManager"
        //指定可以接收的来自锁屏页面的按键信息
        private const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_SEEK_TO)
    }

    init {
        setupMediaSession()
    }
}