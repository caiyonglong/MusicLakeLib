package com.music.lake.musiclib.playback

import android.media.MediaPlayer
import com.music.lake.musiclib.bean.BaseMusicInfo

/**
 * Created by master on 2018/5/14.
 * 播放回调
 */
interface PlaybackListener {

    fun onCompletionNext()
    /**
     * 完成结束
     */
    fun onCompletionEnd()

    fun onBufferingUpdate(mp: MediaPlayer?, percent: Int)
    fun onPrepared()
    fun onError()
    fun onPlaybackProgress(
        position: Long,
        duration: Long,
        buffering: Long
    )

    fun onLoading(isLoading: Boolean)
    fun onPlayerStateChanged(isPlaying: Boolean)
}