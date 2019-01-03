package com.amalbit.animationongooglemap.ProjectionBased;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener;
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
import org.reactivestreams.Subscription;

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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_projection_route);
    initUI();
    mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);
    mRoute = Data.getRoute();
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

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      Window w = getWindow();
      w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    viewForTouchFeedback = findViewById(R.id.tempViewForOnSwipe);
    Observable.create(new ObservableOnSubscribe<MotionEvent>() {
      @Override
      public void subscribe(ObservableEmitter<MotionEvent> emitter) throws Exception {
        viewForTouchFeedback.setMotionEventListner(new OnInterceptTouchListener() {
          @Override
          public void onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN:
                if(mapFragment.getView() != null) {
                  mapFragment.getView().dispatchTouchEvent(event);
                }
                break;
              case MotionEvent.ACTION_MOVE:
                emitter.onNext(event);
                break;
              case MotionEvent.ACTION_UP:
                if(mapFragment.getView() != null) {
                  mapFragment.getView().dispatchTouchEvent(event);
                }
                break;
            }
          }
        });
      }
    }).buffer(25, TimeUnit.MILLISECONDS)
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
          @Override
          public void onSubscribe(Disposable d) {
            Log.d(TAG, " onSubscribe : " + d.isDisposed());
          }

          @Override
          public void onNext(List<MotionEvent> value) {
            if (mapFragment.getView() != null) {
              mapFragment.getView().dispatchTouchEvent(value.get(value.size() - 1));
            }
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

  }

  private boolean isDragging = false;
  private Disposable disposable;
  private long prevTimeDrag;

  private void dragStarted() {
    if (this.disposable != null) this.disposable.dispose();
    disposable = Observable.interval(16, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
        .subscribe(tick -> {
          if (mMap!= null)
          mMapOverlayView.onCameraMove(mMap);
          long curTime =  System.currentTimeMillis();
          //Log interval and the latlng
          Log.i("onTick", "duration: " + (curTime - prevTimeDrag));
          prevTimeDrag = curTime;
        });
  }

  private void dragEnd() {
    if (this.disposable != null) this.disposable.dispose();
  }

  private long prevTime;
  @Override
  public void onMapReady(final GoogleMap map) {
    mMap = map;
    mMap.setMapStyle(mapStyle);
    mMap.getUiSettings().setRotateGesturesEnabled(true);
    mMap.getUiSettings().setTiltGesturesEnabled(false);
    mMap.setMaxZoomPreference(18);

    mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
      @Override
      public void onMapLoaded() {
        zoomRoute(mRoute);
        mMap.setOnCameraMoveListener(() -> {
//          if(!isDragging) {
//            mMapOverlayView.onCameraMove(mMap);
//          }

          long curTime =  System.currentTimeMillis();
          //Log interval and the latlng
          Log.i("onMapLoaded", "duration: " + (curTime - prevTime));
          prevTime = curTime;
        });

        mMap.setOnCameraMoveStartedListener(new OnCameraMoveStartedListener() {
          @Override
          public void onCameraMoveStarted(int i) {
            if (!isDragging) {
              dragStarted();
            }
            isDragging = true;
          }
        });
        mMap.setOnCameraIdleListener(new OnCameraIdleListener() {
          @Override
          public void onCameraIdle() {
            isDragging = false;
            dragEnd();
          }
        });

        Handler handler = new Handler();
        handler.postDelayed(() -> {
          mSwitchCompat.setOnCheckedChangeListener((compoundButton, b) -> {
            drawRoute();
          });
        }, 1000);
      }
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
//    if (mSwitchCompat.isChecked()) {
      mMapOverlayView.drawPath(mRoute, mMap);
//    } else {
//      mMapOverlayView.drawArc(mRoute.get(0), mRoute.get(mRoute.size() - 1), mMap);
//    }
  }
}
