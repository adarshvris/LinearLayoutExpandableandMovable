package com.example.a20122825.trayviewwithitems;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import TrayView.TrayItemSelected;
import TrayView.TrayViewWithThreeItems;
import TrayView.TrayViewWithTwoItems;

public class TwoItemsActivity extends AppCompatActivity {

    public static final String TAG = TwoItemsActivity.class.getSimpleName();
    private FrameLayout mFrameLayout;
    private TrayViewWithTwoItems mTrayViewWithTwoItems;
    private View mTrayView;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_items);

        mFrameLayout = (FrameLayout)findViewById(R.id.frameLayout);
        mButton = (Button) findViewById(R.id.button_to_get_one_item);

        mTrayViewWithTwoItems = new TrayViewWithTwoItems(this, R.array.tray_two_elements_items, R.array.tray_two_elements_icons);
        mTrayViewWithTwoItems.setTrayItemClickListner(mTrayItemSelected);
        mFrameLayout.addView(mTrayViewWithTwoItems);
        mTrayView = mTrayViewWithTwoItems.getTrayView();
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
                startActivity(new Intent(TwoItemsActivity.this, OneItemActivity.class));
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
        if(mTrayViewWithTwoItems != null)
        {
            mTrayViewWithTwoItems.removeView();
        }
    }
}
