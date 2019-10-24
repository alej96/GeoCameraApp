package com.example.geocamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ImageListAdapter extends BaseAdapter {

    private Context context;
    private int layout;
    private ArrayList<ImageTaken> imageTakenArrayList;

    public ImageListAdapter(Context context, int layout, ArrayList<ImageTaken> imageTakenArrayList) {
        this.context = context;
        this.layout = layout;
        this.imageTakenArrayList = imageTakenArrayList;
    }

    @Override
    public int getCount() {
        return imageTakenArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return imageTakenArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    private class ViewHolder{

        ImageView  imageView;
        TextView txtCity, txtTime, txtLatitude, txtLongitude;

    }
    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {

        View row = view;
        ViewHolder holder = new ViewHolder();

        if(row == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(layout, null);

            holder.txtCity = (TextView) row.findViewById(R.id.txtCity);
            holder.txtLatitude = (TextView) row.findViewById(R.id.txtLatitude);
            holder.txtLongitude = (TextView) row.findViewById(R.id.txtLongitude);
            holder.txtTime = (TextView) row.findViewById(R.id.txtTime);
            holder.imageView = (ImageView) row.findViewById(R.id.cameraImg);
            row.setTag(holder);
        }
        else {
            holder = (ViewHolder) row.getTag();
        }


        ImageTaken imageTaken = imageTakenArrayList.get(position);

        holder.txtCity.setText(imageTaken.getCity());
        holder.txtLatitude.setText(imageTaken.getLatitude());
        holder.txtLongitude.setText(imageTaken.getLongitude());
        holder.txtTime.setText(imageTaken.getTime());

        byte[] cameraImg =  imageTaken.getImage();
        Bitmap bitmap  = BitmapFactory.decodeByteArray(cameraImg, 0,cameraImg.length);
        holder.imageView.setImageBitmap(bitmap);


        return row;
    }
}
