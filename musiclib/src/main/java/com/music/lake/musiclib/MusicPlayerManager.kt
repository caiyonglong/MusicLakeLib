package com.music.lake.musiclib

import android.app.Application
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.danikula.videocache.HttpProxyCacheServer
import com.music.lake.musiclib.bean.BaseMusicInfo
import com.music.lake.musiclib.cache.CacheFileNameGenerator
import com.music.lake.musiclib.listener.BindServiceCallBack
import com.music.lake.musiclib.listener.MusicPlayEventListener
import com.music.lake.musiclib.listener.MusicUrlRequest
import com.music.lake.musiclib.manager.MediaQueueManager
import com.music.lake.musiclib.service.MusicPlayerService
import com.music.lake.musiclib.service.MusicServiceBinder
import com.music.lake.musiclib.utils.MusicLibLog
import java.io.File
import java.util.*

/**
 * Created by D22434 on 2017/9/20.
 */
class MusicPlayerManager private constructor() {
    private val TAG = "MusicPlayerManager"
    private var mBinder: MusicServiceBinder? = null
    private val config: MusicPlayerConfig? = null
    private var mToken: ServiceToken? = null
    private var request: MusicUrlRequest? = null
    lateinit var application: Application
    lateinit var appContext: Context
    var useExoPlayer: Boolean = false

    companion object {

        val instance: MusicPlayerManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            MusicPlayerManager()
        }


        @JvmStatic
        fun getProxy(): HttpProxyCacheServer {
            return if (instance.proxy == null) instance.newProxy().also {
                instance.proxy = it
            } else instance.proxy!!
        }

    }

    fun init(application: Application, config: MusicPlayerConfig) {
        this.application = application
        this.appContext = application.applicationContext
        request = config.request
        useExoPlayer = config.useExoPlayer
        mediaBrowser = MediaBrowserCompat(
            appContext,
            ComponentName(appContext, MusicPlayerService::class.java),
            mediaBrowserConnectionCallback, null
        ).apply { connect() }
    }

    fun initialize(
        context: Context?,
        callBack: BindServiceCallBack?
    ): ServiceToken? {
        mToken = bindToService(context, object : ServiceConnection {
            override fun onServiceConnected(
                componentName: ComponentName,
                iBinder: IBinder
            ) {
                mBinder = iBinder as MusicServiceBinder
                mBinder!!.setMusicRequestListener(request)
                callBack?.onSuccess()
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                mBinder = null
                callBack?.onFailed()
                MusicLibLog.d("BaseActivity", "onServiceDisconnected")
            }
        })
        return mToken
    }

    fun unInitialize(token: ServiceToken?) {
        token?.let { unbindFromService(it) }
    }

    fun bindToService(
        context: Context?,
        callback: ServiceConnection?
    ): ServiceToken? {
        //        Activity realActivity = ((Activity) context).getParent();
//        if (realActivity == null) {
//            realActivity = (Activity) context;
//        }
        try { //TODO 修复Android 8.0启动service异常报错 Not allowed to start service Intent { cmp=com.cyl.musiclake/.player.MusicPlayerService }: app is in background uid UidRecord{f44b6ce u0a208 TPSL idle procs:1 seq(0,0,0)}
            val contextWrapper = ContextWrapper(context)
            contextWrapper.startService(Intent(contextWrapper, MusicPlayerService::class.java))
            val binder =
                ServiceBinder(callback, contextWrapper.applicationContext)
            if (contextWrapper.bindService(
                    Intent().setClass(
                        contextWrapper,
                        MusicPlayerService::class.java
                    ), binder, 0
                )
            ) {
                return ServiceToken(contextWrapper)
            }
        } catch (e: Exception) {
        }
        return null
    }

    private fun unbindFromService(token: ServiceToken?) {
        if (token == null) {
            return
        }
//        val mContextWrapper = token.mWrappedContext
////        val binder =
////            mConnectionMap!![mContextWrapper] ?: return
//        mContextWrapper.unbindService(binder)
//        if (mConnectionMap!!.isEmpty()) {
//            mBinder = null
//        }
    }

    val isPlaybackServiceConnected: Boolean
        get() = mBinder != null

    //////////////////////////////////////////////////////////////////
