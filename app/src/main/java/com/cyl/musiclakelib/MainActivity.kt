package com.cyl.musiclakelib

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.music.lake.musiclib.MusicPlayerManager
import com.music.lake.musiclib.bean.BaseMusicInfo
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val musicInfo = BaseMusicInfo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initData();
        initListener()
    }

    private fun initData() {
        musicInfo.uri =
            "http://m10.music.126.net/20200409004740/8cde83b912b721fc646246917a7bba3e/ymusic/1606/426f/10a6/a01cace34f2df73c384bbcfe3e30b827.mp3"
    }

    private fun initListener() {
        initBtn.setOnClickListener {
            verifyStoragePermissions(this);
            titleTv.text = musicInfo.title
        }
        prevBtn.setOnClickListener {
            MusicPlayerManager.instance.playPrevMusic()
        }
        nextBtn.setOnClickListener {
            MusicPlayerManager.instance.playNextMusic()
        }
        playBtn.setOnClickListener {
            MusicPlayerManager.instance.play()
        }
        pauseBtn.setOnClickListener {
            MusicPlayerManager.instance.pausePlay()
        }
    }

    private val REQUEST_EXTERNAL_STORAGE = 1

    //需要检查的权限
    private val mPermissionList = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE //获取电话状态
    )

    fun verifyStoragePermissions(activity: Activity) { // Check if we have write permission
        ActivityCompat.requestPermissions(
            activity,
            mPermissionList,
            REQUEST_EXTERNAL_STORAGE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            Log.d("MainActivity", "授权 $grantResults");
            MusicPlayerManager.instance.playMusic(musicInfo)
        }
    }
}
