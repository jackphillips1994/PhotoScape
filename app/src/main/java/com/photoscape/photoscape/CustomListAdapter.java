package com.photoscape.photoscape;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomListAdapter extends ArrayAdapter {
    private final Activity context;
    private final ArrayList<String> itemname;
    private final ArrayList imgid;
    private final ArrayList<String> description;

    public CustomListAdapter(Activity context, ArrayList<String> itemname, ArrayList imgid,
                             ArrayList<String> description) {
        super(context, R.layout.pin_list, itemname);

        this.context = context;
        this.itemname = itemname;
        this.imgid = imgid;
        this.description = description;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.pin_list, null,true);

        TextView txtTitle = (TextView) rowView.findViewById(R.id.item);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        TextView extratxt = (TextView) rowView.findViewById(R.id.textView1);

        txtTitle.setText(itemname.get(position));
        //imageView.setImageResource(imgid[position]);
        extratxt.setText("Description " + description.get(position));

        return rowView;

    }
}
