package com.music.lake.musiclib.manager

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import com.music.lake.musiclib.bean.BaseMusicInfo
import com.music.lake.musiclib.listener.MusicRequestCallBack
import com.music.lake.musiclib.listener.MusicUrlRequest
import com.music.lake.musiclib.manager.MediaQueueManager.getNextPosition
import com.music.lake.musiclib.manager.MediaQueueManager.getPreviousPosition
import com.music.lake.musiclib.manager.MediaQueueManager.mHistoryPos
import com.music.lake.musiclib.notification.INotification
import com.music.lake.musiclib.playback.IPlaybackManager
import com.music.lake.musiclib.player.BasePlayer
import com.music.lake.musiclib.service.MusicPlayerService
import com.music.lake.musiclib.utils.MusicLibLog

class PlaybackManager constructor(
    val mPlayer: BasePlayer,
    val mHandler: Handler
) : IPlaybackManager {
    private var isMusicPlaying = false
    private var playWhenReady = false
    private var mNowPlayingMusic: BaseMusicInfo? = null
    private var mMediaSessionCallback: MediaSessionCallback
    private var musicUrlRequest: MusicUrlRequest? = null

    private var mNowPlayingIndex = 0
    private var notification: INotification? = null
    /**
     * 错误次数，超过最大错误次数，自动停止播放
     */
    private var playErrorTimes = 0
    private val MAX_ERROR_TIMES = 1

    init {
        mMediaSessionCallback = MediaSessionCallback();
    }

    /**
     * 下一首
     */
    fun skipToNext(isAuto: Boolean?) {
        synchronized(this) {
            mNowPlayingIndex = getNextPosition(isAuto, mNowPlayingIndex)
            MusicLibLog.e(TAG, "next: $mNowPlayingIndex")
            stop(false)
            handlePlayRequest(true);
        }
    }

    /**
     * 上一首
     */
    fun skipToPrevious() {
        synchronized(this) {
            mNowPlayingIndex = getPreviousPosition(mNowPlayingIndex)
            MusicLibLog.e(TAG, "prev: $mNowPlayingIndex")
            stop(false)
            handlePlayRequest(true);
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
                    //                notifyManager.updateNotification(isMusicPlaying, true, bitmap)
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
        //        接收到监听事件，可以有选择的进行重写相关方法
        override fun onPlay() {
            MusicLibLog.d(TAG, "onPlay")
        }

        override fun onPause() {
            MusicLibLog.d(TAG, "onPause")
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
        }

        override fun onSeekTo(pos: Long) {
            MusicLibLog.d(TAG, "onSeekTo $pos")
            mPlayer.seekTo(pos)
        }

        override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
            super.onPlayFromUri(uri, extras)
            MusicLibLog.d(TAG, "onPlayFromUri $uri")
            mNowPlayingMusic = BaseMusicInfo().apply {
                this.uri = uri.toString()
            }
            MediaQueueManager.mPlaylist.add(mNowPlayingMusic!!)
            handlePlayRequest(true)
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