package com.veloxigami.myapplication;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import com.triggertrap.seekarc.SeekArc;

import java.util.ArrayList;


public class MainFragment extends Fragment implements NowPlayingAdapter.NowPlayingInterface {


    private RecyclerView recyclerView;
    public static NowPlayingAdapter playingAdapter;
    private RecyclerView.LayoutManager manager;
    public static ArrayList<MusicFile> playlist = new ArrayList<>();
    public static MediaPlayerService playerService;
    public static boolean serviceBound = false;
    private DataStorage storage;
    private ArrayList<MusicFile> data = new ArrayList<>();
    private int currentFile;

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.veloxigami.myapplication.playnewaudio";
    public final static String Broadcast_PLAY_BTN_CHANGE = "com.veloxigami.myapplication.playbuttonchange";
    public final static String Broadcast_STOP_PLAYING_FOR_CHANGE = "com.veloxigami.myapplication.stopplayingforchange";
    public final static String Broadcast_SONG_TEXT_CHANGE = "com.veloxigami.myapplication.songtextchange";
    public final static String Broadcast_TAPS_UPDATE = "com.veloxigami.myapplication.tapUpdate";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        data = new LibraryFragment().getMusicFiles();
        registerBtnClickBroadcast();
        registerViewClickBroadcast();
        registerNextPlayingBroadcast();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.main_fragment, container, false);
        playlist.clear();
       /* if(new DataStorage(getActivity()).loadPlaylist("lastplayed") != null){
            playlist = new DataStorage(getActivity()).loadPlaylist("lastplayed");
        }*/
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview);
        storage = new DataStorage(getActivity());

        manager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(manager);
        ;
        playingAdapter = new NowPlayingAdapter(playlist, getActivity());
        playingAdapter.setNowPlayingInterface(this);
        recyclerView.setAdapter(playingAdapter);

        return rootView;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            playerService = binder.getService();
            serviceBound = true;

            Toast.makeText(getActivity(), "Media Player Active", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };


    public void playAudio(int audioIndex) {
        if (!serviceBound) {

            // storage = new DataStorage(getActivity());
            storage.storeAudio(playlist);
            storage.storeAudioIndex(audioIndex);
            serviceBound = true;
            Log.v("TAG", "Creating new instance");
            Intent playerIntent = new Intent(getActivity(), MediaPlayerService.class);
            getActivity().startService(playerIntent);
            getActivity().bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {

            //storage = new DataStorage(getActivity());
            storage.storeAudio(playlist);
            storage.storeAudioIndex(audioIndex);

            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            Log.v("TAG", "Broadcasting");
            getActivity().sendBroadcast(broadcastIntent);
        }
        currentFile = new DataStorage(getActivity()).loadAudioIndex();
        Intent playingBroadcast = new Intent(Broadcast_PLAY_BTN_CHANGE);
        getActivity().sendBroadcast(playingBroadcast);
        Intent nextPlayingBroadcastMain = new Intent(Broadcast_SONG_TEXT_CHANGE);
        getActivity().sendBroadcast(nextPlayingBroadcastMain);
    }


    public void saveCurrentPlaying(ArrayList<MusicFile> list, String title) {
        new DataStorage(getActivity()).savePlaylist(title, list);
    }

    public ArrayList<MusicFile> getPlaylist() {
        return playlist;
    }


    private BroadcastReceiver btnClickBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MainFragment.playlist.size() == 1) {
                playAudio(0);
            }
        }
    };

    private void registerBtnClickBroadcast() {
        IntentFilter intentFilter = new IntentFilter(LibraryFragment.BTN_CLICK_BROADCAST);
        getActivity().registerReceiver(btnClickBroadcast, intentFilter);
    }

    private BroadcastReceiver viewClickBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playAudio(0);
        }
    };

    private void registerViewClickBroadcast() {
        IntentFilter intentFilter = new IntentFilter(LibraryFragment.VIEW_CLICK_BROADCAST);
        getActivity().registerReceiver(viewClickBroadcast, intentFilter);
    }

    private void sendStopBroadcast() {
        Intent intent = new Intent(Broadcast_STOP_PLAYING_FOR_CHANGE);
        getActivity().sendBroadcast(intent);
    }

    private void sendTapUpdateBroadcast() {
        Intent intent = new Intent(Broadcast_TAPS_UPDATE);
        getActivity().sendBroadcast(intent);
    }

    @Override
    public void onSongClick(int position) {
        playAudio(position);

        sendTapUpdateBroadcast();
    }

    @Override
    public void onBtnClick(int position) {
        if (playlist.size() == 1) {
            //stop playing
            sendStopBroadcast();
            //clear playlist
            playlist.clear();
            playingAdapter.notifyDataSetChanged();
            Log.v("REMOVE BTN", "Single element");
            Log.v("currentPosition", currentFile + "");

        } else if (currentFile == position) {
            //remove
            sendStopBroadcast();
            playlist.remove(position);
            playingAdapter.notifyDataSetChanged();
            //playnextaudio
            if (position == playlist.size()) {
                Log.v("REMOVE BTN", "Current playing is last element of playlist");
                Log.v("currentPosition", currentFile + "");
                currentFile = currentFile - 1;
                storage.storeAudioIndex(currentFile);
                playAudio(position - 1);
            } else {
                playAudio(position);
                Log.v("REMOVE BTN", "Current playing removed");
                Log.v("currentPosition", currentFile + "");
            }
        } else if (currentFile > position) {
            //remove
            playlist.remove(position);
            playingAdapter.notifyDataSetChanged();
            //audioIndex--
            currentFile = currentFile - 1;
            storage.storeAudioIndex(currentFile);

            Log.v("REMOVE BTN", "Item above current playing removed");
            Log.v("currentPosition", currentFile + "");
        }

        sendTapUpdateBroadcast();
    }


    private BroadcastReceiver nextPlayingBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentFile = storage.loadAudioIndex();
            Log.v("currentPosition", currentFile + "");
            Intent nextPlayingBroadcastMain = new Intent(Broadcast_SONG_TEXT_CHANGE);
            getActivity().sendBroadcast(nextPlayingBroadcastMain);
        }
    };

    private BroadcastReceiver prevPlayingBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentFile = storage.loadAudioIndex();
            Log.v("currentPosition", currentFile + "");
            Intent prevPlayingBroadcastMain = new Intent(Broadcast_SONG_TEXT_CHANGE);
            getActivity().sendBroadcast(prevPlayingBroadcastMain);
        }
    };

    private void registerNextPlayingBroadcast() {
        IntentFilter intentFilter1 = new IntentFilter(MediaPlayerService.Broadcast_NEXT_SONG);
        getActivity().registerReceiver(nextPlayingBroadcast, intentFilter1);

        IntentFilter intentFilter2 = new IntentFilter(MediaPlayerService.Broadcast_PREV_SONG);
        getActivity().registerReceiver(prevPlayingBroadcast,intentFilter2);
    }


/*
    private BroadcastReceiver prevBtnClickedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };
    private BroadcastReceiver nextBtnClickedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    private void registerPrevBtnClickReceiver(){
        IntentFilter prevBtnClick = new IntentFilter(MainActivity.PREV_BUTTON_PRESSED);
        IntentFilter nextBtnClick = new IntentFilter(MainActivity.NEXT_BUTTON_PRESSED);
        getActivity().registerReceiver(prevBtnClickedReceiver,prevBtnClick);
        getActivity().registerReceiver(nextBtnClickedReceiver,nextBtnClick);
    }*/
}
