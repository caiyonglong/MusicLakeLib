
```
1629-1629/com.cyl.musiclakelib E/EventLogger: internalError [eventTime=104.09, mediaPos=0.00, window=0, period=0, loadError]
    com.google.android.exoplayer2.upstream.HttpDataSource$HttpDataSourceException: Unable to connect to http://audio04.dmhmusic.com/71_53_T10040589078_128_4_1_0_sdk-cpm/cn/0206/M00/90/77/ChR47F1_nqiAfD0hAD_MGBybIdk026.mp3?xcode=e321016c44af692e61f811e67a3def379630aa6
        at com.google.android.exoplayer2.upstream.DefaultHttpDataSource.open(DefaultHttpDataSource.java:282)
        at com.google.android.exoplayer2.upstream.DefaultDataSource.open(DefaultDataSource.java:177)
        at com.google.android.exoplayer2.upstream.StatsDataSource.open(StatsDataSource.java:83)
        at com.google.android.exoplayer2.source.ProgressiveMediaPeriod$ExtractingLoadable.load(ProgressiveMediaPeriod.java:956)
        at com.google.android.exoplayer2.upstream.Loader$LoadTask.run(Loader.java:391)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
        at java.lang.Thread.run(Thread.java:919)
     Caused by: java.io.IOException: Cleartext HTTP traffic to audio04.dmhmusic.com not permitted
        at com.android.okhttp.HttpHandler$CleartextURLFilter.checkURLPermitted(HttpHandler.java:124)
        at com.android.okhttp.internal.huc.HttpURLConnectionImpl.execute(HttpURLConnectionImpl.java:462)
        at com.android.okhttp.internal.huc.HttpURLConnectionImpl.connect(HttpURLConnectionImpl.java:131)
        at com.google.android.exoplayer2.upstream.DefaultHttpDataSource.makeConnection(DefaultHttpDataSource.java:550)
        at com.google.android.exoplayer2.upstream.DefaultHttpDataSource.makeConnection(DefaultHttpDataSource.java:439)
        at com.google.android.exoplayer2.upstream.DefaultHttpDataSource.open(DefaultHttpDataSource.java:280)
        at com.google.android.exoplayer2.upstream.DefaultDataSource.open(DefaultDataSource.java:177) 
        at com.google.android.exoplayer2.upstream.StatsDataSource.open(StatsDataSource.java:83) 
        at com.google.android.exoplayer2.source.ProgressiveMediaPeriod$ExtractingLoadable.load(ProgressiveMediaPeriod.java:956) 
        at com.google.android.exoplayer2.upstream.Loader$LoadTask.run(Loader.java:391) 
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167) 
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641) 
        at java.lang.Thread.run(Thread.java:919) 

```