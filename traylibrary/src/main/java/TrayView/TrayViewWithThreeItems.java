package TrayView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.traylibrary.CustomTextView;
import com.example.traylibrary.R;

import java.util.ArrayList;

import animation.Techniques;
import animation.TrayAnimation;

import static android.content.Context.WINDOW_SERVICE;

/**
 * Created by adarsh on 11/10/2017.
 */

public class TrayViewWithThreeItems extends View {

    public static final String TAG = TrayViewWithThreeItems.class.getSimpleName();

    //parameters for tray movement
    private Context mContext;
    private WindowManager mWindowManager;
    private DisplayMetrics metrics;
    private View mFloatingView;
    private WindowManager.LayoutParams params;
    private View mExpandedView;
    private LinearLayout mLeftContainerView;
    private LinearLayout mRightContainerView;
    private View mCollapsedView;

    //parameters to get the tray image dimension
    private int viewHeight = 0;
    private int mWhereAmI = 0;
    private int viewWidth = 0;

    //parameters to check the tray icon movement
    private Boolean isDragStarted;
    private Boolean isTrayContentViewCollapsed;
    private Boolean checkQuadrant;
    private Boolean isDragLeftStarted;

    //glow animation parameter
    private AnimatorSet mAnimationSet;
    private AnimatorSet mAnimationLeftSet;

    //parameters for Threshold Value
    private int mThresholdCheckY;
    private int mThresholdCheckX;
    private int mThresholdWhichkQuadrant;

    //parameters to handle click on each item of the tyar
    private boolean isModeClicked;
    private boolean isEditClicked;
    private boolean isfreeMode;
    private boolean isGridMode;
    private boolean isFloorSelectImage;
    private boolean isDragFirst;
    private StringBuilder mDistanceRight;
    private StringBuilder mDistanceLeft;
    private Boolean isGlowAnimationActivated;

    private Handler mHandler;
    //call back functionality to use in Activity
    private TrayItemSelected mOnItemClickedListener;

    //parameters for icons and texts
    private String[] trayItemNames;
    private TypedArray mTrayItemIconArr;
    private ImageView mModeImage, mEditImage, mFloorImage;
    private CustomTextView mModeLabel, mEditLabel, mFloorLabel;


    /*call back function to interact with ui and activity functionality*/
    public void setTrayItemClickListner(TrayItemSelected onFloatingItemClickedListener){
        this.mOnItemClickedListener = onFloatingItemClickedListener;
    }

    public TrayViewWithThreeItems(Context context, int trayItemsNameArray, int trayItemIconArray) {
        super(context);
        this.mContext = context;
        trayItemNames = getResources().getStringArray(trayItemsNameArray);
        mTrayItemIconArr = getResources().obtainTypedArray(trayItemIconArray);
        createView();
    }

    public TrayViewWithThreeItems(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        createView();
    }

    public TrayViewWithThreeItems(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        createView();
    }

    //This Method will create the tray View.
    /*This method is used to create the basic view which is draggable*/
    private void createView() {
        mWindowManager = (WindowManager) mContext.getSystemService(WINDOW_SERVICE);

        metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);

        //Inflate the Tray view layout we created
        mFloatingView = LayoutInflater.from(mContext).inflate(R.layout.common_tray_layout_mobile, null);

        //Add the view to the window.
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        isDragStarted = false;
        isTrayContentViewCollapsed = false;
        checkQuadrant = false;
        isDragLeftStarted = false;
        mThresholdWhichkQuadrant = 0;
        isfreeMode = false;
        isGridMode = false;
        isEditClicked = false;
        isModeClicked = false;
        isFloorSelectImage = false;
        isDragFirst = true;
        mDistanceRight = new StringBuilder();
        mDistanceLeft = new StringBuilder();
        isGlowAnimationActivated = false;
        mHandler = new Handler();

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.START;        //Initially view will be added to top-left corner
        params.x = metrics.widthPixels;
        params.y = metrics.heightPixels - 300;
        mThresholdCheckY = metrics.heightPixels - 300;
        mThresholdCheckX = metrics.widthPixels;


