package com.cyl.musiclakelib

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.music.lake.musiclib.MusicPlayerManager
import com.music.lake.musiclib.bean.BaseMusicInfo
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
            "https://audio04.dmhmusic.com/71_53_T10040589078_128_4_1_0_sdk-cpm/cn/0206/M00/90/77/ChR47F1_nqiAfD0hAD_MGBybIdk026.mp3?xcode=e321016c44af692e61f811e67a3def379630aa6"
    }

    private fun initListener() {
        initBtn.setOnClickListener {
            MusicPlayerManager.getInstance().playMusic(musicInfo)
            titleTv.text = musicInfo.title
        }
        prevBtn.setOnClickListener {
            MusicPlayerManager.getInstance().playPrevMusic()
        }
        nextBtn.setOnClickListener {
            MusicPlayerManager.getInstance().playNextMusic()
        }
        playPauseBtn.setOnClickListener {
            MusicPlayerManager.getInstance().pausePlay()
        }
    }
}
