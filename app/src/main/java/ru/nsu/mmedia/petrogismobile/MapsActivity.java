package ru.nsu.mmedia.petrogismobile;

import android.content.ContentValues;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import android.support.v4.content.FileProvider;
import android.view.View;
import android.content.Intent;
import android.provider.MediaStore;
import android.graphics.Bitmap;
import android.widget.EditText;
import android.widget.ImageView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.database.sqlite.*;
import android.database.Cursor;
import android.util.Log;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    static final int MY_REQUEST_CODE = 1;
    //public int MY_REQUEST_CODE_GPS = 2;
    public int LOCATION_REFRESH_TIME = 1; //ms
    public float LOCATION_REFRESH_DISTANCE = 0.1f; //meters

    private ImageView mImageView;
    private EditText mLatText, mLngText, mNameText;
    private TextView mGpsText;
    private TextView mStatusText;

    private GoogleMap mMap;

    private SensorManager mSensorManager;
    private float[] mAccelerometerReading = new float[3];
    private float[] mMagnetometerReading = new float[3];

    private LocationManager mLocationManager;

    Integer mToDownload;
    Integer mToUpload;
    Integer mDownloaded;
    Integer mUploaded;


    ServerHelper mServerHelper;
    LocalDBHelper mLocalDBHelper;

    ArrayList<Petroglyph> petroglyphs;
    HashMap<String, Petroglyph> petroglyphsMapped;

    //private String mPhotoPath;

