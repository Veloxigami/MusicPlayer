package com.veloxigami.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.triggertrap.seekarc.SeekArc;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private ImageButton prevButton, playButton,nextButton,saveButton;
    public static SeekArc seekArc;
    public static TextView songText,currentTimeText,durationText;
    private DataStorage dataStorage;
    private DatabaseManger db;

    public final static String PLAY_PAUSE_BUTTON_PRESSED = "com.veloxigami.myapplication.playpausebtnpressed";
    public final static String PREV_BUTTON_PRESSED = "com.veloxigami.myapplication.prevbtnpressed";
    public final static String NEXT_BUTTON_PRESSED = "com.veloxigami.myapplication.nextbtnpressed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new DatabaseManger(this);

        dataStorage = new DataStorage(getApplicationContext(),db);
        prevButton = (ImageButton) findViewById(R.id.prevButton);
        playButton = (ImageButton) findViewById(R.id.playButton);
        nextButton = (ImageButton) findViewById(R.id.nextButton);
        saveButton = (ImageButton) findViewById(R.id.save_button);
        seekArc = (SeekArc) findViewById(R.id.seekArc);
        songText = (TextView) findViewById(R.id.song_display_text);
        currentTimeText = (TextView) findViewById(R.id.current_time_text);
        durationText = (TextView) findViewById(R.id.song_duration_text);


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
        saveButton.setOnClickListener(controlButtonsClickListener);

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
                case R.id.save_button:
                                        if(!MainFragment.playlist.isEmpty()){
                                             AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.myDialog));
                                            LayoutInflater inflater = getLayoutInflater();
                                            builder.setTitle("Save Playlist");
                                            final EditText inputText = (EditText) findViewById(R.id.playlist_name_text);

                                            builder.setView(inflater.inflate(R.layout.save_playlist_alert_dialog,null))
                                            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dataStorage.savePlaylist(inputText.getText().toString(),MainFragment.playlist);
                                                }
                                            })
                                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.cancel();
                                                }
                                            });
                                            builder.create();
                                            builder.show();
                                        }
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
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(songTextChangeReceiver);
        unregisterReceiver(pauseBroadcastReceiver);
        unregisterReceiver(playBroadcastReceiver);
        unregisterReceiver(playingBroadcastReceiver);
    }
}
