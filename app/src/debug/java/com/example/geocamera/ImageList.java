package com.example.geocamera;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.GridView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.nio.file.Files;
import java.io.IOException;
import java.util.ArrayList;

public class ImageList extends AppCompatActivity {


    GridView gridView;
    ArrayList<ImageTaken> list;
    ImageListAdapter adapter = null;
    Cursor cursor;
    DatabaseHelper databaseHelper;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_gallery);

        gridView = (GridView) findViewById(R.id.gridView);
        list = new ArrayList<>();
        adapter = new ImageListAdapter(this, R.layout.activity_single_grid_image, list);
        gridView.setAdapter(adapter);

        //get all data from SQLite
        databaseHelper = new DatabaseHelper(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        //Populate data
        cursor = databaseHelper.getData();
        list.clear();
        cursor.moveToFirst();
        while (cursor.moveToNext()){
            int id = cursor.getInt(0);

            //get image from file path and convert it to array byte
            String imagePath = cursor.getString(1);
            File fi = new File(imagePath);

            byte[] image = new byte[0];
            try {
                image = Files.readAllBytes(fi.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            String city = cursor.getString(2);
            String latitude = cursor.getString(3);
            String longitude = cursor.getString(4);
            String time = cursor.getString(5);

            list.add(new ImageTaken(id, city, time , image, latitude, longitude));

        }

        adapter.notifyDataSetChanged();
    }

/*    public byte[] extractBytes (String ImageName) throws IOException {
        // open image
        File imgPath = new File(ImageName);
        BufferedImage bufferedImage = ImageIO.read(imgPath);

        // get DataBufferBytes from Raster
        WritableRaster raster = bufferedImage .getRaster();
        DataBufferByte data   = (DataBufferByte) raster.getDataBuffer();

        return ( data.getData() );
    }*/
}
