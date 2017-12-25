package com.veloxigami.myapplication;

import android.graphics.Bitmap;
import android.media.Image;
import android.widget.ImageView;

/**
 * Created by Anil on 16-12-2017.
 */

public class MusicFile {

    private String data, albumName, artistName, songName, duration;
    private Bitmap albumArt;


    public MusicFile(String data, String albumName, String artistName, String songName, String duration, Bitmap albumArt) {
        this.data = data;
        this.albumName = albumName;
        this.artistName = artistName;
        this.songName = songName;
        this.duration = duration;
        this.albumArt = albumArt;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Bitmap getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(Bitmap albumArt) {
        this.albumArt = albumArt;
    }
}
