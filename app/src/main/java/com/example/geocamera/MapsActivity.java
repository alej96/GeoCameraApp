package com.example.geocamera;

import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Geocoder geocoder;
    String latitude, longitude;
    double lat, lng;
    long time;
    Date date;
    Timestamp ts;
    String tsString;
    String city = "Some City (fix later)";

    private LocationManager locationManager;

    ImageView imageView;
    Button btnTakePic;
    String pathToFile;
    DatabaseHelper mDatabaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        intializeComponents();
    }

    public void intializeComponents() {

        date = new Date();
        mDatabaseHelper = new DatabaseHelper(this);
        geocoder = new Geocoder(this, Locale.getDefault());

        imageView = findViewById(R.id.image);
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

        mMap.setMyLocationEnabled(true);
        getLatLongTime();
        // Add a marker in Sydney and move the camera
        LatLng currlocation = new LatLng(lat, lng);
        mMap.addMarker(new MarkerOptions().position(currlocation).title("Marker Here!"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currlocation));

    }

    public void onTakePicture(View view) {

        Intent cameraIntent = new Intent(this, ImageList.class);
        startActivityForResult(cameraIntent, 2);


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {

                Bitmap bitmap = BitmapFactory.decodeFile(pathToFile);
                imageView.setImageBitmap(bitmap);

                this.getLatLongTime();
                this.saveToDataBase();


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
        latitude = Double.toString(lat);
        lng = location.getLongitude();
        longitude = Double.toString(lng);

        Log.i("Location", "Debug Lat: " + latitude + " Lng:  " + longitude);

        //get TimeStamp
        time = date.getTime();
        ts =  new Timestamp(time);
        tsString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ").format(ts);
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
    /*public void onSearch(View view){

        EditText location_tf = (EditText) findViewById(R.id.textSearch);
        String location = location_tf.getText().toString();

        List<Address> addressList = null;

        if(location != null || ! location.equals("'")){
            Geocoder geocoder = new Geocoder(this);
            try{
                addressList = geocoder.getFromLocationName(location, 1);

            }catch (IOException e ){
                e.printStackTrace();
            }

            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }

    }*/
}
