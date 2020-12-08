package com.cyl.musiclakelib

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.music.lake.musiclib.MusicPlayerManager
import com.music.lake.musiclib.bean.BaseMusicInfo
import com.music.lake.musiclib.listener.MusicPlayEventListener
import com.music.lake.musiclib.utils.MusicLibLog
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {
    val musicInfo = BaseMusicInfo()
    val musiclist = mutableListOf<BaseMusicInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initData();
        initListener()
        verifyStoragePermissions(this);
    }

    private fun initData() {
        val data = arrayListOf(
            "/storage/emulated/0/musicLake/Music/金玟岐 - 岁月神偷.mp3",
            "/storage/emulated/0/musicLake/Music/李荣浩 - 耳朵.mp3",
            "/storage/emulated/0/musicLake/Music/李荣浩 - 麻雀.mp3"
        )
        musicInfo.title = "岁月神偷"
        musicInfo.uri = data[0]

        var content = ""
        musiclist.clear()
        for (i in 0 until data.size) {
            if (File(data[i]).exists()) {
                MusicLibLog.d("判断文件是存在" + File(data[i]).exists())
            }
            musiclist.add(BaseMusicInfo().apply {
                title = data[i].split("/")[6]
                uri = data[i]
            })
            content += data[i].split("/")[6] + "\n"
        }
        contentTv.text = content
    }

    private fun initListener() {
        MusicPlayerManager.getControl()
            .addMusicPlayerEventListener(object : MusicPlayEventListener {
                override fun onMetaChanged(musicInfo: BaseMusicInfo?) {
                    runOnUiThread {
                        titleTv.text = musicInfo?.title
                    }
                }

                override fun onLoading(isLoading: Boolean) {
                }

                override fun onPlaybackProgress(
                    curPosition: Long,
                    duration: Long,
                    bufferPercent: Int
                ) {
                }

                override fun onAudioSessionId(audioSessionId: Int) {
                }

                override fun onPlayCompletion() {
                }

                override fun onPlayStart() {
                }

                override fun onPlayerStateChanged(isPlaying: Boolean) {
                }

                override fun onPlayStop() {
                }

                override fun onPlayerError(error: Throwable?) {
                }

                override fun onUpdatePlayList(playlist: MutableList<BaseMusicInfo>) {
                }

            })
        initBtn.setOnClickListener {
            titleTv.text = musicInfo.title
            MusicPlayerManager.getControl().updatePlaylist(musiclist, 0)
        }
        prevBtn.setOnClickListener {
            MusicPlayerManager.getControl().playPrevMusic()
        }
        nextBtn.setOnClickListener {
            MusicPlayerManager.getControl().playNextMusic()
        }
        playBtn.setOnClickListener {
            MusicPlayerManager.getControl().playMusic(musicInfo)
        }
        pauseBtn.setOnClickListener {
            MusicPlayerManager.getControl().pausePlay()
        }
    }

    private val REQUEST_EXTERNAL_STORAGE = 1

    //需要检查的权限
    private val mPermissionList = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    fun verifyStoragePermissions(activity: Activity) { // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                mPermissionList,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            Log.d("MainActivity", "授权 $grantResults");
            MusicPlayerManager.getControl().playMusic(musicInfo)
        }
    }
}
