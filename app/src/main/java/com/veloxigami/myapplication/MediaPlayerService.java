package com.veloxigami.myapplication;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Anil on 23-12-2017.
 */

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnInfoListener,
        AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mediaPlayer;
    private MusicFile currentMedia;
    private ArrayList<MusicFile> playList;
    private int  currentFileIndex = -1;
    private int resume = 0;
    private AudioManager audioManager;

    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    private void callStateListener(){
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener(){
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state){
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if(mediaPlayer!=null){
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (mediaPlayer!=null){
                            if(ongoingCall)
                            ongoingCall = false;
                            resumeMedia();
                        }
                        break;
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };

        telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void initMediaPlayer(){
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);

        mediaPlayer.reset();

        playList = new ArrayList<>();
        playList = MainFragment.playlist;

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try{
            mediaPlayer.setDataSource(currentMedia.getData());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }

        mediaPlayer.prepareAsync();
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void playMedia(){
        if (!mediaPlayer.isPlaying()){
            mediaPlayer.start();
        }
    }

    public void pauseMedia(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            resume = mediaPlayer.getCurrentPosition();
        }
    }

    public void stopMedia(){
        if (mediaPlayer == null) return;
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
    }

    public void resumeMedia(){
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.seekTo(resume);
            mediaPlayer.start();
        }
    }

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentFileIndex = new DataStorage(getApplicationContext()).loadAudioIndex();
            if(currentFileIndex != -1 && currentFileIndex < playList.size()){
                currentMedia = playList.get(currentFileIndex);
            }else {
                stopSelf();
            }

            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
            /*updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);*/
        }
    };

    private void register_playNewAudio(){
        IntentFilter filter = new IntentFilter(MainFragment.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio,filter);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        callStateListener();

        registerAudioOutputChange();

        register_playNewAudio();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try{
            currentMedia = MainFragment.playlist.get(new DataStorage(getApplicationContext()).loadAudioIndex());
        }catch (NullPointerException e){
            e.printStackTrace();
            stopSelf();
        }

        if(requestAudioFocus() == false)
            stopSelf();

        if (currentMedia.getData() != null && currentMedia.getData() !="")
            initMediaPlayer();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mediaPlayer!=null){
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();

        if(phoneStateListener != null){
            telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_NONE);
        }

//        removeNotification();

        unregisterReceiver(audioOutputChange);
        unregisterReceiver(playNewAudio);

        new DataStorage(getApplicationContext()).clearCachedAudioPlaylist();
    }

    private BroadcastReceiver audioOutputChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseMedia();
//            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerAudioOutputChange(){
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(audioOutputChange, intentFilter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                //resume play
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(resume);
                    mediaPlayer.start();
                }
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                //Lost mediaPlayer access by some other app for indefinite time
                if (mediaPlayer.isPlaying()) {
                    resume = mediaPlayer.getCurrentPosition();
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // during a call
                if (mediaPlayer.isPlaying())
                    pauseMedia();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // during an incoming notification
                if (mediaPlayer.isPlaying())
                    mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus(){

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            return true;

        return false;
    }

    private boolean removeAudioFocus(){
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {

        stopMedia();

        stopSelf();

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what){
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer error","MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK");
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer error", "MEDIA ERROR SERVER DIED");
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer error","MEDIA ERROR UNKNOWN");
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService(){
            return MediaPlayerService.this;
        }
    }
}
