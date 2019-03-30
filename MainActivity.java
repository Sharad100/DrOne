package com.androstock.galleryapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MergeCursor;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.SmsManager;


import com.bumptech.glide.Glide;
//import com.androstock.galleryapp.ImageClassifier;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MainActivity extends AppCompatActivity {

    // name of album created and used by Tello App on Android
    static final String telloAlbum = "TelloPhoto";

    static final int REQUEST_PERMISSION_KEY = 1;
    LoadAlbum loadAlbumTask;
    static TextView statusMessage;
    static String status = "";
    static ImageView droneView;
    static Bitmap droneImage = null;

    ArrayList<HashMap<String, String>> albumList = new ArrayList<HashMap<String, String>>();
    ArrayList<String> photoList = new ArrayList<String>();
    ImageClassifier classifier;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        int iDisplayWidth = getResources().getDisplayMetrics().widthPixels;
        Resources resources = getApplicationContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = iDisplayWidth / (metrics.densityDpi / 160f);
        /*
        if(dp < 360)
        {
            dp = (dp - 17) / 2;
            float px = Function.convertDpToPixel(dp, getApplicationContext());
            galleryGridView.setColumnWidth(Math.round(px));
        }
        */
        String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (!Function.hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_KEY);
        }


    }


    class LoadAlbum extends AsyncTask<String, String, String> {
        void displayStatus(String msg) {
            status += msg;
            statusMessage.setText(status);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setContentView(R.layout.activity_main);
            albumList.clear();
            statusMessage = (TextView) findViewById(R.id.statusText);
            droneView = (ImageView) findViewById(R.id.image);
            displayStatus("hello\n");


        }

        @Override
        protected void onProgressUpdate(String... values) {
            String status = "\n";
            super.onProgressUpdate(values);
            if (values != null && values.length > 0) {
                //Your View attribute object in the activity
                // already initialized in the onCreate!
                if (droneImage != null) {
                    //status = "redrawing image\n";
                }
                displayStatus(values[0] + status);
            }
            droneView = (ImageView) findViewById(R.id.imageView1);
            if (droneImage != null)
                droneView.setImageBitmap(droneImage);
        }

        protected String doInBackground(String... args) {
            String xml = "";

            String path = null;
            String album = null;
            String timestamp = null;
            String countPhoto = null;
            //Uri uriInternal = android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI;
            Uri uriExternal = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            long lastScannedTime = 1539155146L;
            try {
                classifier = new ImageClassifier(MainActivity.this);
            } catch (IOException e) {
                Log.e("tag", "Failed to initialize an image classifier: " + e.getMessage());
            }

            String[] projection = {MediaStore.MediaColumns.DATA,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.MediaColumns.DATE_MODIFIED};


            while (true) {
                Cursor cursor = getContentResolver().query(uriExternal, projection, "_data IS NOT NULL AND bucket_display_name=?", new String[]{telloAlbum}, MediaStore.Images.Media.DATE_MODIFIED + " DESC");

                if (cursor.moveToNext()) {

                    timestamp = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED));


                    long photoTakenTime = Long.parseLong(timestamp);
                    if (photoTakenTime > lastScannedTime) {
                        path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
                        album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
                        System.out.println("*********** path = " + path + ", album = " + album + ", timestamp = " + timestamp + ": " + " lastscanned = " + lastScannedTime);
                        System.out.println("@@@@@@@@@@@@@@@@@" + photoTakenTime + " > " + lastScannedTime + ", so adding");
                        boolean fallen = isFallen(path);
                        if (fallen) {
                            System.out.println("Sending SMS");
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage("6505555555", null, "Help! man Down!", null, null);
                            System.out.println("Done Sending SMS");
                            publishProgress("Help! Person Down!\n");
                            publishProgress("Sending SMS");
                        } else {
                            publishProgress("All's well; Not Sending SMS\n");
                        }
                    }
                }
                // call TF
                long millis = System.currentTimeMillis();
                lastScannedTime = millis / 1000;
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("AWAKE    ################" + " last scanned " + lastScannedTime);
            }

        }

        boolean isFallen(String path) {
            File image = new File(path);
            System.out.println("path = " + path + ", file = " + image + ", abs path = " + image.getAbsolutePath());
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            //Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(),bmOptions);
            droneImage = BitmapFactory.decodeFile(image.getAbsolutePath(), bmOptions);
            droneImage = Bitmap.createScaledBitmap(droneImage, ImageClassifier.DIM_IMG_SIZE_X, ImageClassifier.DIM_IMG_SIZE_Y, true);
            System.out.println("bitmap = " + droneImage + ", classifier = " + classifier);
            String textToShow = classifier.classifyFrame(droneImage);
            publishProgress(textToShow + "\n");
            String[] arr = textToShow.split("\\s+");
            System.out.println(textToShow);
            System.out.println(arr[2]);
            float value = Float.parseFloat(arr[2]);
            //float value = 0;
            System.out.println("<< " + value + " >>");
            System.out.println("<< " + arr[1] + " >>");
            if (arr[1].equals("fallen:") && value > 0.9) {
                return true;
            }
            return false;

        }


        @Override
        protected void onPostExecute(String xml) {

        }


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_KEY: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadAlbumTask = new LoadAlbum();
                    loadAlbumTask.execute();
                } else {
                    Toast.makeText(MainActivity.this, "You must accept permissions.", Toast.LENGTH_LONG).show();
                }
            }
        }

    }


    @Override
    protected void onResume() {
        super.onResume();

        String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (!Function.hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_KEY);
        } else {
            loadAlbumTask = new LoadAlbum();
            loadAlbumTask.execute();
        }

    }
}


class AlbumAdapter extends BaseAdapter {
    private Activity activity;
    private ArrayList<HashMap<String, String>> data;

    public AlbumAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
        activity = a;
        data = d;
    }

    public int getCount() {
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        AlbumViewHolder holder = null;
        if (convertView == null) {
            holder = new AlbumViewHolder();
            convertView = LayoutInflater.from(activity).inflate(
                    R.layout.album_row, parent, false);

            holder.galleryImage = (ImageView) convertView.findViewById(R.id.galleryImage);
            holder.gallery_count = (TextView) convertView.findViewById(R.id.gallery_count);
            holder.gallery_title = (TextView) convertView.findViewById(R.id.gallery_title);

            convertView.setTag(holder);
        } else {
            holder = (AlbumViewHolder) convertView.getTag();
        }
        holder.galleryImage.setId(position);
        holder.gallery_count.setId(position);
        holder.gallery_title.setId(position);

        HashMap<String, String> song = new HashMap<String, String>();
        song = data.get(position);
        try {
            holder.gallery_title.setText(song.get(Function.KEY_ALBUM));
            holder.gallery_count.setText(song.get(Function.KEY_COUNT));

            Glide.with(activity)
                    .load(new File(song.get(Function.KEY_PATH))) // Uri of the picture
                    .into(holder.galleryImage);


        } catch (Exception e) {
        }
        return convertView;
    }
}


class AlbumViewHolder {
    ImageView galleryImage;
    TextView gallery_count, gallery_title;
}
