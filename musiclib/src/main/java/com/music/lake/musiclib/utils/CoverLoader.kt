package com.music.lake.musiclib.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.music.lake.musiclib.bean.BaseMusicInfo

/**
 * 专辑封面图片加载器
 * Glide加载异常处理
 */
object CoverLoader {
    private val TAG = "CoverLoader"

    fun getCoverUri(context: Context, albumId: String): String? {
        if (albumId == "-1") {
            return null
        }
        var uri: String? = null
        try {
            val cursor = context.contentResolver.query(
                    Uri.parse("content://media/external/audio/albums/$albumId"),
                    arrayOf("album_art"), null, null, null)
            if (cursor != null) {
                cursor.moveToNext()
                uri = cursor.getString(0)
                cursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return uri
    }

    /**
     * 获取专辑图url，
     *
     * @param baseMusicInfo 音乐
     * @param isBig 是否是大图
     * @return
     */
    private fun getCoverUriByMusic(baseMusicInfo: BaseMusicInfo, isBig: Boolean): String? {
        return if (baseMusicInfo.coverBig != null && isBig) {
            baseMusicInfo.coverBig
        } else if (baseMusicInfo.coverUri != null) {
            baseMusicInfo.coverUri
        } else {
            baseMusicInfo.coverSmall
        }
    }


    /**
     * 显示小图
     *
     * @param mContext
     * @param baseMusicInfo
     * @param callBack
     */
    fun loadImageViewByMusic(mContext: Context, baseMusicInfo: BaseMusicInfo?, callBack: ((Bitmap) -> Unit)?) {
        if (baseMusicInfo == null) return
        val url = getCoverUriByMusic(baseMusicInfo, false)
        loadBitmap(mContext, url, callBack)
    }

    /**
     * 显示播放页大图
     *
     * @param mContext
     */
    fun loadBigImageView(mContext: Context?, baseMusicInfo: BaseMusicInfo?, callBack: ((Bitmap) -> Unit)?) {
        if (baseMusicInfo == null) return
        if (mContext == null) return
    }

    fun loadBigImageView(mContext: Context, baseMusicInfo: BaseMusicInfo?, imageView: ImageView?) {
        if (baseMusicInfo == null || imageView == null) return
    }

    fun loadBigImageView(mContext: Context, url: String?, vendor: String?, imageView: ImageView?) {
        if (imageView == null) return
    }

    /**
     * 显示图片
     *
     * @param mContext
     * @param url
     * @param imageView
     */
    fun loadImageView(mContext: Context?, url: String?, imageView: ImageView?) {
        if (mContext == null) return
        if (imageView == null) return
    }

    fun loadImageView(mContext: Context?, url: String?, defaultUrl: Int, imageView: ImageView) {
        if (mContext == null) return
    }

    /**
     * 根据id显示
     *
     * @param mContext
     * @param albumId
     * @param callBack
     */
    fun loadBitmapById(mContext: Context, albumId: String, callBack: ((Bitmap) -> Unit)?) {
        loadBitmap(mContext, getCoverUri(mContext, albumId), callBack)
    }

    /**
     * 返回bitmap
     *
     * @param mContext
     * @param url
     * @param callBack
     */
    fun loadBitmap(mContext: Context?, url: String?, callBack: ((Bitmap) -> Unit)?) {
        if (mContext == null) return

    }

    /**
     * 返回Drawable
     *
     * @param mContext
     * @param url
     * @param callBack
     */
    fun loadDrawable(mContext: Context?, url: String?, callBack: ((Drawable) -> Unit)?) {
        if (mContext == null) return
    }

}
