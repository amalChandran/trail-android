package com.amalbit.animationongooglemap.ProjectionBased;

import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.amalbit.animationongooglemap.R;
import com.amalbit.animationongooglemap.data.Data;
import com.amalbit.trail.RouteOverlayView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import java.util.List;

public class OverlayRouteActivity extends BaseActivity implements OnMapReadyCallback,
    AdapterView.OnItemSelectedListener {

  private static String TAG = "OverlayRouteActivity";

  private GoogleMap mMap;

  private MapStyleOptions mapStyle;

  private List<LatLng> mRoute;

  private RouteOverlayView mRouteOverlayView;

  private Spinner mSpinner;

  private SwitchCompat mSwitchCompat;

  private SupportMapFragment mapFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_projection_route);
    initUI();
    mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);
    mRoute = Data.getNewYorkRoute();
    mapStyle = MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.ub__map_style);
  }

  private void initUI() {
    mRouteOverlayView = findViewById(R.id.mapOverlayView);
    mSpinner = findViewById(R.id.spinner_location);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
        R.array.array_place, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mSpinner.setAdapter(adapter);
    mSpinner.setOnItemSelectedListener(this);

    mSwitchCompat = findViewById(R.id.switch_btn);
  }

  @Override
  public void onMapReady(final GoogleMap map) {
    mMap = map;
    mMap.setMapStyle(mapStyle);
    mMap.getUiSettings().setRotateGesturesEnabled(false);
    mMap.getUiSettings().setTiltGesturesEnabled(false);
//    mMap.setMaxZoomPreference(22);

    mMap.setOnMapLoadedCallback(() -> {
      mMap.setPadding(0,0,0,200);
      zoomRoute(mRoute);
      drawRoute();
      mRouteOverlayView.onCameraMove(mMap);
      mMap.setOnCameraMoveListener(() -> mRouteOverlayView.onCameraMove(mMap));
    });
  }

  public void zoomRoute(List<LatLng> lstLatLngRoute) {

    if (mMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) {
      return;
    }
    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
//    for (LatLng latLngPoint : lstLatLngRoute) {
//      boundsBuilder.include(latLngPoint);
//    }

    for (int i = 0; i < lstLatLngRoute.size()/2; i++) {
      boundsBuilder.include(lstLatLngRoute.get(i));
    }

    LatLngBounds latLngBounds = boundsBuilder.build();
    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0));
  }

  @Override
  public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
    switch (i) {
      case 0:
        mRoute = Data.getNewYorkRoute();
        break;
      case 1:
        mRoute = Data.getTokyoRoute();
        break;
      case 2:
        mRoute = Data.getRoute();
        break;
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> adapterView) {
  }

  private void drawRoute() {
//    if (mSwitchCompat.isChecked()) {
      mRouteOverlayView.drawPath(mRoute, mMap);
//    } else {
//      mMapOverlayView.drawArc(mRoute.get(0), mRoute.get(mRoute.size() - 1), mMap);
//    }
  }
}
