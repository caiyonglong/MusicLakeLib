package com.music.lake.musiclib.playback

import android.support.v4.media.session.MediaSessionCompat
import com.music.lake.musiclib.listener.MusicUrlRequest
import com.music.lake.musiclib.notification.INotification

interface IPlaybackManager {

    val mediaSessionCallback: MediaSessionCompat.Callback

    /**
     * 是否在播放
     */
    var isPlaying: Boolean

    /**
     * 播放
     */
    fun handlePlayRequest(isPlayWhenReady: Boolean)

    /**
     * 暂停
     */
    fun handlePauseRequest()

    /**
     * 停止
     */
    fun handleStopRequest(withError: String?)

    /**
     * 更新播放状态
     */
    fun updatePlaybackState(isOnlyUpdateActions: Boolean, error: String?)

    fun registerNotification(notification: INotification)

    fun setMusicUrlRequest(request: MusicUrlRequest)
}