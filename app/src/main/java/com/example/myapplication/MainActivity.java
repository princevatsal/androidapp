package com.example.myapplication;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.nex3z.notificationbadge.NotificationBadge;
import com.txusballesteros.bubbles.BubbleLayout;
import com.txusballesteros.bubbles.BubblesManager;
import com.txusballesteros.bubbles.OnInitializedCallback;
import java.io.File;
import com.acrcloud.rec.*;
import com.acrcloud.rec.utils.ACRCloudLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity implements IACRCloudListener, IACRCloudRadioMetadataListener{
    private BubblesManager bubblesManager;
    private NotificationBadge mBadge;
    private int MY_PERMISSION=1000;
    private final static String TAG = "MainActivity";
    private TextView  mResult,titlebtn,artistbtn;
    private boolean mProcessing = false;
    private boolean initState = false;
    private String path = "";
    private long startTime = 0;
    private ACRCloudConfig mConfig = null;
    private ACRCloudClient mClient = null;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO
    };
    Intent ser;
    BubbleLayout getBubble;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setting up first bubble and add bubble button
        getPermission();
        initBubble();
        Button btnAdd=(Button) findViewById(R.id.btnAddBubble);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewBubble();
            }
        });
        //

//        LinearLayout ll = new LinearLayout(this);
//        recordButton = new RecordButton(this);
//        ll.addView(recordButton,
//                new LinearLayout.LayoutParams(
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        0));
//        playButton = new PlayButton(this);
//        ll.addView(playButton,
//                new LinearLayout.LayoutParams(
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        0));
//        setContentView(ll);
        //acr demo
        mResult = (TextView) findViewById(R.id.result);
        path = Environment.getExternalStorageDirectory().toString()
                + "/acrcloud";
        Log.e(TAG, path);

        File file = new File(path);
        if(!file.exists()){
            file.mkdirs();
        }

        verifyPermissions();

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
    @Override
    public void onVolumeChanged(double volume) {
        Log.i("logs","vol change");
    }
    @Override
    public void onRadioMetadataResult(String s) {
        mResult.setText(s);
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
    private void initBubble() {
        //
        //

        bubblesManager=new BubblesManager.Builder(this)
                .setInitializationCallback(new OnInitializedCallback() {
                    @Override
                    public void onInitialized() {
                        addNewBubble();
                    }
                }).build();
        bubblesManager.initialize();
    }

    private void addNewBubble() {
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

    @Override
    protected void onDestroy(){
        super.onDestroy();
        bubblesManager.recycle();
        stopService(ser);
        Toast.makeText(MainActivity.this, "Service Un-Binded", Toast.LENGTH_LONG).show();
    }

    public void notification(View view) {
        Toast.makeText(MainActivity.this,"Notification",Toast.LENGTH_SHORT).show();
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
    private boolean ct=true;
    private boolean isrecording=false;
    public void ClickIcon(View view) {
        AppCompatImageView  imageView=(AppCompatImageView) view.findViewById(R.id.avatar);
        CardView card=(CardView) view.findViewById(R.id.card);
        System.out.println(imageView);
        System.out.println("clicked");
        Animation aniRotate = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate);
        imageView.startAnimation(aniRotate);
//        if(ct){
//            start();
//            System.out.println("started recording");
//            ct=!ct;
//            imageView.setImageResource(R.drawable.mic);
//        }
//        else{
//            cancel();
//            ct=!ct;
//            System.out.println("stopped recording");
//            imageView.setImageResource(R.drawable.play);
//        }
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

    @Override
    public void onStop() {
        super.onStop();
//        if (recorder != null) {
//            recorder.release();
//            recorder = null;
//        }
//
//        if (player != null) {
//            player.release();
//            player = null;
//        }
    }
    @Override
    public void onResult(ACRCloudResult results) {
        this.reset();
        CardView card=(CardView) getBubble.findViewById(R.id.card);
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
        card.setVisibility(View.VISIBLE);
        AppCompatImageView  imageView=(AppCompatImageView) getBubble.findViewById(R.id.avatar);
        isrecording=false;
        System.out.println("stopped recording");
        imageView.setImageResource(R.drawable.play);
        ttt="";
        arr="";
        startTime = System.currentTimeMillis();
    }

    public void playsong(View view) {
        System.out.println("***********************************playing song");
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=cxLG2wtE7TM")));
    }
}
