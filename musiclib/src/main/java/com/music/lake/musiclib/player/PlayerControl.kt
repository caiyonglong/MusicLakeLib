package com.music.lake.musiclib.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.music.lake.musiclib.bean.BaseMusicInfo
import com.music.lake.musiclib.listener.MusicPlayEventListener
import com.music.lake.musiclib.listener.MusicPlayerController
import com.music.lake.musiclib.listener.MusicUrlRequest
import com.music.lake.musiclib.manager.MediaQueueManager
import com.music.lake.musiclib.media.extensions.duration
import com.music.lake.musiclib.service.MusicPlayerService
import com.music.lake.musiclib.utils.MusicLibLog

class PlayerControl(context: Context) : MusicPlayerController {
    private val TAG = "MusicPlayerManager"

    /**
     * Media
     */
    private val mediaBrowserConnectionCallback by lazy {
        MediaBrowserConnectionCallback(context)
    }
    private var mediaBrowser: MediaBrowserCompat? = null
    private var transportControls: MediaControllerCompat.TransportControls? = null
    private var mediaController: MediaControllerCompat? = null
    private var listener: MusicPlayEventListener? = null

    init {
        mediaBrowser = MediaBrowserCompat(
            context,
            ComponentName(context, MusicPlayerService::class.java),
            mediaBrowserConnectionCallback, null
        ).apply { connect() }
    }

    override fun playMusicById(index: Int) {}

    override fun playMusic(song: BaseMusicInfo) {
        transportControls?.playFromUri(Uri.parse(song.uri), Bundle().apply {
            putString("title", song.title)
        });
    }

    override fun playMusic(songs: MutableList<BaseMusicInfo>, index: Int) {
        MediaQueueManager.updatePlaylist(songs, index)
    }

    override fun updatePlaylist(songs: MutableList<BaseMusicInfo>, index: Int) {
        MediaQueueManager.updatePlaylist(songs, index)
    }

    override fun playNextMusic() {
        transportControls?.skipToNext()
    }

    override fun playPrevMusic() {
        transportControls?.skipToPrevious()
    }

    override fun restorePlay() {
    }

    override fun pausePlay() {
        transportControls?.pause()
    }

    override fun stopPlay() {
        transportControls?.stop()
    }

    override fun setLoopMode(mode: Int) {
        MediaQueueManager.setLoopMode(mode)
    }

    override fun getLoopMode(): Int {
        return MediaQueueManager.getLoopMode()
    }

    override fun seekTo(ms: Long) {
        transportControls?.seekTo(ms)
    }

    override fun getNowPlayingMusic(): BaseMusicInfo? {
        return MediaQueueManager.getNowPlayingMusic()
    }

    override fun getNowPlayingIndex(): Int {
        return MediaQueueManager.getNowPlayingIndex()
    }

    override fun getPlayList(): List<BaseMusicInfo> {
        return MediaQueueManager.mPlaylist
    }

    override fun removeFromPlaylist(position: Int) {
        MediaQueueManager.removeFromPlaylist(
            position
        )
    }

    override fun clearPlaylist() {
        MediaQueueManager.clear()
    }

    override fun isPlaying(): Boolean {
        return mediaController?.isSessionReady ?: false
    }

    override fun getDuration(): Long {
        return mediaController?.metadata?.duration ?: 0
    }

    override fun getPlayingPosition(): Long {
        return mediaController?.playbackState?.position ?: 0
    }

    override fun addMusicPlayerEventListener(listener: MusicPlayEventListener) {
        this.listener = listener
    }

    override fun removeMusicPlayerEventListener(listener: MusicPlayEventListener) {}

    override fun setMusicRequestListener(urlRequest: MusicUrlRequest) {
    }

    override fun showDesktopLyric(show: Boolean) {}
    override fun AudioSessionId(): Int {
        return 0
    }

    fun getPlayEventListener(): MusicPlayEventListener? {
        return listener
    }

    private inner class MediaBrowserConnectionCallback(private val context: Context) :
        MediaBrowserCompat.ConnectionCallback() {
        /**
         * Invoked after [MediaBrowserCompat.connect] when the request has successfully
         * completed.
         */
        override fun onConnected() {
            // Get a MediaController for the MediaSession.
            MusicLibLog.d(TAG, "MediaBrowserConnectionCallback: onConnected")
            MusicLibLog.d(TAG, "mediaBrowser ${mediaBrowser == null}")
            mediaController = MediaControllerCompat(context, mediaBrowser!!.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            MusicLibLog.d(TAG, "mediaController ${mediaController == null}")
            transportControls = mediaController?.transportControls
        }

        /**
         * Invoked when the client is disconnected from the media browser.
         */
        override fun onConnectionSuspended() {
            MusicLibLog.d(TAG, "MediaBrowserConnectionCallback: onConnectionSuspended")
        }

        /**
         * Invoked when the connection to the media browser failed.
         */
        override fun onConnectionFailed() {
            MusicLibLog.d(TAG, "MediaBrowserConnectionCallback: onConnectionFailed")
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onSessionReady() {
            super.onSessionReady()
            MusicLibLog.d(TAG, "MediaControllerCallback: onSessionReady")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            MusicLibLog.d(TAG, "onPlaybackStateChanged: state ${state?.playbackState}")
//            playbackState.postValue(state ?: EMPTY_PLAYBACK_STATE)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            MusicLibLog.d(TAG, "onMetadataChanged: state ${metadata}")
//            nowPlaying.postValue(metadata ?: NOTHING_PLAYING)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            MusicLibLog.d(TAG, "onQueueChanged: state ${queue.toString()}")
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            MusicLibLog.d(TAG, "onQueueChanged: event $event ")
        }

        /**
         * Normally if a [MediaBrowserServiceCompat] drops its connection the callback comes via
         * [MediaControllerCompat.Callback] (here). But since other connection status events
         * are sent to [MediaBrowserCompat.ConnectionCallback], we catch the disconnect here and
         * send it on to the other callback.
         */
        override fun onSessionDestroyed() {
            MusicLibLog.d(TAG, "onSessionDestroyed")
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }

    fun release() {
        mediaBrowser?.disconnect()
    }
}