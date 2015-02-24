package com.ncode.android.musicplayer;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.app.Activity;

public class ItemAdapter extends ArrayAdapter<Item> {

	Context context; 
    int layoutResourceId;    
    ArrayList<Item> data = null;
    
    public ItemAdapter(Context context, int layoutResourceId, ArrayList<Item> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ItemHolder holder = null;
        
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            
            holder = new ItemHolder();
            holder.txtSongTitle = (TextView)row.findViewById(R.id.txtSongTitle);
            holder.txtArtist = (TextView)row.findViewById(R.id.txtArtist);
            
            row.setTag(holder);
        }
        else
        {
            holder = (ItemHolder)row.getTag();
        }
        
        Item song = data.get(position);
        holder.txtSongTitle.setText(song.title);
        holder.txtArtist.setText(song.artist);
        
        return row;
    }
    
    static class ItemHolder 
    {
        TextView txtSongTitle;
        TextView txtArtist;
    }
}