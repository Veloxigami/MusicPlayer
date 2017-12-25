package com.veloxigami.myapplication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import java.util.ArrayList;


public class MainFragment extends Fragment implements LibraryFragment.libraryFragmentInterface{


    private RecyclerView recyclerView;
    private NowPlayingAdapter playingAdapter;
    private RecyclerView.LayoutManager manager;
    public static ArrayList<MusicFile> playlist = new ArrayList<>();
    private ImageButton prevButton,playButton,nextButton;
    private SeekBar seekBar;
    public static MediaPlayerService playerService;
    public static boolean serviceBound = false;
    private DataStorage storage;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.veloxigami.myapplication";
    private ArrayList<MusicFile>  data = new ArrayList<>();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        data = new LibraryFragment().getMusicFiles();
        //new LibraryFragment().setLibraryFragmentInterface(this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.main_fragment, container, false);
        playlist.clear();

        prevButton = (ImageButton) rootView.findViewById(R.id.prevButton);
        playButton = (ImageButton) rootView.findViewById(R.id.playButton);
        nextButton = (ImageButton) rootView.findViewById(R.id.nextButton);
        seekBar = (SeekBar) rootView.findViewById(R.id.seekbar);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview);

        manager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(manager);;
        this.playingAdapter = new NowPlayingAdapter(playlist,getActivity());
        recyclerView.setAdapter(playingAdapter);

        return rootView;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            playerService = binder.getService();
            serviceBound = true;

            Toast.makeText(getActivity(),"Media Player Active", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    public void playAudio(int audioIndex){
        if (!serviceBound){

            storage = new DataStorage(getActivity());
            storage.storeAudio(playlist);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(getActivity(),MediaPlayerService.class);
            getActivity().startService(playerIntent);
            getActivity().bindService(playerIntent,serviceConnection,Context.BIND_AUTO_CREATE);
        }
        else{

            storage = new DataStorage(getActivity());
            storage.storeAudio(playlist);
            storage.storeAudioIndex(audioIndex);

            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            getActivity().sendBroadcast(broadcastIntent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(outState);
    }


    public void saveCurrentPlaying(ArrayList<MusicFile> list,String title){
        new DataStorage(getActivity()).savePlaylist(title,list);
    }

    public ArrayList<MusicFile> getPlaylist() {
        return playlist;
    }

    public ImageButton getPrevButton() {
        return prevButton;
    }

    public void setPrevButton(ImageButton prevButton) {
        this.prevButton = prevButton;
    }

    public ImageButton getPlayButton() {
        return playButton;
    }

    public void setPlayButton(ImageButton playButton) {
        this.playButton = playButton;
    }

    public ImageButton getNextButton() {
        return nextButton;
    }

    public void setNextButton(ImageButton nextButton) {
        this.nextButton = nextButton;
    }

    public SeekBar getSeekBar() {
        return seekBar;
    }

    public void setSeekBar(SeekBar seekBar) {
        this.seekBar = seekBar;
    }

    @Override
    public void onBtnClick(int position) {
        playlist.add(data.get(position));
        Log.v("TAG","Item Added");
        playingAdapter.notifyDataSetChanged();
        if(MainFragment.playlist.size() == 1){
            playAudio(0);
        }
    }

    @Override
    public void onViewClick(int position) {
        playlist.clear();
        playlist.add(data.get(position));
        playingAdapter.notifyDataSetChanged();
        playAudio(0);
    }

/*
    @Override
    public void onItemBtnClick(int position) {
        playlist.add(data.get(position));
        Log.v("TAG","Item Added");
        playingAdapter.notifyDataSetChanged();
        if(MainFragment.playlist.size() == 1){
            playAudio(0);
        }
    }

    @Override
    public void onItemViewClick(int position) {
        playlist.clear();
        playlist.add(data.get(position));
        playingAdapter.notifyDataSetChanged();
        playAudio(0);
    }*/
}
