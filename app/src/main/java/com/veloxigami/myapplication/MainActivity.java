package com.veloxigami.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.triggertrap.seekarc.SeekArc;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private ImageButton prevButton, playButton,nextButton;
    public static SeekArc seekArc;
    public static TextView songText,currentTimeText,durationText;
    //private DataStorage dataStorage;

    public final static String PLAY_PAUSE_BUTTON_PRESSED = "com.veloxigami.myapplication.playpausebtnpressed";
    public final static String PREV_BUTTON_PRESSED = "com.veloxigami.myapplication.prevbtnpressed";
    public final static String NEXT_BUTTON_PRESSED = "com.veloxigami.myapplication.nextbtnpressed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        prevButton = (ImageButton) findViewById(R.id.prevButton);
        playButton = (ImageButton) findViewById(R.id.playButton);
        nextButton = (ImageButton) findViewById(R.id.nextButton);
        seekArc = (SeekArc) findViewById(R.id.seekArc);
        songText = (TextView) findViewById(R.id.song_display_text);
        currentTimeText = (TextView) findViewById(R.id.current_time_text);
        durationText = (TextView) findViewById(R.id.song_duration_text);
        //dataStorage = new DataStorage(this);


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragments(new MainFragment(),"Now Playing");
        viewPagerAdapter.addFragments(new LibraryFragment(),"Library");
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);

        prevButton.setOnClickListener(controlButtonsClickListener);
        playButton.setOnClickListener(controlButtonsClickListener);
        nextButton.setOnClickListener(controlButtonsClickListener);

        registerPlayPauseBroadcast();
        registerSongTextChangeBroadcast();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("servicestate",MainFragment.serviceBound);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        MainFragment.serviceBound = savedInstanceState.getBoolean("servicestate");
    }
    private Button.OnClickListener controlButtonsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.playButton: playButtonBroadcast();
                    break;
                case R.id.prevButton: prevButtonBroadcast();
                    break;
                case R.id.nextButton: nextButtonBroadcast();
                    break;
            }
        }
    };



    private void playButtonBroadcast(){
        Intent intent = new Intent(PLAY_PAUSE_BUTTON_PRESSED);
        sendBroadcast(intent);
    }

    private void prevButtonBroadcast(){
        Intent intent = new Intent(PREV_BUTTON_PRESSED);
        sendBroadcast(intent);
    }

    private void nextButtonBroadcast(){
        Intent intent = new Intent(NEXT_BUTTON_PRESSED);
        sendBroadcast(intent);
    }

    private BroadcastReceiver playingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playButton.setImageResource(android.R.drawable.ic_media_pause);
        }
    };

    private BroadcastReceiver playBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playButton.setImageResource(android.R.drawable.ic_media_pause);
        }
    };

    private BroadcastReceiver pauseBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playButton.setImageResource(android.R.drawable.ic_media_play);
        }
    };

    private BroadcastReceiver songTextChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            songText.setText(MainFragment.playlist.get(MainFragment.currentFile).getSongName());
        }
    };

    private void registerSongTextChangeBroadcast(){
      IntentFilter songTextChangeBroadcast = new IntentFilter(MainFragment.Broadcast_SONG_TEXT_CHANGE);
      registerReceiver(songTextChangeReceiver,songTextChangeBroadcast);
    }

    private void registerPlayPauseBroadcast(){
        IntentFilter nowplaying = new IntentFilter(MainFragment.Broadcast_PLAY_BTN_CHANGE);
        IntentFilter play = new IntentFilter(MediaPlayerService.Broadcast_PLAY_SONG);
        IntentFilter pause = new IntentFilter(MediaPlayerService.Broadcast_PAUSE_SONG);
        registerReceiver(playBroadcastReceiver,play);
        registerReceiver(playingBroadcastReceiver,nowplaying);
        registerReceiver(pauseBroadcastReceiver,pause);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(songTextChangeReceiver);
        unregisterReceiver(pauseBroadcastReceiver);
        unregisterReceiver(playBroadcastReceiver);
        unregisterReceiver(playingBroadcastReceiver);
    }
}
