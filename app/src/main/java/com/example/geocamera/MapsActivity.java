package com.example.geocamera;

import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.MultiTapKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

   private GoogleMap mMap;
    Geocoder geocoder;
    String latitude, longitude;
    double lat, lng;
    long time;
    Date date;
    Timestamp ts;
    String tsString;
    String city = "Some City (fix later)";
    LatLng currlocation;
    String clickedCity;

    private Marker clickedMarker;

    private LocationManager locationManager;

  //  ImageView imageView;
    Button btnTakePic;
    String pathToFile;
    DatabaseHelper mDatabaseHelper;
    Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if(mMap == null) {
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
        intializeComponents();
    }

    public void intializeComponents() {

        date = new Date();
        mDatabaseHelper = new DatabaseHelper(this);
        geocoder = new Geocoder(this, Locale.getDefault());

      //  imageView = findViewById(R.id.image);
        btnTakePic = findViewById(R.id.btnTakePic);

        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 4);
        }

        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchPictureTakerAction();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    //Camera Code inspired from https://www.youtube.com/watch?v=32RG4lR0PMQ
    private void dispatchPictureTakerAction() {

        Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //make sure the app can handle the action of the intent
        if (takePic.resolveActivity(getPackageManager()) != null) {
            //file where photo will be store
            File photoFile = null;

            photoFile = createPhotoFile();

            if (photoFile != null) {
                pathToFile = photoFile.getAbsolutePath();
                Uri photoURI = FileProvider.getUriForFile(MapsActivity.this, "com.example.geocamera.fileprovider", photoFile);
                takePic.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePic, 1);
            }

        }
    }


    //save our image
    private File createPhotoFile() {

        String name = new SimpleDateFormat("yyyMMdd_HHmmss").format(new Date());
        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(name, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("Camera", "Excep: " + e.toString());
        }
        return image;
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
      //  mMap.setOnMarkerClickListener(this);
        mMap.setMyLocationEnabled(true);
        getLatLongTime();
        // Add a marker in Sydney and move the camera
        currlocation = new LatLng(lat, lng);

        //geocode the city
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(lat, lng, 1);
            city = addresses.get(0).getLocality();
        } catch (IOException e) {
            e.printStackTrace();
        }

      //  mMap.addMarker(new MarkerOptions().position(currlocation).title(city));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currlocation));

        this.initializeMap();

        //react when marker is clicked!
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            public boolean onMarkerClick(Marker marker) {
                double dlat =marker.getPosition().latitude;
                double dlon =marker.getPosition().longitude;
                List<Address> clickedAddress = null;

                try {
                    clickedAddress = geocoder.getFromLocation(dlat, dlon, 1);
                    clickedCity = clickedAddress.get(0).getLocality();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Intent imageListIntent = new Intent(MapsActivity.this, ImageList.class);
                imageListIntent.putExtra("clickedCity", clickedCity);
                startActivity(imageListIntent);

                return true;
            }
        });

    }

    private void initializeMap() {

        //repopulate markers
        cursor = mDatabaseHelper.getData();
        cursor.moveToFirst();

        if (cursor != null && cursor.moveToFirst()){
            do {

                String tempLat = cursor.getString(3);
                String tempLng = cursor.getString(4);
                Double tempLatDouble = Double.parseDouble(tempLat);
                Double tempLngDouble = Double.parseDouble(tempLng);

                LatLng tempLoc = new LatLng(tempLatDouble, tempLngDouble);

                //geocode the city
                List<Address> tempAddresses = null;
                String cityTemp = null;
                try {
                    tempAddresses = geocoder.getFromLocation(tempLatDouble, tempLngDouble, 1);
                    cityTemp = tempAddresses.get(0).getLocality();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mMap.addMarker(new MarkerOptions().position(tempLoc).title(cityTemp));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(tempLoc));


                String dbColID = cursor.getColumnName(0);
                String dbID = cursor.getString(0);
                String dbColCity = cursor.getColumnName(2);
                String dbCity = cursor.getString(2);

                Log.i("Maps Debug: DataBase", dbColCity + " -> " +  dbCity);
                Log.i("Maps Debug: DataBase", dbColID + " -> " +  dbID);

            }while (cursor.moveToNext());
        }

    }

    public void onTakePicture(View view) {

        Intent viewListIntent = new Intent(this, ImageList.class);
        viewListIntent.putExtra("clickedCity", "pleaseDisplayAllData");
        startActivity(viewListIntent);


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {

                Bitmap bitmap = BitmapFactory.decodeFile(pathToFile);
                //imageView.setImageBitmap(bitmap);

                this.getLatLongTime();
                this.saveToDataBase();

                //add a marker to the new created map, if its in a different spot
                LatLng tempLoc = new LatLng(lat, lng);
                if(tempLoc != currlocation){
                    currlocation = tempLoc;
                    mMap.addMarker(new MarkerOptions().position(currlocation).title( city));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currlocation));
                }



            }else   if (requestCode == 2) {
                  //retrieve the latitude, longitude, and picture URL
              }
        }
    }

    public void getLatLongTime(){

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        Location location = locationManager.getLastKnownLocation(Objects.requireNonNull(locationManager.getBestProvider(criteria, false)));
        lat = location.getLatitude();
        lng = location.getLongitude();

        //round values
        DecimalFormat df = new DecimalFormat("###.##");
        lat = Double.valueOf(df.format(lat));
        lng = Double.valueOf(df.format(lng));

        //convert them strings
        latitude = Double.toString(lat);
        longitude = Double.toString(lng);

        Log.i("Location", "Debug Lat: " + latitude + " Lng:  " + longitude);

       // date = Calendar.getInstance().getTime();
        date = new Date();
        //get TimeStamp
        time = date.getTime();
        ts =  new Timestamp(time);
        tsString = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z").format(ts);
        Log.i("Time", "Debug Time in Milliseconds: " + time + "  Current Time Stamp:  " + ts +
                "  TS String: "+ tsString);

        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(lat, lng, 1);
            city = addresses.get(0).getLocality();
            String cityName = addresses.get(0).getAddressLine(0);
            String stateName = addresses.get(0).getAddressLine(1);
            String countryName = addresses.get(0).getAddressLine(2);

            Log.i("Maps Debug", "city:  " + city + cityName + "   " + stateName + "  " + countryName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveToDataBase(){

        mDatabaseHelper.insertData(pathToFile, city, latitude,longitude,tsString);
        this.toastMessage("Picture Saved!");
    }
    private void toastMessage(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

 /*   @Override
    public void onMapClick(LatLng latLng) {


                double dlat = latLng.latitude;
                double dlon = latLng.longitude;
                List<Address> clickedAddress = null;
                String clickedCity =null;
                try {
                    clickedAddress = geocoder.getFromLocation(dlat, dlon, 1);
                    clickedCity = clickedAddress.get(0).getLocality();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Intent imageListIntent = new Intent(MapsActivity.this, ImageList.class);
                imageListIntent.putExtra("clickedCity", clickedCity);
                startActivity(imageListIntent);



    }*/
}
