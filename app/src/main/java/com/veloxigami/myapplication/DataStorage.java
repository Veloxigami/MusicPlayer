package com.veloxigami.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

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
    private DatabaseManger dbHelper;

   public DataStorage(Context context, DatabaseManger db){
        this.context = context;
        this.dbHelper = db;
    }

    public ArrayList<String> loadPlaylistName(){
       SQLiteDatabase db = dbHelper.getReadableDatabase();
       String[] projection = {DatabaseManger.COLUMN_NAME};
       String sortOrder = DatabaseManger.COLUMN_NAME + "ASC";

       Cursor cursor = db.query(DatabaseManger.TABLE_NAME,projection,
               null,null,null,null,sortOrder);
       cursor.moveToFirst();
       ArrayList<String> playlistNames = new ArrayList<>();
       while(cursor != null){
           String name =  cursor.getString(cursor.getColumnIndex(DatabaseManger.COLUMN_NAME));
           playlistNames.add(name);
       }
       cursor.close();
       return playlistNames;
    }

    public void savePlaylist(String playlistName, ArrayList<MusicFile> currentList){
        /*preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();*/

        Gson gson = new Gson();
        String json = gson.toJson(currentList);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseManger.COLUMN_NAME,playlistName);
        values.put(DatabaseManger.COLUMN_DATA,json);
        try {
            long newRowId = db.insert(DatabaseManger.TABLE_NAME,null,values);
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(context,"Playlist name exists",Toast.LENGTH_LONG).show();
        }
        //editor.putString(playlistName,json);
    }

    public  ArrayList<MusicFile> loadPlaylist(String playlistName){
        //preferences =context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);

        //String json = preferences.getString(playlistName, null);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {DatabaseManger.COLUMN_DATA};
        String selection = DatabaseManger.COLUMN_NAME + " = ?";
        String[] selectionArgs = {playlistName};

        Cursor cursor = db.query(DatabaseManger.TABLE_NAME,projection
                ,selection,selectionArgs,null,null,null);

        cursor.moveToFirst();

        String json = cursor.getString(cursor.getColumnIndex(DatabaseManger.COLUMN_DATA));
        Gson gson = new Gson();
        Type type= new TypeToken<ArrayList<MusicFile>>(){}.getType();
        cursor.close();
        return gson.fromJson(json, type);
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

    public ArrayList<MusicFile> loadFile(){
        preferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("audioArrayList",null);
        Type type = new TypeToken<ArrayList<MusicFile>>(){}.getType();
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
        editor.apply();
    }

}
