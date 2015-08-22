package com.example.android.spotifystreamer;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class MediaPlayerService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener {

    public static final String LOG_TAG = MainActivityFragment.class.getSimpleName();
    public static final String URL = "URL";
    private final IBinder mBinder = new LocalBinder(this);
    PlaybackState mPlaybackState = PlaybackState.END;
    private NotificationManager mNM;
    private MediaPlayer mMediaPlayer = null;
    private WifiManager.WifiLock mWifiLock = null;
    private String mUrl;
    private int mTrackPosition = -1;
    private int mSeekTo = 0;
    private ActionOnPrepare mActionOnPrepare = ActionOnPrepare.START;

    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void onCreate() {
        mNM = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
    }

    public void onDestroy() {
        release();
    }

    public PlaybackState getPlaybackState() {
        return mPlaybackState;
    }

    public int getTrackPosition() {
        return mTrackPosition;
    }

    public void setTrackPosition(int position) {
        mTrackPosition = position;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "Received start id " + startId + ": " + intent);
        if ((mMediaPlayer == null) || (!isPlaying())) {
            fullReset(false);
        }
        return 2;
    }

    public void fullReset(boolean forceRelease) {
        if ((mPlaybackState == PlaybackState.END) || (forceRelease)) {
            release();
            mMediaPlayer = new MediaPlayer();
        }
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);

        mMediaPlayer.reset();
        mPlaybackState = PlaybackState.IDLE;

        mMediaPlayer.setAudioStreamType(3);

        mMediaPlayer.setWakeMode(getApplicationContext(), 1);
        if (mWifiLock == null) {
            mWifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE))
                    .createWifiLock(1, "spotifystreamer_lock");
        }
        if ((mWifiLock != null) && (!mWifiLock.isHeld())) {
            mWifiLock.acquire();
        }
    }

    public boolean isPlaying() {
        boolean ret = false;
        try {
            ret = (mPlaybackState == PlaybackState.STARTED) && (mMediaPlayer.isPlaying());
        } catch (IllegalStateException ex) {
        }
        return ret;
    }

    public void reset() {
        mMediaPlayer.reset();
        mPlaybackState = PlaybackState.IDLE;
    }

    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mPlaybackState = PlaybackState.END;
    }

    public void setDataSource(String url) {
        mUrl = null;
        try {
            mMediaPlayer.setDataSource(url);
            mUrl = url;
            mPlaybackState = PlaybackState.INITIALIZED;
        } catch (IllegalArgumentException e) {
            Log.i(LOG_TAG, "IllegalArgumentException in setDataSource()");
        } catch (IOException e) {
            Log.i(LOG_TAG, "IOException in setDataSource()");
        }
    }

    public void onCompletion(MediaPlayer mp) {
        mPlaybackState = PlaybackState.PLAYBACK_COMPLETED;
    }

    public boolean canStart() {
        return (mPlaybackState == PlaybackState.PAUSED)
                || (mPlaybackState == PlaybackState.STARTED)
                || (mPlaybackState == PlaybackState.PLAYBACK_COMPLETED)
                || (mPlaybackState == PlaybackState.PREPARED);
    }

    public boolean canPause() {
        return mPlaybackState == PlaybackState.STARTED;
    }

    public boolean canPrepare() {
        return (mPlaybackState == PlaybackState.INITIALIZED)
                || (mPlaybackState == PlaybackState.STOPPED);
    }

    public boolean canSeekTo() {
        return (mPlaybackState == PlaybackState.PAUSED)
                || (mPlaybackState == PlaybackState.STARTED)
                || (mPlaybackState == PlaybackState.PREPARED)
                || (mPlaybackState == PlaybackState.PLAYBACK_COMPLETED);
    }

    public void seekTo(int msec) {
        if (canSeekTo()) {
            try {
                mMediaPlayer.seekTo(msec);
            } catch (IllegalStateException ex) {
                Log.i(LOG_TAG, "IllegalArgumentException on seekTo(" + msec + ")");
                mPlaybackState = PlaybackState.ILLEGAL_STATE_EXCEPTION;
            }
        }
    }

    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public void start(int seekTo) {
        mSeekTo = seekTo;
        if ((mWifiLock != null) && (!mWifiLock.isHeld())) {
            mWifiLock.acquire();
        }
        try {
            if (canStart()) {
                mMediaPlayer.start();
                mPlaybackState = PlaybackState.STARTED;
                if (mSeekTo > 0) {
                    seekTo(mSeekTo);
                    mSeekTo = 0;
                }
            } else if (canPrepare()) {
                mPlaybackState = PlaybackState.PREPARING;
                mActionOnPrepare = ActionOnPrepare.START;
                mMediaPlayer.prepareAsync();
            }
        } catch (IllegalStateException ex) {
            Log.i(LOG_TAG, "IllegalArgumentException on start() or prepareAsync()");
            mPlaybackState = PlaybackState.ILLEGAL_STATE_EXCEPTION;
        }
    }

    public void pause(int seekTo) {
        mSeekTo = seekTo;
        try {
            if (canPause()) {
                mMediaPlayer.pause();
                mPlaybackState = PlaybackState.PAUSED;
            }
            if (mSeekTo > 0) {
                if (canSeekTo()) {
                    seekTo(mSeekTo);
                    mSeekTo = 0;
                } else if (canPrepare()) {
                    mActionOnPrepare = ActionOnPrepare.PAUSE;
                    mPlaybackState = PlaybackState.PREPARING;
                    mMediaPlayer.prepareAsync();
                }
            }
        } catch (IllegalStateException ex) {
            Log.i(LOG_TAG, "IllegalArgumentException on pause()");
            mPlaybackState = PlaybackState.ILLEGAL_STATE_EXCEPTION;
        }
        if (mWifiLock != null) {
            mWifiLock.release();
        }
    }

    private void stop() {
        try {
            mMediaPlayer.stop();
        } catch (IllegalStateException ex) {
            Log.i(LOG_TAG, "IllegalArgumentException on stop()");
            mPlaybackState = PlaybackState.ILLEGAL_STATE_EXCEPTION;
        }
        if (mWifiLock != null) {
            mWifiLock.release();
        }
    }

    public void onPrepared(MediaPlayer player) {
        try {
            if (mActionOnPrepare == ActionOnPrepare.START) {
                mMediaPlayer.start();
                mPlaybackState = PlaybackState.STARTED;
            } else if (mActionOnPrepare == ActionOnPrepare.PAUSE) {
                mPlaybackState = PlaybackState.PREPARED;
            }
            if (mSeekTo > 0) {
                seekTo(mSeekTo);
                mSeekTo = 0;
            }
        } catch (IllegalStateException ex) {
            Log.i(LOG_TAG, "IllegalArgumentException in onPrepared()");
            mPlaybackState = PlaybackState.ILLEGAL_STATE_EXCEPTION;
        }
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.i(LOG_TAG, "onError: what = " + what + ", extra = " + extra);
        reset();
        return false;
    }

    public enum PlaybackState {
        IDLE,
        INITIALIZED,
        PREPARING,
        PREPARED,
        STARTED,
        PAUSED,
        STOPPED,
        PLAYBACK_COMPLETED,
        ILLEGAL_STATE_EXCEPTION,
        END;
    }

    enum ActionOnPrepare {
        START,
        PAUSE,
    }

    public class LocalBinder extends Binder {
        MediaPlayerService mPlayerService;

        public LocalBinder(MediaPlayerService playerService) {
            mPlayerService = playerService;
        }

        MediaPlayerService getService() {
            return mPlayerService;
        }
    }
}
