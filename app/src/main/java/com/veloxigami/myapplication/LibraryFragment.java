package com.veloxigami.myapplication;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;


public class LibraryFragment extends Fragment implements LibraryAdapter.libraryAdapterInterface {

    private ArrayList<MusicFile> musicFiles = new ArrayList();
    private MusicFile file;
    private Context context;
    private  RecyclerView libraryRecyclerView;
    private LibraryAdapter libraryAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private GetFiles getFiles;
    private MainFragment mainFragment;

    public final static String BTN_CLICK_BROADCAST = "com.veloxigami.myapplication.itemBtnClick";
    public final static String VIEW_CLICK_BROADCAST = "com.veloxigami.myapplication.itemViewClick";

    public ArrayList<MusicFile> getMusicFiles() {
        return musicFiles;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainFragment = new MainFragment();
    }

    public LibraryAdapter getLibraryAdapter() {
        return libraryAdapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.library_fragment, container, false);
        context = container.getContext();

        libraryRecyclerView = (RecyclerView) rootView.findViewById(R.id.lib_recycler_view);

        layoutManager = new LinearLayoutManager(getActivity());

        libraryRecyclerView.setLayoutManager(layoutManager);



        libraryAdapter = new LibraryAdapter(musicFiles, getActivity());

        libraryAdapter.setAdapterInterface(this);

        libraryRecyclerView.setAdapter(libraryAdapter);
        Log.v("TAG","attached");
        new GetFiles().execute();
        return rootView;
    }

    @Override
    public void onItemBtnClick(int position) {

        MainFragment.playlist.add(musicFiles.get(position));
        Log.v("TAG","Item Added");
        MainFragment.playingAdapter.notifyDataSetChanged();
        //new DataStorage(getActivity()).storeAudio(MainFragment.playlist);

        Intent itemBtnClickBroadcast = new Intent(BTN_CLICK_BROADCAST);
//        itemBtnClickBroadcast.putExtra("position",position);
        getActivity().sendBroadcast(itemBtnClickBroadcast);

//        Log.v("currentPosition",new DataStorage(getActivity()).loadFile().size()+"");
    }

    @Override
    public void onItemViewClick(int position) {
        MainFragment.playlist.clear();
        MainFragment.playlist.add(musicFiles.get(position));
        MainFragment.playingAdapter.notifyDataSetChanged();
        //new DataStorage(getActivity()).storeAudio(MainFragment.playlist);

       Intent itemViewClickBroadcast = new Intent(VIEW_CLICK_BROADCAST);
//       itemViewClickBroadcast.putExtra("position",position);
       getActivity().sendBroadcast(itemViewClickBroadcast);
 //       Log.v("currentPosition",new DataStorage(getActivity()).loadFile().size()+"");
    }


    private class GetFiles extends AsyncTask<Void, Void, Void>{


        @Override
        protected Void doInBackground(Void... voids) {
            ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();
            Cursor cursor = resolver.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    null,
                    null,
                    null);

            cursor.moveToFirst();

            while(cursor.moveToNext()){
                String duration="";
                //AlbumArt
                long albumId = (long) cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                //Bitmap bm = convertToImage(imagePath);

                Uri sArtworkUri = Uri
                        .parse("content://media/external/audio/albumart");
                Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId);

                //Logger.debug(albumArtUri.toString());
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(
                            getActivity().getContentResolver(), albumArtUri);
                bitmap = Bitmap.createScaledBitmap(bitmap, 30, 30, true);

                } catch (FileNotFoundException exception) {
                    exception.printStackTrace();
                    bitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                            R.drawable.ic_launcher);
                } catch (IOException e) {

                    e.printStackTrace();
                }

                //AlbumName
                String albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AlbumColumns.ALBUM));

                //ArtistName
                String artistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.ArtistColumns.ARTIST));


                long durationInMs=0;
                //Duration
                if(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)) != null){
                     durationInMs = Long.parseLong(cursor
                            .getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)));

                    int durationInMin = ((int) durationInMs/1000) / 60;
                    int durationInSec = ((int) durationInMs/1000) % 60;

                    duration = (""+ durationInMin+ ":" + String.format("%02d",durationInSec)) ;
                }

                //SongName
                String songName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE));

                //FilePath
                String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA));

                file = new MusicFile(filePath,albumName,artistName,songName,duration,bitmap);

                musicFiles.add(file);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {

            //getLibraryAdapter().notifyDataSetChanged();
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            getLibraryAdapter().notifyDataSetChanged();
            super.onPostExecute(aVoid);
        }

    }

    public Bitmap convertToImage(String imagePath){
        Bitmap albumArt = BitmapFactory.decodeFile(imagePath);
        return albumArt;
    }
}


