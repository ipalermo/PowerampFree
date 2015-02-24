package com.ncode.android.musicplayer;

import android.content.ContentUris;
import android.net.Uri;

public class Item {
    long id;
    String title;
    long artistId;
    String artist;
    long albumId;
    String album;
    long duration;

    public Item(long id, String title, long artistId, String artist, long albumId, String album, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.artistId = artistId;
        this.albumId = albumId;
        this.album = album;
        this.duration = duration;
    }

    public long getId() {
        return id;
    }

    public String getArtist() {
        return artist;
    }
    
    public long getArtistId() {
        return artistId;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbum() {
        return album;
    }
    
    public long getAlbumId() {
        return albumId;
    }
    
    public long getDuration() {
        return duration;
    }

    public Uri getURI() {
        return ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }
}

