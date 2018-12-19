package com.amalbit.animationongooglemap.ProjectionBased;

import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.amalbit.animationongooglemap.R;
import com.amalbit.animationongooglemap.data.Data;
import com.amalbit.trail.MapOverlayView;
import com.amalbit.trail.TouchViewGroup;
import com.amalbit.trail.TouchViewGroup.OnInterceptTouchListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import java.util.List;

public class OverlayRouteActivity extends AppCompatActivity implements OnMapReadyCallback,
    AdapterView.OnItemSelectedListener {

  private static final String TAG = "OverlayRouteActivity";

  private GoogleMap mMap;

  private MapStyleOptions mapStyle;

  private List<LatLng> mRoute;

  private MapOverlayView mMapOverlayView;

  private Spinner mSpinner;

  private SwitchCompat mSwitchCompat;

  private TouchViewGroup viewForTouchFeedback;

  private SupportMapFragment mapFragment;

  private int mTapTouchSlop;

  private boolean mIsDragging;

  private PointF mOriginalTouchPosition = new PointF();

  private VelocityTracker mVelocityTracker = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_projection_route);
    initUI();
    mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);
    mRoute = Data.getRoute();
    mTapTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
    mapStyle = MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.ub__map_style);
  }

  private void initUI() {
    mMapOverlayView = findViewById(R.id.mapOverlayView);
    mSpinner = findViewById(R.id.spinner_location);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
        R.array.array_place, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mSpinner.setAdapter(adapter);
    mSpinner.setOnItemSelectedListener(this);

    mSwitchCompat = findViewById(R.id.switch_btn);
    viewForTouchFeedback = findViewById(R.id.tempViewForOnSwipe);
    viewForTouchFeedback.setMotionEventListner(new OnInterceptTouchListener() {
      private float dX, dY;
      @Override
      public void onTouchEvent(MotionEvent event) {
        int index = event.getActionIndex();
        int action = event.getActionMasked();
        int pointerId = event.getPointerId(index);


        //Pass the touch to overlay
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            mIsDragging = false;
            dX = mMapOverlayView.getX() - event.getRawX();
            dY = mMapOverlayView.getY() - event.getRawY();
            mOriginalTouchPosition.set(event.getRawX(), event.getRawY());

            if (mVelocityTracker == null) {
              // Retrieve a new VelocityTracker object to watch the velocity of a motion.
              mVelocityTracker = VelocityTracker.obtain();
            } else {
              // Reset the velocity tracker back to its initial state.
              mVelocityTracker.clear();
            }

            // Add a user's movement to the tracker.
            mVelocityTracker.addMovement(event);

            break;
          case MotionEvent.ACTION_MOVE:
            float dragDeltaX = event.getRawX() - mOriginalTouchPosition.x;
            float dragDeltaY = event.getRawY() - mOriginalTouchPosition.y;

            float currentPositionX = event.getRawX() + dX;
            float currentPositionY = event.getRawY() + dY;

            if (mIsDragging || !isTouchWithinSlopOfOriginalTouch(dragDeltaX, dragDeltaY)) {
              if (!mIsDragging) {
                // Dragging just started
                Log.d(TAG, "MOVE Start Drag.");
                mIsDragging = true;
              } else {
                mMapOverlayView.setX(currentPositionX);
                mMapOverlayView.setY(currentPositionY);
              }
            }

            //Velocity code
            mVelocityTracker.addMovement(event);
            // When you want to determine the velocity, call
            // computeCurrentVelocity(). Then call getXVelocity()
            // and getYVelocity() to retrieve the velocity for each pointer ID.
            mVelocityTracker.computeCurrentVelocity(1000);

            break;
          case MotionEvent.ACTION_UP:
            if (!mIsDragging) {
              Log.d(TAG, "ACTION_UP: Tap.");
            } else {
              Log.d(TAG, "ACTION_UP: Released from dragging.");

              Log.d(TAG, "Velocity tracker: " + VelocityTrackerCompat.getXVelocity(mVelocityTracker,
                  pointerId));
//              flingX.setStartVelocity(-VelocityTrackerCompat.getXVelocity(mVelocityTracker,
//                  pointerId))
//                  .setMinValue(0)
//                  .setMaxValue(1000)
//                  .setFriction(1.5f)
//                  .start();
//              FlingAnimation flingY = new FlingAnimation(mMapOverlayView, DynamicAnimation.SCROLL_Y);
//              flingY.setStartVelocity(-VelocityTrackerCompat.getYVelocity(mVelocityTracker,
//                  pointerId))
//                  .setMinValue(0)
//                  .setMaxValue(1000)
//                  .setFriction(1.1f)
//                  .start();
            }
            mIsDragging = false;
            break;
          case MotionEvent.ACTION_CANCEL:
            // Return a VelocityTracker object back to be re-used by others.
            mVelocityTracker.recycle();
            break;
        }

        if (mapFragment.getView() != null) {
          mapFragment.getView().dispatchTouchEvent(event);
        }
      }
    });

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      Window w = getWindow();
      w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }
  }

  private boolean isTouchWithinSlopOfOriginalTouch(float dx, float dy) {
    double distance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
    return distance < mTapTouchSlop;
  }

  @Override
  public void onMapReady(final GoogleMap map) {
    mMap = map;
    mMap.setMapStyle(mapStyle);
    mMap.getUiSettings().setRotateGesturesEnabled(true);
    mMap.getUiSettings().setTiltGesturesEnabled(false);
    mMap.setMaxZoomPreference(18);

    mMap.setOnMapLoadedCallback(() -> {
      zoomRoute(mRoute);
      mMap.setOnCameraMoveListener(() -> {
//        if (!mIsDragging) {
//          mMapOverlayView.onCameraZoom(mMap);
//        }
      });

      Handler handler = new Handler();
      handler.postDelayed(() -> {
        mSwitchCompat.setOnCheckedChangeListener((compoundButton, b) -> {
          drawRoute();
        });
      }, 1000);
    });
  }

  public void zoomRoute(List<LatLng> lstLatLngRoute) {

    if (mMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) {
      return;
    }
    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
    for (LatLng latLngPoint : lstLatLngRoute) {
      boundsBuilder.include(latLngPoint);
    }
    int routePadding = 100;
    LatLngBounds latLngBounds = boundsBuilder.build();
    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding));
  }

  @Override
  public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
    switch (i) {
      case 0:
        mRoute = Data.getRoute();
        break;
      case 1:
        mRoute = Data.getTokyoRoute();
        break;
      case 2:
        mRoute = Data.getNewYorkRoute();
        break;
    }

    zoomRoute(mRoute);
    drawRoute();
  }

  @Override
  public void onNothingSelected(AdapterView<?> adapterView) {

  }

  private void drawRoute() {
    if (!mSwitchCompat.isChecked()) {
      mMapOverlayView.drawPath(mRoute, mMap);
    } else {
      mMapOverlayView.drawArc(mRoute.get(0), mRoute.get(mRoute.size() - 1), mMap);
    }
  }
}