//* Start
//* 播放相关接口
///////////////////////////////////////////////////////////////////
    fun playMusicById(index: Int) {
        if (mBinder != null) {
            mBinder!!.playMusicById(index)
        }
        transportControls?.skipToQueueItem(index.toLong())
    }

    fun playMusic(song: BaseMusicInfo) {
        if (mBinder != null) {
            mBinder!!.playMusic(song)
        }
        transportControls?.playFromUri(Uri.parse(song.uri), Bundle().apply {
            putString("title", "测试")
        })
    }

    fun playMusic(songs: MutableList<BaseMusicInfo>, index: Int) {
        MediaQueueManager.updatePlaylist(songs, index, "")
        transportControls?.playFromUri(Uri.parse(songs[index].uri), Bundle().apply {
            putString("title", "测试")
        })
    }

    fun updatePlaylist(songs: MutableList<BaseMusicInfo>, index: Int) {
        MediaQueueManager.updatePlaylist(songs, index, "")
    }

    fun playNextMusic() {
        transportControls?.skipToNext()
    }

    fun playPrevMusic() {
        transportControls?.skipToPrevious()
    }

    fun pausePlay() {
        transportControls?.pause();
    }

    fun play() {
        transportControls?.play();
    }

    fun stopPlay() {
        transportControls?.stop();
    }

    var loopMode: Int
        get() = if (mBinder != null) {
            mBinder!!.loopMode
        } else 0
        set(mode) {
            if (mBinder != null) {
                mBinder!!.loopMode = mode
            }
        }

    val nowPlayingMusic: BaseMusicInfo?
        get() = if (mBinder != null) {
            mBinder!!.nowPlayingMusic
        } else null

    val nowPlayingIndex: Int
        get() = if (mBinder != null) {
            mBinder!!.nowPlayingIndex
        } else 0

    fun removeFromPlaylist(position: Int) {
        if (mBinder != null) {
            mBinder!!.removeFromPlaylist(position)
        }
    }

    fun clearPlaylist() {
        if (mBinder != null) {
            mBinder!!.clearPlaylist()
        }
    }

    val playingPosition: Long
        get() = if (mBinder != null) {
            mBinder!!.playingPosition
        } else 0

    fun addMusicPlayerEventListener(listener: MusicPlayEventListener) {
        if (mBinder != null) {
            mBinder!!.addMusicPlayerEventListener(listener)
        }
    }

    fun removeMusicPlayerEventListener(listener: MusicPlayEventListener) {
        if (mBinder != null) {
            mBinder!!.removeMusicPlayerEventListener(listener)
        }
    }

    fun AudioSessionId(): Int {
        return if (mBinder != null) {
            mBinder!!.AudioSessionId()
        } else 0
    }

    fun seekTo(ms: Long) {
        if (mBinder != null) {
            mBinder!!.seekTo(ms)
        }
        transportControls?.seekTo(ms)
    }

    val playList: List<BaseMusicInfo>
        get() = if (mBinder != null) {
            mBinder!!.playList
        } else ArrayList()

    val isPlaying: Boolean
        get() = if (mBinder != null) {
            mBinder!!.isPlaying
        } else false

    val duration: Long
        get() = if (mBinder != null) {
            mBinder!!.duration
        } else 0

    fun showDesktopLyric(show: Boolean) {}
    //////////////////////////////////////////////////////////////////
//* End
//* 播放相关接口
///////////////////////////////////////////////////////////////////
    /**
     * AndroidVideoCache缓存设置
     */
    private var proxy: HttpProxyCacheServer? = null
    private val musicFilelCacheDir: String? = null
    var isHasCache = false

    private fun newProxy(): HttpProxyCacheServer {
        return HttpProxyCacheServer.Builder(application)
            .cacheDirectory(File(musicFilelCacheDir))
            .fileNameGenerator(CacheFileNameGenerator())
            .build()
    }

    inner class ServiceBinder(
        private val mCallback: ServiceConnection?,
        private val mContext: Context
    ) : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            mBinder = service as MusicServiceBinder
            mCallback?.onServiceConnected(className, service)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mCallback?.onServiceDisconnected(className)
            mBinder = null
        }

    }

    class ServiceToken(var mWrappedContext: ContextWrapper)

    /**
     * Media
     */
    private val mediaBrowserConnectionCallback by lazy {
        MediaBrowserConnectionCallback(appContext)
    }
    private var mediaBrowser: MediaBrowserCompat? = null
    private var transportControls: MediaControllerCompat.TransportControls? = null
    private var mediaController: MediaControllerCompat? = null

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
//            isConnected.postValue(false)
        }

        /**
         * Invoked when the connection to the media browser failed.
         */
        override fun onConnectionFailed() {
            MusicLibLog.d(TAG, "MediaBrowserConnectionCallback: onConnectionFailed")
//            isConnected.postValue(false)
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

}