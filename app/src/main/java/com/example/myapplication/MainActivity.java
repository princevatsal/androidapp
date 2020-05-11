package com.example.myapplication;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.acrcloud.rec.ACRCloudClient;
import com.acrcloud.rec.ACRCloudConfig;
import com.acrcloud.rec.ACRCloudResult;
import com.acrcloud.rec.IACRCloudListener;
import com.acrcloud.rec.IACRCloudPartnerDeviceInfo;
import com.acrcloud.rec.IACRCloudRadioMetadataListener;
import com.acrcloud.rec.utils.ACRCloudLogger;
import com.nex3z.notificationbadge.NotificationBadge;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.Picasso;
import com.txusballesteros.bubbles.BubbleLayout;
import com.txusballesteros.bubbles.BubblesManager;
import com.txusballesteros.bubbles.OnInitializedCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.android.volley.*;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;





public class MainActivity extends AppCompatActivity implements IACRCloudListener, IACRCloudRadioMetadataListener {
    //declaring class variable
    private int MY_PERMISSION=1000;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO
    };
    //declaring Bubble Variables
    private BubblesManager bubblesManager;
    private NotificationBadge mBadge;
    private TextView  titlebtn,artistbtn,mResult;
    private boolean isrecording=false;
    private long startTime = 0;
    private ACRCloudConfig mConfig = null;
    private ACRCloudClient mClient = null;
    private boolean mProcessing = false;
    private boolean initState = false;
    private String path = "";
    public String YoutubeUrl="";
    public String SpotifySearchTerm="";
    BubbleLayout getBubble;

    //
    Intent ser;

    //
    @Override
    protected  void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermission();
        verifyPermissions();
        initBubble();
        initAcr();
        System.out.println("bubble called");