        //Add the view to the window
        mWindowManager.addView(mFloatingView, params);

        mLeftContainerView = (LinearLayout) mFloatingView.findViewById(R.id.expanded_container_left);
        mRightContainerView = (LinearLayout) mFloatingView.findViewById(R.id.expanded_container_right);

        //The root element of the collapsed view layout
        mCollapsedView = mFloatingView.findViewById(R.id.collapse_view);

        //The glow animation
        initFadeInFadeOutAnimation(true);
        mFloatingView.findViewById(R.id.collapsed_iv_animate).setVisibility(GONE);
        mFloatingView.findViewById(R.id.collapsed_iv_animate_left).setVisibility(GONE);

        setTouchListener();

        ViewTreeObserver viewTreeObserver = mFloatingView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mFloatingView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    viewWidth = mFloatingView.getWidth();
                    viewHeight = mFloatingView.getHeight();
                }
            });
        }

    }


    //The tray touch and movement controller
    private void setTouchListener() {

        mFloatingView.findViewById(R.id.collapsed_iv).setOnTouchListener(new OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int Xdiff;
                int Ydiff;
                switch (event.getAction() & MotionEvent.ACTION_MASK) {

                    case MotionEvent.ACTION_DOWN:

                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

                        Log.d(TAG, "ACTION_DOWN");
                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        return true;

                    case MotionEvent.ACTION_MOVE:

                        if (mExpandedView != null) {
                            mCollapsedView.setVisibility(INVISIBLE);
                        }

                        if(isGlowAnimationActivated)
                        {
                            mFloatingView.findViewById(R.id.collapsed_iv_animate).setVisibility(GONE);
                            mFloatingView.findViewById(R.id.collapsed_iv_animate_left).setVisibility(GONE);
                        }

                        //background change of the tray based on the tray movement left half or right half.
                        if((int)event.getRawX() > mThresholdCheckX/2)
                        {
                            mFloatingView.findViewById(R.id.collapsed_iv).setBackground(getResources().getDrawable(R.drawable.tray_icon_facing_right_shaded_for_mobile));
                        }
                        else {
                            mFloatingView.findViewById(R.id.collapsed_iv).setBackground(getResources().getDrawable(R.drawable.tray_icon_facing_left_shaded_for_mobile));
                        }

                        mDistanceRight.append((int)(mThresholdCheckX - event.getRawX()));
                        mDistanceLeft.append((int)event.getRawX());

                        //This will take care of the movement of tray all over the screen including all scenarios.
                        if(isEditClicked && isDragFirst)
                        {
                            if (!isTrayContentViewCollapsed) {
                                Log.d(TAG, "In viewCollapseCheck = " + isTrayContentViewCollapsed);
                                //params.x = initialX + (int) (event.getRawX() - initialTouchX) - Integer.parseInt(mDistanceRight.substring(0,2));
                                params.x = initialX + (int) (event.getRawX() - initialTouchX) - 50;
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            }
                            else if (checkQuadrant) {
                                Log.d(TAG, "In checkQuadrant edit_check= " + checkQuadrant);
                                params.x = initialX + (int) (event.getRawX() - initialTouchX) - Integer.parseInt(mDistanceRight.substring(0,3));
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            }
                        }
                        else if(isEditClicked)
                        {
                            if (!isTrayContentViewCollapsed) {
                                Log.d(TAG, "In viewCollapseCheck = " + isTrayContentViewCollapsed);
                                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            } else if (checkQuadrant) {
                                Log.d(TAG, "In checkQuadrant = " + checkQuadrant);
                                params.x = initialX + (int) (event.getRawX() - initialTouchX) - Integer.parseInt(mDistanceRight.substring(0,3));
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            } else if (isDragLeftStarted) {
                                Log.d(TAG, "In blinkCheck = " + isDragLeftStarted);
                                params.x = initialX + (int) (event.getRawX() - initialTouchX) + Integer.parseInt(mDistanceLeft.substring(0,3));
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            } else {
                                Log.d(TAG, "In else");
                                params.x = initialX + (int) (event.getRawX() - initialTouchX) - Integer.parseInt(mDistanceRight.substring(0,3));
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            }
                        }
                        else if(isDragFirst)
                        {
                            if (!isTrayContentViewCollapsed) {
                                Log.d(TAG, "In viewCollapseCheck isDragFirst= " + isTrayContentViewCollapsed);
                                //params.x = initialX + (int) (event.getRawX() - initialTouchX) - Integer.parseInt(mDistanceRight.substring(0,2));
                                params.x = initialX + (int) (event.getRawX() - initialTouchX) - 50;
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            } else if (checkQuadrant) {
                                Log.d(TAG, "In checkQuadrant isDragFirst= " + checkQuadrant);
                                params.x = initialX + (int) (event.getRawX() - initialTouchX) - Integer.parseInt(mDistanceRight.substring(0,3));
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            } else if (isDragLeftStarted) {
                                Log.d(TAG, "In blinkCheck isDragFirst= " + isDragLeftStarted);
                                params.x = initialX + (int) (event.getRawX() - initialTouchX) + Integer.parseInt(mDistanceLeft.substring(0,3));
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            } else {
                                Log.d(TAG, "In else isDragFirst");
                                params.x = initialX + (int) (event.getRawX() - initialTouchX) - Integer.parseInt(mDistanceRight.substring(0,3));
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            }
                        }
                        else if (!isTrayContentViewCollapsed) {
                            Log.d(TAG, "In viewCollapseCheck = " + isTrayContentViewCollapsed);
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        } else if (checkQuadrant) {
                            Log.d(TAG, "In checkQuadrant = " + checkQuadrant);
                            params.x = initialX + (int) (event.getRawX() - initialTouchX) - Integer.parseInt(mDistanceRight.substring(0,3));
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        } else if (isDragLeftStarted) {
                            Log.d(TAG, "In blinkCheck = " + isDragLeftStarted);
                            params.x = initialX + (int) (event.getRawX() - initialTouchX) + Integer.parseInt(mDistanceLeft.substring(0,3));
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        } else {
                            Log.d(TAG, "In else");
                            params.x = initialX + (int) (event.getRawX() - initialTouchX) - Integer.parseInt(mDistanceRight.substring(0,3));
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        }

                        Xdiff = (int) (event.getRawX() - initialTouchX);
                        Ydiff = (int) (event.getRawY() - initialTouchY);

                        //Collapse the expanded view when you move
                        if (mExpandedView != null) {

                            ((LinearLayout) mFloatingView.findViewById(R.id.expanded_container_right)).removeAllViews();
                            mCollapsedView.setVisibility(INVISIBLE);

                            final LinearLayout linearLayout = ((LinearLayout) mFloatingView.findViewById(R.id.expanded_container_left));

                            Animation animation = new TranslateAnimation(0f, 200f, 0.0f, 0.0f);
                            animation.setDuration(100);
                            linearLayout.setAnimation(animation);
                            linearLayout.removeAllViews();
                            try {
                                Thread.sleep(100);//30
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mCollapsedView.setVisibility(VISIBLE);
                            isDragLeftStarted = true;
                            mExpandedView = null;
                        }


                        Log.d(TAG, "ACTION_MOVE");
                        isDragStarted = true;

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mFloatingView, params);

                        return true;

                    case MotionEvent.ACTION_UP:
                        Xdiff = (int) (event.getRawX() - initialTouchX);
                        Ydiff = (int) (event.getRawY() - initialTouchY);

                        Log.d(TAG, "ACTION_UP");
                        isDragLeftStarted = false;

                        if(mDistanceRight.length() > 0)
                        {
                            mDistanceRight.delete(0, mDistanceRight.length());
                        }

                        if(mDistanceLeft.length() > 0)
                        {
                            mDistanceLeft.delete(0, mDistanceLeft.length());
                        }

                        //So that is click event.
                        if (Xdiff < 10 && Ydiff < 10 && !isDragStarted) {
                            if (mExpandedView == null) {
                                //When user clicks on the image view of the collapsed layout,
                                // inflate the expanded view in the appropriate position.
                                isTrayContentViewCollapsed = true;
                                if (mWhereAmI == 0 && mThresholdWhichkQuadrant == 0) {
                                    mFloatingView.findViewById(R.id.collapsed_iv).setBackground(getResources().getDrawable(R.drawable.tray_icon_facing_right_for_mobile));
                                    checkQuadrant = true;
                                    mFloatingView.findViewById(R.id.collapsed_iv).clearFocus();
                                    ((LinearLayout) mFloatingView.findViewById(R.id.expanded_container_left)).removeAllViews();
                                    mRightContainerView.removeAllViews();
                                    LayoutInflater.from(mContext).inflate(R.layout.tray_content_floor_view, mRightContainerView);

                                    if(isGlowAnimationActivated)
                                    {
                                        mFloatingView.findViewById(R.id.collapsed_iv_animate).setVisibility(GONE);
                                        mFloatingView.findViewById(R.id.collapsed_iv_animate_left).setVisibility(GONE);
                                    }

                                    mModeImage = (ImageView)mFloatingView.findViewById(R.id.mode_image);
                                    mModeImage.setImageResource(mTrayItemIconArr.getResourceId(0, 0));
                                    mModeLabel = (CustomTextView)mFloatingView.findViewById(R.id.mode_text);
                                    mModeLabel.setText(trayItemNames[0]);

                                    mEditImage = (ImageView)mFloatingView.findViewById(R.id.edit_image);
                                    mEditImage.setImageResource(mTrayItemIconArr.getResourceId(mTrayItemIconArr.length()-2, 0));
                                    mEditLabel = (CustomTextView)mFloatingView.findViewById(R.id.edit_text);
                                    mEditLabel.setText(trayItemNames[2]);

                                    if(isGridMode)
                                    {
                                        mModeImage.setImageResource(mTrayItemIconArr.getResourceId(mTrayItemIconArr.length()-3, 0));
                                        mModeLabel.setText(trayItemNames[1]);
                                    }

                                    if(isFloorSelectImage)
                                    {
                                        RelativeLayout floor_layout = (RelativeLayout)mFloatingView.findViewById(R.id.floor);
                                        mFloorImage = (ImageView)mFloatingView.findViewById(R.id.floor_image);
                                        mFloorImage.setImageResource(mTrayItemIconArr.getResourceId(mTrayItemIconArr.length()-1, 0));
                                        mFloorLabel = (CustomTextView)mFloatingView.findViewById(R.id.floor_text);
                                        mFloorLabel.setText(trayItemNames[3]);
                                        floor_layout.setOnClickListener(new OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                Toast.makeText(mContext, "Select pic from gallery", Toast.LENGTH_LONG).show();
                                                mOnItemClickedListener.onItemClicked("Select pic from gallery", null);
                                            }
                                        });
                                    }
                                    //TODO REVERSE THE VIEW
                                   /*ArrayList<View> views = new ArrayList<View>();
                                   for(int x = 0; x < mRightContainerView.getChildCount(); x++) {
                                       views.add(mRightContainerView.getChildAt(x));
                                   }
                                   mRightContainerView.removeAllViews();
                                   for(int x = views.size() - 1; x >= 0; x--) {
                                       mRightContainerView.addView(views.get(x));
                                   }*/
                                    //TODO REVERSE END

                                    try {
                                        mExpandedView = mRightContainerView;
                                        Thread.sleep(10);

                                        TrayAnimation.with(Techniques.ShakeLeft).playOn(mExpandedView);
                                        TrayAnimation.with(Techniques.ShakeLeft).playOn(mFloatingView.findViewById(R.id.collapsed_iv));
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    ((LinearLayout) mFloatingView.findViewById(R.id.expanded_container_right)).removeAllViews();
                                    mLeftContainerView.removeAllViews();
                                    LayoutInflater.from(mContext).inflate(R.layout.tray_content_floor_view, mLeftContainerView);

                                    if(isGlowAnimationActivated)
                                    {
                                        mFloatingView.findViewById(R.id.collapsed_iv_animate).setVisibility(GONE);
                                        mFloatingView.findViewById(R.id.collapsed_iv_animate_left).setVisibility(GONE);
                                    }

                                    mModeImage = (ImageView)mFloatingView.findViewById(R.id.mode_image);
                                    mModeImage.setImageResource(mTrayItemIconArr.getResourceId(0, 0));
                                    mModeLabel = (CustomTextView)mFloatingView.findViewById(R.id.mode_text);
                                    mModeLabel.setText(trayItemNames[0]);

                                    mEditImage = (ImageView)mFloatingView.findViewById(R.id.edit_image);
                                    mEditImage.setImageResource(mTrayItemIconArr.getResourceId(mTrayItemIconArr.length()-2, 0));
                                    mEditLabel = (CustomTextView)mFloatingView.findViewById(R.id.edit_text);
                                    mEditLabel.setText(trayItemNames[2]);

                                    if(isGridMode)
                                    {
                                        mModeImage.setImageResource(mTrayItemIconArr.getResourceId(mTrayItemIconArr.length()-3, 0));
                                        mModeLabel.setText(trayItemNames[1]);
                                    }

                                    if(isFloorSelectImage)
                                    {
                                        RelativeLayout floor_layout = (RelativeLayout)mFloatingView.findViewById(R.id.floor);
                                        mFloorImage = (ImageView)mFloatingView.findViewById(R.id.floor_image);
                                        mFloorImage.setImageResource(mTrayItemIconArr.getResourceId(mTrayItemIconArr.length()-1, 0));
                                        mFloorLabel = (CustomTextView)mFloatingView.findViewById(R.id.floor_text);
                                        mFloorLabel.setText(trayItemNames[3]);
                                        floor_layout.setOnClickListener(new OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                Toast.makeText(mContext, "Select pic from gallery", Toast.LENGTH_LONG).show();
                                                mOnItemClickedListener.onItemClicked("Select pic from gallery", null);
                                            }
                                        });
                                    }

                                    ArrayList<View> views = new ArrayList<View>();
                                    for (int x = 0; x < mLeftContainerView.getChildCount(); x++) {
                                        views.add(mLeftContainerView.getChildAt(x));
                                    }
                                    mLeftContainerView.removeAllViews();
                                    for (int x = views.size() - 1; x >= 0; x--) {
                                        mLeftContainerView.addView(views.get(x));
                                    }


                                    mExpandedView = mLeftContainerView;
                                    TrayAnimation.with(Techniques.Shake).playOn(mFloatingView.findViewById(R.id.collapsed_iv));
                                    TrayAnimation.with(Techniques.Shake).playOn(mExpandedView);
                                }

                                imageFunctionality();
                                //initExpandedView();

                            } else {
                                if (v.getId() == R.id.collapsed_iv) {

                                    isTrayContentViewCollapsed = false;
                                    ((LinearLayout) mFloatingView.findViewById(R.id.expanded_container_right)).removeAllViews();
                                    ((LinearLayout) mFloatingView.findViewById(R.id.expanded_container_left)).removeAllViews();
                                    mExpandedView = null;

                                    if(isGlowAnimationActivated && mWhereAmI == 0)
                                    {
                                        mHandler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                mFloatingView.findViewById(R.id.collapsed_iv_animate).setVisibility(VISIBLE);
                                            }
                                        }, 1000);
                                    }
                                    else if(isGlowAnimationActivated)
                                    {
                                        mHandler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                mFloatingView.findViewById(R.id.collapsed_iv_animate_left).setVisibility(VISIBLE);
                                            }
                                        }, 1000);
                                    }

                                    try {
                                        Thread.sleep(5);
                                        if (checkQuadrant) {
                                            TrayAnimation.with(Techniques.Shake).playOn(mFloatingView.findViewById(R.id.collapsed_iv));
                                        } else {
                                            TrayAnimation.with(Techniques.ShakeLeft).playOn(mFloatingView.findViewById(R.id.collapsed_iv));
                                        }
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }
                        }

                        // align the view to the nearest border.
                        if (isDragStarted) {
                            isTrayContentViewCollapsed = false;
                            isDragStarted = false;

                            alignView(event.getRawX(), event.getRawY());

                            if(isGlowAnimationActivated && mWhereAmI == 0)
                            {
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mFloatingView.findViewById(R.id.collapsed_iv_animate).setVisibility(VISIBLE);
                                    }
                                }, 1000);
                            }
                            else if(isGlowAnimationActivated)
                            {
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mFloatingView.findViewById(R.id.collapsed_iv_animate_left).setVisibility(VISIBLE);
                                    }
                                }, 1000);
                            }

                                /*if(checkQuadrant && thresholdCheckQuad == 0)*/
                            if (mWhereAmI == 0 && mThresholdWhichkQuadrant == 0) {
                                TrayAnimation.with(Techniques.Shake).playOn(mFloatingView.findViewById(R.id.collapsed_iv));
                            } else {
                                TrayAnimation.with(Techniques.ShakeLeft).playOn(mFloatingView.findViewById(R.id.collapsed_iv));
                            }
                        }

                        return true;

                    default:
                        Log.d(TAG, "Default");
                }
                return false;
            }
        });
    }

    // align the view to the nearest border.
    /*This method is used for alliging the view to the nearest place from where you leave.*/
    private void alignView(float rawX, float rawY) {

        if (rawY > mThresholdCheckY && rawX > mThresholdCheckX / 2) {
            params.x = metrics.widthPixels;
            params.y = metrics.heightPixels - 300;
            mThresholdWhichkQuadrant = 0;
            mWhereAmI = 0;
            checkQuadrant = true;
            isDragFirst = true;
            mFloatingView.findViewById(R.id.collapsed_iv).setBackground(getResources().getDrawable(R.drawable.tray_icon_facing_right_for_mobile));
        } else if (rawX < mThresholdCheckX / 2 && rawY > mThresholdCheckY) {
            params.x = 0;
            params.y = metrics.heightPixels - 300;
            mThresholdWhichkQuadrant = 1;
            mWhereAmI = 1;
            checkQuadrant = false;
            isDragFirst = false;
            mFloatingView.findViewById(R.id.collapsed_iv).setBackground(getResources().getDrawable(R.drawable.tray_icon_facing_left_for_mobile));
        } else if (rawX < metrics.widthPixels / 2 && rawX < metrics.heightPixels / 2) {
            // 1st quadrant..// align left top section
            params.x = 0;
            params.y = (int) rawY - viewHeight;
            mWhereAmI = 1;
            checkQuadrant = false;
            mThresholdWhichkQuadrant = 1;
            Log.d(TAG, "Quad 1");
            isDragFirst = false;
            mFloatingView.findViewById(R.id.collapsed_iv).setBackground(getResources().getDrawable(R.drawable.tray_icon_facing_left_for_mobile));
        } else if (rawX < metrics.widthPixels / 2 && rawX >= metrics.heightPixels / 2) {
            // 3rd quadrant..// align left bottom section
            params.x = 0;
            params.y = (int) rawY - viewHeight;
            mWhereAmI = 1;
            checkQuadrant = false;
            mThresholdWhichkQuadrant = 1;
            Log.d(TAG, "Quad 3");
            isDragFirst = false;
            mFloatingView.findViewById(R.id.collapsed_iv).setBackground(getResources().getDrawable(R.drawable.tray_icon_facing_left_for_mobile));
        } else if (rawX > metrics.widthPixels / 2 && rawX < metrics.heightPixels / 2) {
            // 2nd quadrant..// align right top section
            params.x = metrics.widthPixels - viewWidth;
            params.y = (int) rawY - viewHeight;
            mWhereAmI = 0;
            checkQuadrant = true;
            mThresholdWhichkQuadrant = 0;
            isDragFirst = false;
            Log.d(TAG, "Quad 2");
            mFloatingView.findViewById(R.id.collapsed_iv).setBackground(getResources().getDrawable(R.drawable.tray_icon_facing_right_for_mobile));
        } else {
            // 4th quadrant..// align right bottom section
            params.x = metrics.widthPixels - viewWidth;
            params.y = (int) rawY - viewHeight;
            mWhereAmI = 0;
            checkQuadrant = true;
            mThresholdWhichkQuadrant = 0;
            isDragFirst = false;
            Log.d(TAG, "Quad 4");
            mFloatingView.findViewById(R.id.collapsed_iv).setBackground(getResources().getDrawable(R.drawable.tray_icon_facing_right_for_mobile));
        }
        mWindowManager.updateViewLayout(mFloatingView, params);
    }


    //Glow animation
    /*This method is used for animation of glow when you close the extended view.*/
    private void initFadeInFadeOutAnimation(boolean viewInEditMode) {

        View view = mFloatingView.findViewById(R.id.collapsed_iv_animate);
        View viewLeft = mFloatingView.findViewById(R.id.collapsed_iv_animate_left);

        if (viewInEditMode && mAnimationSet == null) {
            /*try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            view.setVisibility(View.VISIBLE);
            viewLeft.setVisibility(VISIBLE);

            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
            fadeOut.setDuration(400);
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
            fadeIn.setDuration(1500);

            ObjectAnimator fadeLeftOut = ObjectAnimator.ofFloat(viewLeft, "alpha", 0f, 1f);
            fadeLeftOut.setDuration(400);
            ObjectAnimator fadeLeftIn = ObjectAnimator.ofFloat(viewLeft, "alpha", 1f, 0f);
            fadeLeftIn.setDuration(1500);

            mAnimationSet = new AnimatorSet();
            mAnimationSet.play(fadeIn).after(fadeOut);
            mAnimationSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mAnimationSet.start();
                }
            });
            mAnimationSet.start();

            mAnimationLeftSet = new AnimatorSet();
            mAnimationLeftSet.play(fadeLeftIn).after(fadeLeftOut);
            mAnimationLeftSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mAnimationLeftSet.start();
                }
            });
            mAnimationLeftSet.start();

        } else {
            mAnimationSet.cancel();
            mAnimationLeftSet.cancel();
        }
    }

    //To remove the view call back function.
    public void removeView() {
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }

    /*public void setViews(int views)
    {
        layout = views;
    }*/

    public View getViews()
    {
        return mFloatingView;
    }

    public View getTrayView(){
        if(mFloatingView!=null)
        {
            return mFloatingView;
        }
        else
            return null;
    }

    //Tray elements functionality handling function.
    public void imageFunctionality()
    {

        final RelativeLayout mModeLayout = (RelativeLayout)mFloatingView.findViewById(R.id.mode);
        final ImageView mModeImage = (ImageView)mFloatingView.findViewById(R.id.mode_image);
        final CustomTextView mModeLabel = (CustomTextView)mFloatingView.findViewById(R.id.mode_text);

        //Toggle between "Grid Mode" and "Free Mode"
        mModeLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isModeClicked)
                {
                    mModeImage.setImageResource(mTrayItemIconArr.getResourceId(mTrayItemIconArr.length()-3, 0));
                    mModeLabel.setText(trayItemNames[1]);
                    isModeClicked = true;
                    isGridMode = true;
                    isfreeMode = false;
                    mOnItemClickedListener.onItemClicked("icon", null);
                }
                else {
                    mModeImage.setImageResource(mTrayItemIconArr.getResourceId(0, 0));
                    mModeLabel.setText(trayItemNames[0]);
                    isModeClicked = false;
                    isfreeMode = true;
                    isGridMode = false;
                    mOnItemClickedListener.onItemClicked("status", null);
                }
            }
        });

        //On click of Edit Layout the Floor Layout will be enabled and viceversa
        final RelativeLayout mEditLayout = (RelativeLayout)mFloatingView.findViewById(R.id.edit);

        if(isEditClicked)
        {
            RelativeLayout mFloorLayout = (RelativeLayout)mFloatingView.findViewById(R.id.floor);
            mEditLayout.setBackgroundColor(getResources().getColor(R.color.odd));
            mFloatingView.findViewById(R.id.editView).setVisibility(VISIBLE);
            mFloorLayout.setVisibility(VISIBLE);
            mModeLayout.setBackgroundColor(getResources().getColor(R.color.tint_black));
            mModeLayout.setClickable(false);
        }

        mEditLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isEditClicked)
                {
                    mEditLayout.setBackgroundColor(getResources().getColor(R.color.odd));
                    RelativeLayout floor_layout = (RelativeLayout)mFloatingView.findViewById(R.id.floor);
                    floor_layout.setVisibility(VISIBLE);
                    mModeLayout.setBackgroundColor(getResources().getColor(R.color.tint_black));
                    mModeLayout.setClickable(false);
                    isEditClicked = true;
                    isFloorSelectImage = true;
                    mFloatingView.findViewById(R.id.editView).setVisibility(VISIBLE);
                    mOnItemClickedListener.onItemClicked("edit", null);

                    isGlowAnimationActivated = true;
                    mFloorImage = (ImageView)mFloatingView.findViewById(R.id.floor_image);
                    mFloorImage.setImageResource(mTrayItemIconArr.getResourceId(mTrayItemIconArr.length()-1, 0));
                    mFloorLabel = (CustomTextView)mFloatingView.findViewById(R.id.floor_text);
                    mFloorLabel.setText(trayItemNames[3]);

                    if(floor_layout.getVisibility() == VISIBLE)
                    {
                        floor_layout.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(mContext, "Select pic from gallery", Toast.LENGTH_LONG).show();
                                mOnItemClickedListener.onItemClicked("Select pic from gallery", null);
                            }
                        });
                    }
                }
                else
                {
                    if(mWhereAmI == 0 && mThresholdWhichkQuadrant == 0)
                    {
                        ((LinearLayout) mFloatingView.findViewById(R.id.expanded_container_right)).removeAllViews();
                        isEditClicked = false;
                        isFloorSelectImage = false;
                        isTrayContentViewCollapsed = false;
                        mExpandedView = null;
                        mOnItemClickedListener.onItemClicked("editClose", null);
                        if(isGlowAnimationActivated)
                        {
                            mFloatingView.findViewById(R.id.collapsed_iv_animate).setVisibility(GONE);
                            mFloatingView.findViewById(R.id.collapsed_iv_animate_left).setVisibility(GONE);
                            isGlowAnimationActivated = false;
                        }
                    }
                    else
                    {
                        ((LinearLayout) mFloatingView.findViewById(R.id.expanded_container_left)).removeAllViews();
                        isEditClicked = false;
                        isTrayContentViewCollapsed = false;
                        isFloorSelectImage = false;
                        mExpandedView = null;
                        mOnItemClickedListener.onItemClicked("editClose", null);
                        if(isGlowAnimationActivated)
                        {
                            mFloatingView.findViewById(R.id.collapsed_iv_animate).setVisibility(GONE);
                            mFloatingView.findViewById(R.id.collapsed_iv_animate_left).setVisibility(GONE);
                            isGlowAnimationActivated = false;
                        }
                    }
                }
            }
        });

    }
}
