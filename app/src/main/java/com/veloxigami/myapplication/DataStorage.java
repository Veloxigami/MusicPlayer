package com.veloxigami.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by Anil on 24-12-2017.
 */

public class DataStorage {

    private final String STORAGE = "com.veloxigami.myapplication.STORAGE";
    private SharedPreferences preferences;
    private Context context;

    public DataStorage(Context context){
        this.context = context;
    }

    public void storeAudio(ArrayList<MusicFile> arrayList){
        if(context == null) {
            Log.v("TAG", "Error");
            return;
        }
        preferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString("audioArrayList",json);
        editor.apply();
    }

    public void lastPlayed(ArrayList<MusicFile> lastPlayed){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(lastPlayed);
        editor.putString("lastplayed",json);
    }

    public void savePlaylist(String playlistName, ArrayList<MusicFile> currentList){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(currentList);
        editor.putString(playlistName,json);
    }

    public ArrayList<MusicFile> loadFile(){
        preferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("audioArrayList",null);
        Type type = new TypeToken<ArrayList<MusicFile>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public  ArrayList<MusicFile> loadPlaylist(String playlistName){
        preferences =context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString(playlistName, null);
        Type type= new TypeToken<ArrayList<MusicFile>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void storeAudioIndex(int index){
        preferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioIndex", index);
        editor.apply();
    }

    public int loadAudioIndex(){
        preferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        return preferences.getInt("audioIndex",-1);
    }

    public void clearCachedAudioPlaylist(){
        preferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("audioArrayList");
        editor.remove("audioIndex");
        editor.commit();
    }
}
