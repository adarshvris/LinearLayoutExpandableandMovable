package com.example.a20122825.trayviewwithitems;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import TrayView.TrayItemSelected;
import TrayView.TrayViewWithThreeItems;

public class ThreeItemsActivity extends AppCompatActivity {

    public static final String TAG = ThreeItemsActivity.class.getSimpleName();
    private FrameLayout mFrameLayout;
    private TrayViewWithThreeItems mTrayViewWithThreeItems;
    private View mTrayView;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*This app requires draw app over permission for Android K and below
        it will automatically give permission but from Android M we need to explicitly give permission*/
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            }
        }

        mFrameLayout = (FrameLayout)findViewById(R.id.frameLayout);
        mButton = (Button) findViewById(R.id.button_to_get_two_items);

        mTrayViewWithThreeItems = new TrayViewWithThreeItems(this, R.array.tray_three_elements_items, R.array.tray_three_elements_icons);
        mTrayViewWithThreeItems.setTrayItemClickListner(mTrayItemSelected);
        mFrameLayout.addView(mTrayViewWithThreeItems);
        mTrayView = mTrayViewWithThreeItems.getTrayView();
    }

    private TrayItemSelected mTrayItemSelected = new TrayItemSelected() {
        @Override
        public void onItemClicked(String name, Object obj) {

            Log.d(TAG, "Selected = "+name);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        /*Once app comes to foreground we have to make the vioew vivible.*/
        if(mFrameLayout.getVisibility() == View.GONE){
            mFrameLayout.setVisibility(View.VISIBLE);
            mTrayView.setVisibility(View.VISIBLE);
        }

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ThreeItemsActivity.this, TwoItemsActivity.class));
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        /*Since we are using draw over other apps we have to remove the view
        when app is in background instead of removing here we are hiding the view.*/
        mFrameLayout.setVisibility(View.GONE);
        if(mTrayView!=null)
        {
            mTrayView.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /*Once the app is killed then we have to remove the view.*/
        if(mTrayViewWithThreeItems != null)
        {
            mTrayViewWithThreeItems.removeView();
        }
    }
}
