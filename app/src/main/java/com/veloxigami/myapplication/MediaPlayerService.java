package com.veloxigami.myapplication;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

import com.triggertrap.seekarc.SeekArc;

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

    public static final String Broadcast_NEXT_SONG = "com.veloxigami.myapplication.nextsongupdate";
    public static final String Broadcast_PREV_SONG = "com.veloxigami.myapplication.prevsongupdate";
    public static final String Broadcast_PLAY_SONG = "com.veloxigami.myapplication.play";
    public static final String Broadcast_PAUSE_SONG = "com.veloxigami.myapplication.pause";

    private final Handler handler = new Handler();

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



        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try{
            mediaPlayer.setDataSource(currentMedia.getData());
            currentFileIndex = MainFragment.currentFile;
            Toast.makeText(getApplicationContext(),"Playlist Size: "+MainFragment.playlist.size() +"\nSong No.: "+(currentFileIndex+1) ,Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        callStateListener();

        registerAudioOutputChange();

        register_playNewAudio();

        registerStopMediaBroadcast();

        registerUpdatePlaylistReceiver();

        registerPlayButtonBroadcast();

        registerPrevButtonBroadcast();

        registerNextButtonBroadcast();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try{
            playList = new ArrayList<>();
            playList = MainFragment.playlist;
            currentMedia = MainFragment.playlist.get(MainFragment.currentFile);
        }catch (NullPointerException e){
            e.printStackTrace();
            stopSelf();
        }

        if(requestAudioFocus() == false)
            stopSelf();

        if (currentMedia.getData() != null && currentMedia.getData() !="") {
            initMediaPlayer();
        }
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
        unregisterReceiver(stopMediaBroadcast);
        unregisterReceiver(updatePlaylistReceiver);
        unregisterReceiver(playButtonBroadcast);
        unregisterReceiver(prevButtonBroadcast);
        unregisterReceiver(nextButtonBroadcast);

        //new DataStorage(getApplicationContext()).clearCachedAudioPlaylist();
    }

    private final Runnable updatePositionRunnable = new Runnable() {
        @Override
        public void run() {
            MainActivity.seekArc.setProgress(mediaPlayer.getCurrentPosition());
        }
    };

    public void updateSeekArc(){
        handler.postDelayed(updatePositionRunnable,100);
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

    private SeekArc.OnSeekArcChangeListener seekArcChangeListener = new SeekArc.OnSeekArcChangeListener() {
        @Override
        public void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser) {
            updateSeekArc();
        }

        @Override
        public void onStartTrackingTouch(SeekArc seekArc) {
            handler.removeCallbacks(updatePositionRunnable);
        }

        @Override
        public void onStopTrackingTouch(SeekArc seekArc) {
            handler.removeCallbacks(updatePositionRunnable);
            //int totalDuration = mediaPlayer.getDuration();
            int currentPosition = seekArc.getProgress();

            mediaPlayer.seekTo(currentPosition);
            updateSeekArc();
        }
    };

    /*private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            updateSeekArc();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            handler.removeCallbacks(updatePositionRunnable);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            handler.removeCallbacks(updatePositionRunnable);
            //int totalDuration = mediaPlayer.getDuration();
            int currentPosition = seekBar.getProgress();

            mediaPlayer.seekTo(currentPosition);
            updateSeekArc();
        }
    };*/

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

        currentFileIndex = MainFragment.currentFile;

        if(currentFileIndex < playList.size()){
            currentFileIndex = ++currentFileIndex;
            if(currentFileIndex != -1 && currentFileIndex < playList.size()){
                currentMedia = playList.get(currentFileIndex);
                //new DataStorage(getApplicationContext()).storeAudioIndex(currentFileIndex);
                MainFragment.currentFile = currentFileIndex;
                Intent nextPlaying = new Intent(Broadcast_NEXT_SONG);
                sendBroadcast(nextPlaying);
            }else {
                stopSelf();
            }

            stopMedia();
            mediaPlayer.stop();
            mediaPlayer.reset();
            initMediaPlayer();
        }
        else{
            stopMedia();

            stopSelf();
        }


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

        MainActivity.seekArc.setMax(mediaPlayer.getDuration());
        updateSeekArc();
        MainActivity.seekArc.setOnSeekArcChangeListener(seekArcChangeListener);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService(){
            return MediaPlayerService.this;
        }
    }

    /**
     * play,stop,resume,pause
     */
    public void playMedia(){
        if (!mediaPlayer.isPlaying()){
            mediaPlayer.start();


            Intent pause = new Intent(Broadcast_PLAY_SONG);
            sendBroadcast(pause);
        }
    }

    public void pauseMedia(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            resume = mediaPlayer.getCurrentPosition();

            Intent pause = new Intent(Broadcast_PAUSE_SONG);
            sendBroadcast(pause);
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


    /*************
     *
     *  BROADCAST RECEIVERS and OTHER INTERACTION SECTION BELOW
     *
     * **********/

    /**
     * play newfile in mediaplayer broadcast receiver
     */
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentFileIndex = MainFragment.currentFile;
            if(currentFileIndex != -1 && currentFileIndex < playList.size()){
                currentMedia = playList.get(currentFileIndex);
            }else {
                stopSelf();
            }

            stopMedia();
            mediaPlayer.stop();
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
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * audio output change broadcast receiver
     */
    ////////////////////////////////////////////////////////////////////////////////////////////////
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
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * stop mediaplayer broadcast
     */
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private BroadcastReceiver stopMediaBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopMedia();
        }
    };

    private void registerStopMediaBroadcast(){
        IntentFilter intentFilter = new IntentFilter(MainFragment.Broadcast_STOP_PLAYING_FOR_CHANGE);
        registerReceiver(stopMediaBroadcast,intentFilter);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * control buttons broadcast recievers
     */
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private BroadcastReceiver playButtonBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!mediaPlayer.isPlaying()){
                playMedia();
            }
            else{
                pauseMedia();
            }
        }
    };

    private BroadcastReceiver prevButtonBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(currentFileIndex != 0 && currentFileIndex < playList.size()){
                currentMedia = MainFragment.playlist.get(currentFileIndex-1);
                //new DataStorage(getApplicationContext()).storeAudioIndex(currentFileIndex-1);
                MainFragment.currentFile = currentFileIndex -1;
                stopMedia();
                initMediaPlayer();
                Intent prevPlaying = new Intent(Broadcast_PREV_SONG);
                sendBroadcast(prevPlaying);
            }else{
                Toast.makeText(getApplicationContext(),"First song playing!",Toast.LENGTH_SHORT).show();
            }
        }
    };

    private BroadcastReceiver nextButtonBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(currentFileIndex != -1 && currentFileIndex < playList.size()-1){
                currentMedia = MainFragment.playlist.get(currentFileIndex+1);
                //new DataStorage(getApplicationContext()).storeAudioIndex(currentFileIndex+1);
                MainFragment.currentFile = currentFileIndex + 1;
                stopMedia();
                initMediaPlayer();
                Intent nextPlaying = new Intent(Broadcast_NEXT_SONG);
                sendBroadcast(nextPlaying);
            }
            else {
                Toast.makeText(getApplicationContext(),"Last song playing!",Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void registerPlayButtonBroadcast(){
        IntentFilter play = new IntentFilter(MainActivity.PLAY_PAUSE_BUTTON_PRESSED);
        registerReceiver(playButtonBroadcast,play);
    }

    private void registerPrevButtonBroadcast(){
        IntentFilter prev = new IntentFilter(MainActivity.PREV_BUTTON_PRESSED);
        registerReceiver(prevButtonBroadcast,prev);
    }

    private void registerNextButtonBroadcast(){
        IntentFilter next = new IntentFilter(MainActivity.NEXT_BUTTON_PRESSED);
        registerReceiver(nextButtonBroadcast,next);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * LibraryFragment update playlist
     */
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private BroadcastReceiver updatePlaylistReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playList = MainFragment.playlist;
        }
    };

    private void registerUpdatePlaylistReceiver(){
        IntentFilter libViewUpdate = new IntentFilter(LibraryFragment.VIEW_CLICK_BROADCAST);
        IntentFilter libBtnUpdate = new IntentFilter(LibraryFragment.BTN_CLICK_BROADCAST);
        IntentFilter mainFragViewUpdate = new IntentFilter(MainFragment.Broadcast_TAPS_UPDATE);
        IntentFilter mainFragBtnUpdate = new IntentFilter(MainFragment.Broadcast_TAPS_UPDATE);
        registerReceiver(updatePlaylistReceiver,libViewUpdate);
        registerReceiver(updatePlaylistReceiver,libBtnUpdate);
        registerReceiver(updatePlaylistReceiver,mainFragViewUpdate);
        registerReceiver(updatePlaylistReceiver,mainFragBtnUpdate);

    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
}
