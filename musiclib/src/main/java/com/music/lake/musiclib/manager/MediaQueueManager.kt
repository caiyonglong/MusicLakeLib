package com.music.lake.musiclib.manager

import com.music.lake.musiclib.bean.BaseMusicInfo
import com.music.lake.musiclib.utils.MusicLibLog

/**
 * Created by master on 2018/5/14.
 */

object MediaQueueManager {
    /**
     * 播放模式 0：顺序播放，1：单曲循环，2：随机播放
     */
    const val PLAY_MODE_LOOP = 0
    const val PLAY_MODE_REPEAT = 1
    const val PLAY_MODE_RANDOM = 2
    const val PLAY_MODE = "PLAY_MODE"
    //播放模式
    private var playingModeId = 0

    private var mNowPlayingIndex = -1
    private var mNowPlayingMusic: BaseMusicInfo? = null

    /**
     * 总共多少首歌曲
     */
    private var orderList = mutableListOf<Int>()
    private var saveList = mutableListOf<Int>()
    private var randomPosition = 0

    val mPlaylist = mutableListOf<BaseMusicInfo>()
    val mHistoryPos = mutableListOf<Int>()

    fun getNowPlayingIndex(): Int {
        return mNowPlayingIndex
    }

    fun isInValidIndex(): Boolean {
        if (mNowPlayingIndex >= mPlaylist.size || mNowPlayingIndex < 0) {
            return false
        }
        return true
    }

    fun getNowPlayingMusic(): BaseMusicInfo? {
        mNowPlayingMusic = mPlaylist[mNowPlayingIndex]
        return mNowPlayingMusic
    }

    /**
     * 更新播放模式
     */
    fun setLoopMode(loopMode: Int) {
        playingModeId = loopMode
    }

    /**
     * 更新播放模式
     */
    fun updateLoopMode(): Int {
        playingModeId = (playingModeId + 1) % 3
        return playingModeId
    }

    /**
     * 获取播放模式id
     */
    fun getLoopMode(): Int {
        return playingModeId
    }

    private fun initOrderList(total: Int) {
        orderList.clear()
        for (i in 0 until total) {
            orderList.add(i)
        }

        /**
         * 更新
         */
        if (getLoopMode() == PLAY_MODE_RANDOM) {
            orderList.shuffle()
            randomPosition = 0
            printOrderList(-1)
        }
    }

    /**
     * 获取下一首位置
     *
     * @return isAuto 是否自动下一曲
     */
    fun getNextPosition(isAuto: Boolean?): Int {
        if (mPlaylist.size == 1) {
            mNowPlayingIndex = 0
            return 0
        }
        initOrderList(mPlaylist.size)
        if (playingModeId == PLAY_MODE_REPEAT && isAuto!!) {
            mNowPlayingIndex = if (mNowPlayingIndex < 0) {
                0
            } else {
                mNowPlayingIndex
            }
        } else if (playingModeId == PLAY_MODE_RANDOM) {
            printOrderList(orderList[randomPosition])
            saveList.add(orderList[randomPosition])
            mNowPlayingIndex = orderList[randomPosition]
        } else {
            if (mNowPlayingIndex == mPlaylist.size - 1) {
                mNowPlayingIndex = 0
            } else if (mNowPlayingIndex < mPlaylist.size - 1) {
                mNowPlayingIndex += 1
            } else {
                mNowPlayingIndex = mPlaylist.size - 1
            }
        }
        return mNowPlayingIndex
    }

    /**
     * 获取下一首位置
     *
     * @return isAuto 是否自动下一曲
     */
    fun getPreviousPosition(): Int {
        if (mPlaylist.size == 1) {
            mNowPlayingIndex = 0
            return 0
        }
        getLoopMode()
        if (playingModeId == PLAY_MODE_REPEAT) {
            mNowPlayingIndex = if (mNowPlayingIndex < 0) {
                0
            } else {
                mNowPlayingIndex
            }
        } else if (playingModeId == PLAY_MODE_RANDOM) {
            randomPosition = if (saveList.size > 0) {
                saveList.last()
                saveList.removeAt(saveList.lastIndex)
            } else {
                randomPosition--
                if (randomPosition < 0) {
                    randomPosition = mPlaylist.size - 1
                }
                orderList[randomPosition]
            }
            printOrderList(randomPosition)
            mNowPlayingIndex = randomPosition
        } else {
            if (mNowPlayingIndex == 0) {
                mNowPlayingIndex = mPlaylist.size - 1
            } else if (mNowPlayingIndex > 0) {
                mNowPlayingIndex -= 1
            }
        }
        return mNowPlayingIndex
    }

    fun updatePlaylist(list: MutableList<BaseMusicInfo>, id: Int) {
        mPlaylist.clear()
        mPlaylist.addAll(list)
        mNowPlayingIndex = id
    }

    fun removeFromPlaylist(index: Int) {
    }

    fun setNowPlayingIndex(position: Int): Int {
        mNowPlayingIndex = if (position >= mPlaylist.size || position == -1) {
            getNextPosition(true)
        } else {
            position
        }
        return mNowPlayingIndex;
    }

    fun setNowPlayingMusic(musicInfo: BaseMusicInfo?) {
        musicInfo?.let {
            mNowPlayingMusic = it
            mPlaylist.add(it)
            mNowPlayingIndex = mPlaylist.size - 1
        }
    }

    /**
     * 打印当前顺序
     */
    private fun printOrderList(cur: Int) {
        MusicLibLog.d("PlayQueueManager", "$orderList --- $cur")
    }

    fun clear() {
        mPlaylist.clear()
        mHistoryPos.clear()
    }
}
