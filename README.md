[![](https://jitpack.io/v/caiyonglong/MusicLakeLib.svg)](https://jitpack.io/#caiyonglong/MusicLakeLib)

# 前言
一个丰富的音乐播放封装库，针对快速集成音频播放功能，抽离MusicLake播放模块，

# 特点
轻松播放本地和网络音频
提供丰富的API方法来轻松实现各种功能。
使用 MusicPlayer、ExoPlayer 作为底层播放器，可根据需求自动配置。

# ExoPlayer版本


# 使用

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
Step 2. Add the dependency
```
	dependencies {
	        implementation 'com.github.caiyonglong:MusicLakeLib:master-SNAPSHOT'
	}
```
