package com.example.myapplication;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import com.txusballesteros.bubbles.BubbleLayout;
import com.txusballesteros.bubbles.BubblesManager;
import com.txusballesteros.bubbles.OnInitializedCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

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
    BubbleLayout getBubble;

    //
    Intent ser;
    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermission();
        verifyPermissions();
        initBubble();
        initAcr();
        System.out.println("bubble called");
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
        stopService(ser);
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
        //setting text
        mResult.setText(tres);
        titlebtn.setText(ttt);
        artistbtn.setText(arr);
        //setting icon and state
        LinearLayout linearLayout = (LinearLayout) getBubble.findViewById(R.id.bubbleCover);
        ImageView  imageView=(ImageView) getBubble.findViewById(R.id.avatar);
        CardView card=(CardView) getBubble.findViewById(R.id.card);
        maximize();
        System.out.println("are the set");
        isrecording=false;
            imageView.setImageResource(R.drawable.play);
        System.out.println("stopped recording");
        card.setOnFocusChangeListener(new CardView.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    Toast.makeText(getApplicationContext(), "got the focus", Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(getApplicationContext(), "lost the focus", Toast.LENGTH_LONG).show();
                }
            }
        });
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
        linearLayout.getLayoutParams().width = 142;
        linearLayout.getLayoutParams().height = 142;
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
        card.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;;
        card.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;;
        card.requestLayout();
    }
    
}