//    Bitmap mCurrentImageBitmap;
    String mTempImageFileName;

    long mLastTimeGPSReceived = 0;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable(){
        public void run() {
            if (System.currentTimeMillis() - mLastTimeGPSReceived >= 5000) {
                mLatText.setText("");
                mLngText.setText("");
            }
            handler.postDelayed(runnable, 5000);
        }
    };

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mImageView = (ImageView) findViewById(R.id.mImageView);
        mLatText = (EditText) findViewById(R.id.latText);
        mLngText = (EditText) findViewById(R.id.lngText);
        mNameText = (EditText) findViewById(R.id.nameText);
        mGpsText = (TextView) findViewById(R.id.gpsText);
        mStatusText = (TextView) findViewById(R.id.statusText);
        mToDownload = 0;
        mDownloaded = 0;
        mToUpload = 0;
        mUploaded = 0;

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            mSensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            mSensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.CAMERA},
                                    MY_REQUEST_CODE);
            }
            else
            {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                        LOCATION_REFRESH_DISTANCE, mLocationListener);

            }
        }
        else
        {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE, mLocationListener);

        }

        mServerHelper = new ServerHelper();
        mServerHelper.context = getApplicationContext();
        mLocalDBHelper = new LocalDBHelper(this);
        mServerHelper.localDBHelper = mLocalDBHelper;
        if (savedInstanceState != null) {
            mTempImageFileName = savedInstanceState.getString("mTempImageFileName");
            if (mTempImageFileName != null)
            {
                showImage(mTempImageFileName);
            }
        }
        //handler.postAtTime(runnable, System.currentTimeMillis()+interval);
        handler.postDelayed(runnable, 5000);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        boolean cameraAllowed = false;
        boolean gpsAllowed = false;
        switch (requestCode) {
            case MY_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                for(int i = 0; i< permissions.length; i++) {
                    if (permissions[i].contentEquals(Manifest.permission.CAMERA) && grantResults[i] == PackageManager.PERMISSION_GRANTED)
                        cameraAllowed = true;
                    if (permissions[i].contentEquals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED)
                        gpsAllowed = true;
                }
                if (!cameraAllowed || !gpsAllowed) finish();
                else
                {
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                            LOCATION_REFRESH_DISTANCE, mLocationListener);
                    if (mMap != null) mMap.setMyLocationEnabled(true);

                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void cameraBtnClick(View view)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
        }
        dispatchTakePictureIntent();
    }

    public void syncBtnClick(View view)
    {
        ServerHelper.Callback onFinish = new ServerHelper.Callback() {
            @Override
            public void onDownloadUuidsFinished(List<String> list) {
                sync(list);
            }
        };
        mServerHelper.downloadUuidList(onFinish);
    }
    public void sync(List<String> list)
    {
        ServerHelper.CallbackUploadRecord recordUploaded = new ServerHelper.CallbackUploadRecord() {
            @Override
            public void onUploadFinished() {
                updateUploadCounter();
            }
        };
        ServerHelper.CallbackDownloadRecord recordDownloaded = new ServerHelper.CallbackDownloadRecord() {
            @Override
            public void onDownloadFinished() {
                updateDownloadCounter();
            }
        };
        try {
            //определим id для отправки на сервер
            List<Petroglyph> localPetroglyphs = mLocalDBHelper.getRecords();
            List<Petroglyph> localPetroglyphsToUpload = new ArrayList<Petroglyph>();
            for(int i = 0; i< localPetroglyphs.size(); i++)
            {
                Petroglyph lp = localPetroglyphs.get(i);
                if (!list.contains(lp.uuid)){
                    localPetroglyphsToUpload.add(lp);
                }
            }
            mToUpload = localPetroglyphsToUpload.size();
            mUploaded = 0;
            for (int i = 0; i< localPetroglyphsToUpload.size(); i++) {
                Petroglyph lp = localPetroglyphsToUpload.get(i);
                File file = new File(getApplicationContext().getFilesDir() + "/" + lp.image);
                mServerHelper.UploadRecord(lp.name, lp.uuid, lp.lat, lp.lng, lp.image, file, lp.orientation_x, lp.orientation_y, lp.orientation_z, recordUploaded);
            }
            List<String> petroglyphsToDownload = new ArrayList<String>();
            for(int i = 0; i < list.size(); i++)
            {
                String uuid = list.get(i);
                for(int j = 0; j< localPetroglyphs.size(); j++) {
                    if (localPetroglyphs.get(j).uuid.contentEquals(uuid)){
                        uuid = null;
                        break;
                    }
                }
                if (uuid != null) petroglyphsToDownload.add(uuid);
            }
            mToDownload = petroglyphsToDownload.size();
            mDownloaded = 0;
            for(int i = 0; i< petroglyphsToDownload.size(); i++)
            {
                String uuid = petroglyphsToDownload.get(i);
                mServerHelper.downloadRecord(uuid, recordDownloaded);
            }
        }
        catch(Exception e) {}
    }

    public void updateUploadCounter(){
        runOnUiThread(new Runnable() {
            public void run() {
                mUploaded++;
                mStatusText.setText("Sync (1/2) Upload: " + mUploaded + "/" + mToUpload);
            }
        });
    }

    public void updateDownloadCounter(){
        runOnUiThread(new Runnable() {
            public void run() {
                mDownloaded++;
                mStatusText.setText("Sync (2/2) Download: " + mDownloaded + "/" + mToDownload);
                if (mDownloaded == mToDownload)
                    updateUI();
            }
        });
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }
    }

    public void pushBtnClick(View view)
    {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        Location location = mLocationManager.getLastKnownLocation(mLocationManager.GPS_PROVIDER);


        // Rotation matrix based on current readings from accelerometer and magnetometer.
        final float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrix(rotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

// Express the updated rotation matrix as three orientation angles.
        final float[] orientationAngles = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        if (location == null) {
            //ToDo:What if we don't have a location or gps is off.
        }
        String photoPath = createImageFile();
        long rowId = mLocalDBHelper.createDBRecord(mNameText.getText().toString(), location.getLatitude(), location.getLongitude(), photoPath,
                orientationAngles[1], orientationAngles[2], orientationAngles[0]);
        updateUI();
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File tempImageFile = File.createTempFile(
                        "image",  /* prefix */
                        ".jpg",         /* suffix */
                        getApplicationContext().getCacheDir()      /* directory */
                );
                tempImageFile.deleteOnExit();
                mTempImageFileName = tempImageFile.getPath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider",
                                tempImageFile));
                //Uri.fromFile(mTempImageFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public String uniqid(String prefix,boolean more_entropy)
    {
        long time = System.currentTimeMillis();
        //String uniqid = String.format("%fd%05f", Math.floor(time),(time-Math.floor(time))*1000000);
        //uniqid = uniqid.substring(0, 13);
        String uniqid = "";
        if(!more_entropy)
        {
            uniqid = String.format("%s%08x%05x", prefix, time/1000, time);
        }else
        {
            java.security.SecureRandom sec = new  	java.security.SecureRandom();
            byte[] sbuf = sec.generateSeed(8);
            java.nio.ByteBuffer  bb = java.nio.ByteBuffer.wrap(sbuf);

            uniqid = String.format("%s%08x%05x", prefix, time/1000, time);
            uniqid += "." + String.format("%.8s", ""+bb.getLong()*-1);
        }


        return uniqid ;
    }


    protected String createImageFile() {
        String filename = uniqid("", false) + ".jpg";
        try {
            //FileOutputStream out = new FileOutputStream(getApplicationContext().getFilesDir() + filename);
            File tempImageFile = new File(mTempImageFileName);
            File persistentImageFile = new File(getApplicationContext().getFilesDir() + "/" + filename);
            tempImageFile.renameTo(persistentImageFile);
            //mCurrentImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return filename;
    }


    protected void updateUI() {
        showMarkers();
        Bitmap oldbitmap = null;
        if ((mImageView.getDrawable()) != null && mImageView.getDrawable() instanceof BitmapDrawable)
            oldbitmap = ((BitmapDrawable)mImageView.getDrawable()).getBitmap();
        mImageView.setImageResource(android.R.color.transparent);
        if (oldbitmap != null) oldbitmap.recycle();
        mTempImageFileName = null;
        mNameText.setText("");
        mLatText.setText("");
        mLngText.setText("");
        mStatusText.setText("Всего записей: " + petroglyphs.size());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //Bundle extras = data.getExtras();
            //mCurrentImageBitmap = (Bitmap) extras.get("data");
            showImage(mTempImageFileName);
        }
    }
    protected void showImage(String filename)
    {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 8;
        Bitmap bitmap = BitmapFactory.decodeFile(mTempImageFileName, opts);

        Bitmap oldbitmap = null;
        if ((mImageView.getDrawable()) != null && mImageView.getDrawable() instanceof BitmapDrawable)
            oldbitmap = ((BitmapDrawable)mImageView.getDrawable()).getBitmap();
        mImageView.setImageBitmap(bitmap);
        if (oldbitmap != null) oldbitmap.recycle();
    }

    public void showMarkers(){
        mMap.clear();
        petroglyphs = mLocalDBHelper.getRecords();
        petroglyphsMapped = new HashMap<String, Petroglyph>();

        for(int i = 0; i< petroglyphs.size(); i++)
        {
            Petroglyph petroglyph = petroglyphs.get(i);
            LatLng position = new LatLng(petroglyph.lat, petroglyph.lng);
            Marker marker = mMap.addMarker(new MarkerOptions().position(position).title(petroglyph.name));
            petroglyphsMapped.put(marker.getId(), petroglyph);

               /* // получаем значения по номерам столбцов и пишем все в лог
                if (c.getInt(idColIndex) == rowId) {
                    File imgFile = new File(getApplicationContext().getFilesDir() + c.getString(imgColIndex));
                    if (imgFile.exists()) {
                        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        mImageView.setImageBitmap(myBitmap);
                    }
                }*/

        }
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            // Use default InfoWindow frame
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(Marker marker) {
                View v = getLayoutInflater().inflate(R.layout.info_window_view, null);
                TextView tvName = (TextView) v.findViewById(R.id.name);
                ImageView imageView = (ImageView) v.findViewById(R.id.image);
                tvName.setText(marker.getTitle());

                String fileName = getApplicationContext().getFilesDir() + "/cache/" + petroglyphsMapped.get(marker.getId()).image;
                File f = new File(fileName);
                if(!f.exists()) {
                    fileName = getApplicationContext().getFilesDir() + "/" + petroglyphsMapped.get(marker.getId()).image;
                    f = new File(fileName);
                }
                if(f.exists()) {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = 8;
                    Bitmap bitmap = BitmapFactory.decodeFile(fileName, opts);

                    Bitmap oldbitmap = null;
                    if ((imageView.getDrawable()) != null && imageView.getDrawable() instanceof BitmapDrawable)
                        oldbitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                    imageView.setImageBitmap(bitmap);
                    if (oldbitmap != null) oldbitmap.recycle();
                }
                return v;
            }
        });
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
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mMap.setMyLocationEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        showMarkers();
    }

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            //your code here
            mLatText.setText(String.valueOf(location.getLatitude()));
            mLngText.setText(String.valueOf(location.getLongitude()));
            mLastTimeGPSReceived = System.currentTimeMillis();
        }
        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {}
        @Override
        public void onProviderEnabled(String s) {
            //getLocation();
        }
        @Override
        public void onProviderDisabled(String s) {
            if (mLocationManager != null) {
                mLocationManager.removeUpdates(this);
            }
        }
    };

    /*
    private class MyGPSListener implements GnssStatus.Callback {
        public void onSatelliteStatusChanged(int event) {
            switch (event) {
                if (mLastLocation != null)
                    isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < 3000;

                if (isGPSFix) { // A fix has been acquired.
                    // Do something.
                } else { // The fix has been lost.
                    // Do something.
                }
            }
        }
        public void onFirstFix(int event) {
            mGpsText.setText("Gps On");
        }
    }
*/

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mTempImageFileName != null)
            outState.putCharSequence("mTempImageFileName", mTempImageFileName);
        //toDo initialize variables like in onCreate
    }
}
