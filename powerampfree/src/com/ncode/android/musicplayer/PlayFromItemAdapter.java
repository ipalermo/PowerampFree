package com.ncode.android.musicplayer;

import java.util.ArrayList;

import com.ncode.android.musicplayer.ItemAdapter.ItemHolder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;

public class PlayFromItemAdapter extends ArrayAdapter<PlayFromItem> {

	Context context; 
    int layoutResourceId;    
    ArrayList<PlayFromItem> data = null;
    
    public PlayFromItemAdapter(Context context, int layoutResourceId, ArrayList<PlayFromItem> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder = null;
        
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            
            holder = new ViewHolder();
            holder.image = (ImageView)row.findViewById(R.id.playfromimageView);
            holder.text = (TextView)row.findViewById(R.id.playfromtextView);
            
            row.setTag(holder);
        }
        else
        {	
        	holder = (ViewHolder)row.getTag();
        }
        
        PlayFromItem item = data.get(position);
        holder.image.setImageResource(item.getImageId());
        holder.text.setText(item.getText());
        
        return row;
    }
    
    static class ViewHolder
    {
        ImageView image;
        TextView text;
    }
}