//        moveTaskToBack(true);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

    }
    public void verifyPermissions() {

        for (int i=0; i<PERMISSIONS.length; i++) {
            int permission = ActivityCompat.checkSelfPermission(this, PERMISSIONS[i]);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS,
                        REQUEST_EXTERNAL_STORAGE);
                break;
            }
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
//        stop+ice(ser);
        Toast.makeText(MainActivity.this, "Service Un-Binded", Toast.LENGTH_LONG).show();
        bubblesManager.recycle();
    }
    public void getPermission(){
        if(Build.VERSION.SDK_INT>=23){
            if(!Settings.canDrawOverlays(this)){
                Intent intent=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" +getPackageName()));
                startActivityForResult(intent,MY_PERMISSION);

            }
            else{
                Intent it=new Intent(this, Service.class);
                ser=it;
                startService(it);
            }
        }
    }
    @Override
    public void onStop() {
        super.onStop();
    }
    //Bubble Functions
    private void initBubble() {
        bubblesManager=new BubblesManager.Builder(this)
                .setInitializationCallback(new OnInitializedCallback() {
                    @Override
                    public void onInitialized() {
                        System.out.println("addming bubble ");
                        addNewBubble();
                    }
                }).build();
        bubblesManager.initialize();
    }
    private void initAcr(){
        mResult = (TextView) findViewById(R.id.result);
        path = Environment.getExternalStorageDirectory().toString()
                + "/acrcloud";
        Log.e("logs", path);

        File file = new File(path);
        if(!file.exists()){
            file.mkdirs();
        }
        this.mConfig = new ACRCloudConfig();
        this.mConfig.acrcloudListener = this;
        this.mConfig.context = this;
        this.mConfig.host = "identify-ap-southeast-1.acrcloud.com";
        this.mConfig.accessKey = "96569ce32f677be9ef5da22319655fb8";
        this.mConfig.accessSecret = "WNW0o352IyBq5qdnohGARcmlHsl9G6G4oTet7EgQ";
        this.mConfig.hostAuto = "";
        this.mConfig.accessKeyAuto = "";
        this.mConfig.accessSecretAuto = "";
        this.mConfig.recorderConfig.rate = 8000;
        this.mConfig.recorderConfig.channels = 1;
        this.mConfig.acrcloudPartnerDeviceInfo = new IACRCloudPartnerDeviceInfo() {
            @Override
            public String getGPS() {
                return null;
            }

            @Override
            public String getRadioFrequency() {
                return null;
            }

            @Override
            public String getDeviceId() {
                return "";
            }

            @Override
            public String getDeviceModel() {
                return null;
            }
        };

        // If you do not need volume callback, you set it false.
        this.mConfig.recorderConfig.isVolumeCallback = false;
        this.mClient = new ACRCloudClient();
        ACRCloudLogger.setLog(true);
        this.initState = this.mClient.initWithConfig(this.mConfig);
    }
    public void addNewBubble() {
       BubbleLayout bubbleView=(BubbleLayout) LayoutInflater.from(this)
                .inflate(R.layout.bubble_layout,null);
        mBadge=(NotificationBadge) bubbleView.findViewById(R.id.count);
        mBadge.setText(" x ");
        bubbleView.setOnBubbleRemoveListener(new BubbleLayout.OnBubbleRemoveListener(){
            @Override
            public void onBubbleRemoved(BubbleLayout bubble){
                Toast.makeText(MainActivity.this,"Removed",Toast.LENGTH_SHORT).show();
            }
        });
        bubbleView.setOnBubbleClickListener(new BubbleLayout.OnBubbleClickListener() {
            @Override
            public void onBubbleClick(BubbleLayout bubble) {
                Toast.makeText(MainActivity.this,"Clicked",Toast.LENGTH_SHORT).show();
                titlebtn=(TextView) bubble.findViewById(R.id.title);
                artistbtn=(TextView) bubble.findViewById(R.id.artist);
                getBubble=bubble;
                ClickIcon(bubble);
            }
        });
        bubbleView.setShouldStickToWall(true);
        bubblesManager.addBubble(bubbleView,60,20);

    }
    public void notification(View view) {
        Toast.makeText(this,"Notification",Toast.LENGTH_SHORT).show();
        bubblesManager.recycle();
    }
    public void ClickIcon(View view) {

        ImageView imageView=(ImageView) view.findViewById(R.id.avatar);
        CardView card=(CardView) view.findViewById(R.id.card);
        System.out.println(imageView);
        System.out.println("clicked");
        System.out.println("changed view");
        Animation aniRotate = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate);
        imageView.startAnimation(aniRotate);
        if(isrecording){
            cancel();
            isrecording=!isrecording;
            System.out.println("stopped recording");
            imageView.setImageResource(R.drawable.play);
            card.setVisibility(View.INVISIBLE);
        }else{
                start();
            System.out.println("started recording");
            isrecording=!isrecording;
            imageView.setImageResource(R.drawable.mic);
            card.setVisibility(View.INVISIBLE);
        }
    }
    public void start() {
        if (!this.initState) {
            Toast.makeText(this, "init error", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mProcessing) {
            mProcessing = true;
            mResult.setText("");
            if (this.mClient == null || !this.mClient.startRecognize()) {
                mProcessing = false;
                mResult.setText("start error!");
            }
            startTime = System.currentTimeMillis();
        }
    }
    public void cancel() {
        if (mProcessing && this.mClient != null) {
            this.mClient.cancel();
        }

        this.reset();
    }
    public void reset() {
        mResult.setText("");
        mProcessing = false;
    }
    @Override
    public void onVolumeChanged(double volume) {
        Log.i("logs","vol change");
    }
    @Override
    public void onRadioMetadataResult(String s) {
        mResult.setText(s);
    }
    @Override
    public void onResult(ACRCloudResult results) {
        this.reset();
        // If you want to save the record audio data, you can refer to the following codes.
	/*
	byte[] recordPcm = results.getRecordDataPCM();
        if (recordPcm != null) {
            byte[] recordWav = ACRCloudUtils.pcm2Wav(recordPcm, this.mConfig.recorderConfig.rate, this.mConfig.recorderConfig.channels);
            ACRCloudUtils.createFileWithByte(recordWav, path + "/" + "record.wav");
        }
	*/

        String result = results.getResult();

        String tres = "\n";
        String ttt="",arr="";
        try {
            JSONObject j = new JSONObject(result);
            JSONObject j1 = j.getJSONObject("status");
            int j2 = j1.getInt("code");
            System.out.println("code:-");
            System.out.println(j2);
            //3003
            if(j2 == 0){
                JSONObject metadata = j.getJSONObject("metadata");
                //
                if (metadata.has("music")) {

                    JSONArray musics = metadata.getJSONArray("music");
                    for(int i=0; i<musics.length(); i++) {
                        JSONObject tt = (JSONObject) musics.get(i);
                        String title = tt.getString("title");
                        JSONArray artistt = tt.getJSONArray("artists");
                        JSONObject art = (JSONObject) artistt.get(0);
                        String artist = art.getString("name");
                        ttt = ttt + title ;
                        arr = arr + artist ;
                    }


                }


            }else{
                tres = result;
                ttt="Not";
                arr="Found";
            }
        } catch (JSONException e) {
            tres = result;
//            titlebtn.setText("Not");
//            artistbtn.setText("Found");
            ttt="Not";
            arr="Found";
            e.printStackTrace();
        }
        Log.i("logs",tres);

        //setting icon and state
        LinearLayout linearLayout = (LinearLayout) getBubble.findViewById(R.id.bubbleCover);
        final ImageView  imageView=(ImageView) getBubble.findViewById(R.id.avatar);
        final ImageView thumbnail=(ImageView) getBubble.findViewById(R.id.thumbnail);
        CardView card=(CardView) getBubble.findViewById(R.id.card);
        //
        if(ttt!="Not")
        {
            final String localttt=ttt;
            SpotifySearchTerm=ttt;
        RequestQueue queue = Volley.newRequestQueue(this);
        String query=ttt+" by "+arr;
        System.out.println("searching for query:-");
        System.out.println(query);
        String url ="https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=1&q="+query+"&key=AIzaSyA4yrKXmfFVMnLaLhCDFTRF8AsAvkbRZ8w";

            final String finalTtt = ttt;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        System.out.println("we Get Response");
                        JSONObject objj;
                        JSONArray arr;
                        JSONObject objj2;
                        JSONObject snippet;
                        JSONObject id;
                        String videoId;
                        JSONObject thumbnails;
                        JSONObject def;
                        String url,title;
                        try {
                            objj = new JSONObject(response);
                            arr=(JSONArray) objj.get("items");
                            objj2=(JSONObject) arr.get(0);
                            snippet =(JSONObject) objj2.get("snippet");
                            id=(JSONObject) objj2.get("id");
                            videoId=id.get("videoId").toString();
                            title= snippet.get("title").toString();
                            thumbnails=(JSONObject) snippet.get("thumbnails");
                            def=(JSONObject) thumbnails.get("default");
                            url=def.get("url").toString();
                            System.out.println(title);
                            System.out.println(url);
                            //setting text
//                          mResult.setText(title);
                            titlebtn.setText(localttt);
                            artistbtn.setText("");
                            //setting img
                            System.out.println(localttt);
                            try {
                                URL urll = new URL(url);
                                System.out.println("fetching ");
                                Bitmap bmp = BitmapFactory.decodeStream(urll.openConnection().getInputStream());
                                thumbnail.setImageBitmap(bmp);
                            }catch (IOException E){

                            }

                            //
                            YoutubeUrl="watch?v="+videoId;
                            maximize();
//                          imageView.setImageResource(R.drawable.play);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            maximize();
                            titlebtn.setText("Not");
                            artistbtn.setText("Found");

                            imageView.setImageResource(R.drawable.play);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("an error occured");
            }
        });
        // Add the request to the
            queue.add(stringRequest);
        }else{
            System.out.println("seeting 404 ");
            thumbnail.setImageResource(R.drawable.notfound);
            YoutubeUrl="";
            SpotifySearchTerm="";
            maximize();
            titlebtn.setText("Not");
            artistbtn.setText("Found");
        }
        //
        System.out.println("are the set");
        isrecording=false;
        System.out.println("stopped recording");

        ttt="";
        arr="";
        startTime = System.currentTimeMillis();
    }

    public void cardClick(View view) {
        System.out.println("do nothing ");
    }

    public void goHome(View view) {
        minimize();
    }
    //
    public void minimize(){
        CardView card=(CardView) getBubble.findViewById(R.id.card);
        LinearLayout linearLayout = (LinearLayout) getBubble.findViewById(R.id.bubbleCover);
        linearLayout.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        linearLayout.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        linearLayout.requestLayout();
        card.getLayoutParams().width =0;
        card.getLayoutParams().height = 0;
        card.requestLayout();
    }
    public void  maximize(){
        CardView card=(CardView) getBubble.findViewById(R.id.card);
        LinearLayout linearLayout = (LinearLayout) getBubble.findViewById(R.id.bubbleCover);
        linearLayout.getLayoutParams().width = 0;
        linearLayout.getLayoutParams().height = 0;
        linearLayout.requestLayout();
        card.setVisibility(View.VISIBLE);
        card.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        card.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        card.requestLayout();
    }

    public void GoYoutube(View view) {
        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/"+YoutubeUrl));
        startActivity(intent);
    }

    public void goSpotify(View view) {
        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/"+SpotifySearchTerm));
        startActivity(intent);
    }
}


