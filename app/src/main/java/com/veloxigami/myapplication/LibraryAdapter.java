package com.veloxigami.myapplication;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Anil on 20-12-2017.
 */

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.MyViewHolder> {

    ArrayList<MusicFile> list = new ArrayList<>();
    Context context;
    MainFragment mainFragment;
    MediaPlayerService playerService;

    public interface libraryAdapterInterface {
        void onItemBtnClick(int position);
        void onItemViewClick(int position);
    }

    private libraryAdapterInterface adapterInterface;

    public LibraryAdapter(ArrayList<MusicFile> list, Context context) {
        this.list = list;
        this.context = context;
        /*if(context instanceof libraryAdapterInterface){
            this.adapterInterface = (libraryAdapterInterface) context;

        }*/

        mainFragment = new MainFragment();
        playerService = new MediaPlayerService();
    }

    public void setAdapterInterface(libraryAdapterInterface adapterInterface){
        this.adapterInterface = adapterInterface;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View v = layoutInflater.inflate(R.layout.library_item,parent,false);
        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {

        holder.albumArt.setImageBitmap(list.get(position).getAlbumArt());
        holder.songName.setText(list.get(position).getSongName());
        holder.duration.setText(list.get(position).getDuration());
        holder.albumName.setText(list.get(position).getAlbumName());
        holder.addSongBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterInterface.onItemBtnClick(position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder{

        ImageView albumArt;
        TextView songName, albumName, duration;
        ImageButton addSongBtn;

        public MyViewHolder(View itemView) {
            super(itemView);
            albumArt = (ImageView) itemView.findViewById(R.id.lib_album_art);
            songName = (TextView) itemView.findViewById(R.id.lib_song_name);
            albumName = (TextView) itemView.findViewById(R.id.lib_album_name);
            duration = (TextView) itemView.findViewById(R.id.lib_duration_text);
            addSongBtn = (ImageButton) itemView.findViewById(R.id.lib_add_btn);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapterInterface.onItemViewClick(getAdapterPosition());
                }
            });
        }
    }
}
