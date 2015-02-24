package com.ncode.android.musicplayer;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.MediaStore;

public class Album {
    String id;
    String artist;
    String album;

    public Album(String id, String album, String artist) {
        this.id = id;
        this.artist = artist;
        this.album = album;
    }

    public String getId() {
        return id;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public Uri getURI() {
        return ContentUris.withAppendedId(
        		MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, Long.getLong(id));
    }
}
