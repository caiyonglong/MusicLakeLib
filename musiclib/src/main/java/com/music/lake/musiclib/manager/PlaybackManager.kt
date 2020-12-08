package com.music.lake.musiclib.manager

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import com.music.lake.musiclib.MusicPlayerManager
import com.music.lake.musiclib.bean.BaseMusicInfo
import com.music.lake.musiclib.listener.MusicPlayEventListener
import com.music.lake.musiclib.listener.MusicRequestCallBack
import com.music.lake.musiclib.listener.MusicUrlRequest
import com.music.lake.musiclib.manager.MediaQueueManager.getNextPosition
import com.music.lake.musiclib.manager.MediaQueueManager.getPreviousPosition
import com.music.lake.musiclib.manager.MediaQueueManager.mHistoryPos
import com.music.lake.musiclib.notification.INotification
import com.music.lake.musiclib.playback.IPlaybackManager
import com.music.lake.musiclib.playback.PlaybackListener
import com.music.lake.musiclib.player.BasePlayer
import com.music.lake.musiclib.player.MusicExoPlayer
import com.music.lake.musiclib.player.MusicMediaPlayer
import com.music.lake.musiclib.service.MusicPlayerService
import com.music.lake.musiclib.utils.MusicLibLog

class PlaybackManager constructor(
    val context: Context,
    val mHandler: Handler,
    val listener: PlaybackListener
) : IPlaybackManager {
    private var isMusicPlaying = false
    private var playWhenReady = true
    private var mNowPlayingMusic: BaseMusicInfo? = null
    private var mMediaSessionCallback: MediaSessionCallback
    private var musicPlayEventListener: MusicPlayEventListener? = null
    private var musicUrlRequest: MusicUrlRequest? = null

    private var mNowPlayingIndex = 0
    private var notification: INotification? = null

    /**
     * 错误次数，超过最大错误次数，自动停止播放
     */
    private var playErrorTimes = 0
    private val MAX_ERROR_TIMES = 1

    private val mPlayer: BasePlayer = if (MusicPlayerManager.getInstance().isUseExoPlayer) {
        MusicExoPlayer(context)
    } else {
        MusicMediaPlayer(context)
    }

    init {
        mPlayer.setPlayBackListener(listener)
        mMediaSessionCallback = MediaSessionCallback();
        musicPlayEventListener = MusicPlayerManager.getControl().getPlayEventListener()
    }

    /**
     * 下一首
     */
    fun skipToNext(isAuto: Boolean?) {
        synchronized(this) {
            mNowPlayingIndex = getNextPosition(isAuto)
            MusicLibLog.e(TAG, "next: $mNowPlayingIndex")
            stop(false)
            mHandler.post {
                handlePlayRequest(true)
            }
        }
    }

    /**
     * 上一首
     */
    fun skipToPrevious() {
        synchronized(this) {
            mHandler.post {
                mNowPlayingIndex = getPreviousPosition()
                MusicLibLog.e(TAG, "prev: $mNowPlayingIndex")
                stop(false)
                handlePlayRequest(true)
            }
        }
    }


    /**
     * 检查歌曲播放地址是否正常
     */
    private fun checkPlayOnValid() {
        mNowPlayingMusic?.let {
            musicUrlRequest?.checkNonValid(it, object : MusicRequestCallBack {
                override fun onMusicBitmap(bitmap: Bitmap) {
                    //                coverBitmap = bitmap
//                    notifyManager.updateNotification(isMusicPlaying, true, bitmap)
                }

                override fun onMusicValid(url: String) {
                    MusicLibLog.e(TAG, "checkNonValid-----$url")
                    mNowPlayingMusic?.uri = url
                    mPlayer.playWhenReady = playWhenReady
                    mPlayer.setDataSource(url)
                }

                override fun onActionDirect() {
                    mPlayer.playWhenReady = playWhenReady
                    mPlayer.setDataSource(mNowPlayingMusic!!.uri)
                }
            })
        }
    }

    fun pausePlay() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause()
        } else {
            mPlayer.playWhenReady = true
        }
    }

    /**
     * 停止播放
     *
     * @param remove_status_icon
     */
    fun stop(remove_status_icon: Boolean) {
        if (remove_status_icon && mPlayer.isInitialized()) {
            mPlayer.stop()
        }
        if (remove_status_icon) {
            isMusicPlaying = false
        }
    }

    fun getAudioSessionId(): Int {
        synchronized(this) { return mPlayer.getAudioSessionId() }
    }

    /**
     * API 21 以上 耳机多媒体按钮监听 MediaSessionCompat.Callback
     */
    inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        //接收到监听事件，可以有选择的进行重写相关方法
        override fun onPlay() {
            MusicLibLog.d(TAG, "onPlay")
            mHandler.post {
                pausePlay()
            }
        }

        override fun onPause() {
            MusicLibLog.d(TAG, "onPause")
            mHandler.post {
                pausePlay()
            }
        }

        override fun onSkipToNext() {
            MusicLibLog.d(TAG, "onSkipToNext")
            skipToNext(false)
        }

        override fun onSkipToPrevious() {
            MusicLibLog.d(TAG, "onSkipToPrevious")
            skipToPrevious()
        }

        override fun onStop() {
            MusicLibLog.d(TAG, "onStop")
            mPlayer.stop()
        }

        override fun onSeekTo(pos: Long) {
            MusicLibLog.d(TAG, "onSeekTo $pos")
            mPlayer.seekTo(pos)
        }

        override fun onAddQueueItem(description: MediaDescriptionCompat?) {
            super.onAddQueueItem(description)
            MusicLibLog.d(TAG, "onAddQueueItem")
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
            MusicLibLog.d(TAG, "onPlayFromMediaId")
        }

        override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
            super.onPlayFromUri(uri, extras)
            MusicLibLog.d(TAG, "onPlayFromUri $uri ${extras?.getString("title")}")
            val musicInfo = BaseMusicInfo().apply {
                this.uri = uri.toString()
                this.title = extras?.getString("title")
            }
            MediaQueueManager.setNowPlayingMusic(musicInfo)
            mHandler.post {
                handlePlayRequest(true)
            }
        }
    }

    companion object {
        const val TAG = "PlaybackManager"
    }

    override val mediaSessionCallback: MediaSessionCompat.Callback
        get() = mMediaSessionCallback
    override var isPlaying: Boolean
        get() = false
        set(value) {}

    override fun handlePlayRequest(isPlayWhenReady: Boolean) {
        synchronized(this) {
            MusicLibLog.d(
                TAG,
                "index: ${MediaQueueManager.getNowPlayingIndex()} = ${MediaQueueManager.isInValidIndex()}"
            )
            if (!MediaQueueManager.isInValidIndex()) {
                return
            }
            mNowPlayingIndex = MediaQueueManager.getNowPlayingIndex()
            mNowPlayingMusic = MediaQueueManager.getNowPlayingMusic()
            musicPlayEventListener?.onMetaChanged(mNowPlayingMusic)
            mPlayer.setMusicInfo(mNowPlayingMusic)

            isMusicPlaying = false

            if (musicUrlRequest != null) {
                checkPlayOnValid()
            } else {
                playErrorTimes = 0
                mPlayer.playWhenReady = playWhenReady
                mPlayer.setDataSource(mNowPlayingMusic?.uri)
            }
            mHistoryPos.add(mNowPlayingIndex)

            if (mPlayer.isInitialized()) {
                mHandler.removeMessages(MusicPlayerService.VOLUME_FADE_DOWN)
                mHandler.sendEmptyMessage(MusicPlayerService.VOLUME_FADE_UP) //组件调到正常音量
            }
        }
    }

    override fun handlePauseRequest() {
    }

    override fun handleStopRequest(withError: String?) {
    }

    override fun updatePlaybackState(isOnlyUpdateActions: Boolean, error: String?) {
    }

    override fun registerNotification(notification: INotification) {
        this.notification = notification
    }

    override fun setMusicUrlRequest(request: MusicUrlRequest) {
        this.musicUrlRequest = request
    }
}