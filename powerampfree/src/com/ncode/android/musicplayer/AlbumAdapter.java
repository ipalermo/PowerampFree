package com.ncode.android.musicplayer;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.app.Activity;

public class AlbumAdapter extends ArrayAdapter<Album> {

	Context context; 
    int layoutResourceId;    
    ArrayList<Album> data = null;
    
    public AlbumAdapter(Context context, int layoutResourceId, ArrayList<Album> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        AlbumHolder holder = null;
        
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            
            holder = new AlbumHolder();
            holder.txtAlbum = (TextView)row.findViewById(R.id.txtAlbum);
            holder.txtArtist = (TextView)row.findViewById(R.id.txtArtist);
            
            row.setTag(holder);
        }
        else
        {
            holder = (AlbumHolder)row.getTag();
        }
        
        Album alb = data.get(position);
        holder.txtAlbum.setText(alb.album);
        holder.txtArtist.setText(alb.artist);
        
        return row;
    }
    
    static class AlbumHolder
    {
        TextView txtAlbum;
        TextView txtArtist;
    }
}