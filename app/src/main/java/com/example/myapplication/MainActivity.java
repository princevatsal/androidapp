package com.example.myapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.nex3z.notificationbadge.NotificationBadge;
import com.txusballesteros.bubbles.BubbleLayout;
import com.txusballesteros.bubbles.BubblesManager;
import com.txusballesteros.bubbles.OnInitializedCallback;

public class MainActivity extends AppCompatActivity {
    private BubblesManager bubblesManager;
    private NotificationBadge mBadge;
    private int MY_PERMISSION=1000;
    public  de.hdodenhof.circleimageview.CircleImageView Icon;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermission();
        initBubble();

        Button btnAdd=(Button) findViewById(R.id.btnAddBubble);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewBubble();
            }
        });
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


            }
        });
        bubbleView.setShouldStickToWall(true);
        bubblesManager.addBubble(bubbleView,60,20);

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        bubblesManager.recycle();
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
                startService(it);
            }
        }
    }
}
