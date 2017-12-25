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
 * Created by Anil on 16-12-2017.
 */

public class NowPlayingAdapter extends RecyclerView.Adapter<NowPlayingAdapter.PlayViewHolder> {

    ArrayList<MusicFile> list = new ArrayList<>();
    Context context;

    public interface NowPlayingInterface
    {
        void onSongClick(int position);
    }

    NowPlayingInterface nowPlayingInterface;

    public NowPlayingAdapter( ArrayList<MusicFile> list, Context context) {
        this.list = list;
        this.context = context;
        if(context instanceof NowPlayingAdapter.NowPlayingInterface){
            nowPlayingInterface = (NowPlayingInterface) context;
        }
    }

    @Override
    public PlayViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.playlist_item,parent,false);
        PlayViewHolder viewHolder = new PlayViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(PlayViewHolder holder, final int position) {

        holder.songName.setText(list.get(position).getSongName());
        holder.albumArt.setImageBitmap(list.get(position).getAlbumArt());
        holder.albumName.setText(list.get(position).getAlbumName());
        holder.duration.setText(list.get(position).getDuration());
        holder.removeItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(list.size() == 1){
                    //stop playing
                    //clear playlist
                }

                if(new DataStorage(context).loadAudioIndex() == position){
                    //remove
                    //playnextaudio
                }

                if(new DataStorage(context).loadAudioIndex() > position){
                    //remove
                    //audioIndex--
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class PlayViewHolder extends RecyclerView.ViewHolder{

        TextView songName, albumName, duration;
        ImageView albumArt;
        ImageButton removeItem;

        public PlayViewHolder(View itemView) {
            super(itemView);
            songName = (TextView) itemView.findViewById(R.id.song_name);
            albumName = (TextView) itemView.findViewById(R.id.album_name);
            duration = (TextView) itemView.findViewById(R.id.duration_text);
            albumArt = (ImageView) itemView.findViewById(R.id.album_art);
            removeItem = (ImageButton) itemView.findViewById(R.id.add_btn);
        }
    }
}




















/*

    ArrayList<MediaStore.Audio> list = new ArrayList<>();
    Context context;
    private static final String TAG = "operation";

    public interface NowPlayingInterface {
        void onSongClick(int position);
    }

    public NowPlayingInterface nowPlayingInterface;

    public NowPlayingAdapter(ArrayList<Audio> list, Context context) {
        this.list = list;
        this.context = context;
        if(context instanceof NowPlayingAdapter.NowPlayingInterface){
            nowPlayingInterface = (NowPlayingInterface) context;
        }
    }

    @Override
    public NowPlayingAdapter.PlayViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v  = inflater.inflate(R.layout.play_list_item,parent,false);
        PlayViewHolder holder = new PlayViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(NowPlayingAdapter.PlayViewHolder holder, int position) {
        final int pos = position;
        holder.songName.setText(list.get(position).getTitle());
        holder.duration.setText(list.get(position).getDuration());
        holder.album.setText(list.get(position).getAlbum());
        holder.removeItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Audio item = HomeFragment.audioList.get(pos);
                HomeFragment.audioList.remove(item);
                Log.e(TAG, "Remove: " );
                HomeFragment.nowPlayingAdapter.notifyDataSetChanged();
                Log.e(TAG, "Change: " );


            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class PlayViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView songName, album, duration;
        ImageButton removeItem;
        RelativeLayout layout;

        public PlayViewHolder(View itemView) {
            super(itemView);
            layout = (RelativeLayout) itemView.findViewById(R.id.play_list_item);
            songName = (TextView) itemView.findViewById(R.id.songname);
            album = (TextView) itemView.findViewById(R.id.album);
            duration = (TextView) itemView.findViewById(R.id.duration);
            removeItem = (ImageButton) itemView.findViewById(R.id.remove_item_button);
        }

        @Override
        public void onClick(View v) {
            if(v==layout && nowPlayingInterface!=null)
                nowPlayingInterface.onSongClick(getAdapterPosition());
        }
    }
}
*/
