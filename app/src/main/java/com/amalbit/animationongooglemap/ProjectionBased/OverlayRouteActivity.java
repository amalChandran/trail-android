package com.amalbit.animationongooglemap.ProjectionBased;

import android.content.Intent;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.amalbit.animationongooglemap.R;
import com.amalbit.animationongooglemap.data.Data;
import com.amalbit.animationongooglemap.util.SharedPreferenceHelper;
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
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OverlayRouteActivity extends AppCompatActivity implements OnMapReadyCallback,
    AdapterView.OnItemSelectedListener {

  private static String TAG = "OverlayRouteActivity";

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

  private TextView txtToMapFreq, txtFromTouchFreq;

  private EditText edtUpdateInterval;

  private Button btnupdateInterval;

  long lastTime = 0;

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

    edtUpdateInterval = findViewById(R.id.editText);
    btnupdateInterval = findViewById(R.id.btnRefresh);
    btnupdateInterval.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (edtUpdateInterval.getText() != null && edtUpdateInterval.getText().length() > 0) {
          String value= edtUpdateInterval.getText().toString();
          int interval =Integer.parseInt(value);
          SharedPreferenceHelper.setSharedPreferenceInterval(OverlayRouteActivity.this, interval);
          restartActivity();
        } else {
          Toast.makeText(OverlayRouteActivity.this, "Enter a valid number in ms for the interval", Toast.LENGTH_SHORT).show();
        }

      }
    });
    int interval = SharedPreferenceHelper.getSharedPreferenceInterval(OverlayRouteActivity.this);
    edtUpdateInterval.setHint(interval+"ms");
    txtToMapFreq = findViewById(R.id.txtToMapFreq);
    txtFromTouchFreq = findViewById(R.id.txtFromTouchFreq);
    mMapOverlayView = findViewById(R.id.mapOverlayView);
    mSpinner = findViewById(R.id.spinner_location);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
        R.array.array_place, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mSpinner.setAdapter(adapter);
    mSpinner.setOnItemSelectedListener(this);

    mSwitchCompat = findViewById(R.id.switch_btn);
    viewForTouchFeedback = findViewById(R.id.tempViewForOnSwipe);
    Observable.create(new ObservableOnSubscribe<MotionEvent>() {
      private float dX, dY;
      private float overlayDx, overlayDy;
      private long touchStartTime;
      @Override
      public void subscribe(ObservableEmitter<MotionEvent> emitter) throws Exception {
        viewForTouchFeedback.setMotionEventListner(new OnInterceptTouchListener() {
          @Override
          public void onTouchEvent(MotionEvent event) {
            //Pass the touch to overlay
            switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN:
                mIsDragging = false;
                dX = mMapOverlayView.getX() - event.getRawX();
                dY = mMapOverlayView.getY() - event.getRawY();
                mOriginalTouchPosition.set(event.getRawX(), event.getRawY());

                if(mapFragment.getView() != null) {
                  mapFragment.getView().dispatchTouchEvent(event);
                }

                touchStartTime = System.currentTimeMillis();
                emitter.onNext(event);
                break;
              case MotionEvent.ACTION_MOVE:
                long currentTime  = System.currentTimeMillis();
                Log.d(TAG, " Motion normal : " + (currentTime - lastTime));
                txtFromTouchFreq.setText((currentTime - lastTime) + "ms");
                lastTime = currentTime;
                emitter.onNext(event);
                break;
              case MotionEvent.ACTION_UP:
                if(mapFragment.getView() != null) {
                  mapFragment.getView().dispatchTouchEvent(event);
                }
                if (!mIsDragging) {
//              Log.d(TAG, "ACTION_UP: Tap.");
                } else {
//              Log.d(TAG, "ACTION_UP: Released from dragging.");
                }
                mIsDragging = false;
                emitter.onNext(event);
                break;
            }
          }
        });
      }
    })
        .buffer(interval, TimeUnit.MILLISECONDS)
        .filter(new Predicate<List<MotionEvent>>() {
          @Override
          public boolean test(List<MotionEvent> list) {
            return list != null && list.size() > 0;
          }
        })
        // Run on a background thread
        .subscribeOn(Schedulers.io())
        // Be notified on the main thread
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<List<MotionEvent>>() {
          long lastTimeThrottle = 0;

          @Override
          public void onSubscribe(Disposable d) {
            Log.d(TAG, " onSubscribe : " + d.isDisposed());
          }

          @Override
          public void onNext(List<MotionEvent> value) {
            if (mapFragment.getView() != null) {
//                onMotionEvent(value);
                mapFragment.getView().dispatchTouchEvent(value.get(value.size() - 1));
            }
            long currentTime  = System.currentTimeMillis();
            Log.d(TAG, " Motion throttled : " + (currentTime - lastTimeThrottle));
            txtToMapFreq.setText((currentTime - lastTimeThrottle) + "ms");
            lastTimeThrottle = currentTime;
          }

          @Override
          public void onError(Throwable e) {
            Log.d(TAG, " onError : " + e.getMessage());
          }

          @Override
          public void onComplete() {
            Log.d(TAG, " onComplete");
          }
        });

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      Window w = getWindow();
      w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }
  }


  private float dX, dY;
  private float overlayDx, overlayDy;
  private long touchStartTime;

  private void onMotionEvent(MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        mIsDragging = false;
        dX = mMapOverlayView.getX() - event.getRawX();
        dY = mMapOverlayView.getY() - event.getRawY();
        mOriginalTouchPosition.set(event.getRawX(), event.getRawY());

//                if(mapFragment.getView() != null) {
//                  mapFragment.getView().dispatchTouchEvent(event);
//                }

        touchStartTime = System.currentTimeMillis();

        break;
      case MotionEvent.ACTION_MOVE:

        float dragDeltaX = event.getRawX() - mOriginalTouchPosition.x;
        float dragDeltaY = event.getRawY() - mOriginalTouchPosition.y;

        if (mIsDragging || !isTouchWithinSlopOfOriginalTouch(dragDeltaX, dragDeltaY)) {
          if (!mIsDragging) {
            // Dragging just started
            mIsDragging = true;
          } else {
            float currentOverlayPositionX = event.getRawX() + dX;//530 413  = 117
            float currentOverlayPositionY = event.getRawY() + dY;
            PointF currentPoint = new PointF(currentOverlayPositionX, currentOverlayPositionY);

            overlayDx = overlayDx - currentPoint.x;
            overlayDy = overlayDy - currentPoint.y;

//            mMapOverlayView.setX(currentOverlayPositionX);
//            mMapOverlayView.setY(currentOverlayPositionY);
//            CameraUpdate update = CameraUpdateFactory.scrollBy(overlayDx, overlayDy);
//            long currentTime = System.currentTimeMillis();
//            Log.i("CameraUpdate", "Throttled : " + (currentTime - touchStartTime));
//            touchStartTime = currentTime;
//            mMap.moveCamera(update);

            overlayDx = currentPoint.x;
            overlayDy = currentPoint.y;
          }
        }

        break;
      case MotionEvent.ACTION_UP:
//                if(mapFragment.getView() != null) {
//                  mapFragment.getView().dispatchTouchEvent(event);
//                }
        if (!mIsDragging) {
//              Log.d(TAG, "ACTION_UP: Tap.");
        } else {
//              Log.d(TAG, "ACTION_UP: Released from dragging.");
        }
        mIsDragging = false;
        break;
    }
  }


  long onCameraMoveTime = 0;
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
        long currentTime  = System.currentTimeMillis();
        Log.d("onCameraMoveTime", "onCameraMoveTime : " + (currentTime - onCameraMoveTime));
        onCameraMoveTime = currentTime;
        mMapOverlayView.onCameraZoom(mMap);
      });

      Handler handler = new Handler();
      handler.postDelayed(() -> {
        drawRoute();
//        mSwitchCompat.setOnCheckedChangeListener((compoundButton, b) -> {
//          drawRoute();
//        });
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
//    if (!mSwitchCompat.isChecked()) {
      mMapOverlayView.drawPath(mRoute, mMap);
//    } else {
//      mMapOverlayView.drawArc(mRoute.get(0), mRoute.get(mRoute.size() - 1), mMap);
//    }
  }

  private boolean isTouchWithinSlopOfOriginalTouch(float dx, float dy) {
    double distance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
    return distance < mTapTouchSlop;
  }

  private void restartActivity() {
    Intent intent = getIntent();
    finish();
    startActivity(intent);
  }

}
