package com.amalbit.animationongooglemap.ProjectionBased;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.amalbit.animationongooglemap.R;
import com.amalbit.animationongooglemap.data.LatlngData;
import com.amalbit.trail.OverlayMarker;
import com.amalbit.trail.MarkerOverlayView;
import com.amalbit.trail.RouteOverlayView;
import com.amalbit.trail.RouteOverlayView.RouteType;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OverlayRouteActivity extends BaseActivity implements OnMapReadyCallback,
    AdapterView.OnItemSelectedListener {

  private static String TAG = "OverlayRouteActivity";

  private GoogleMap mMap;

  private MapStyleOptions mapStyle;

  private List<LatLng> mRoute;

  private RouteOverlayView mRouteOverlayView;

  private MarkerOverlayView mMarkerOverlayView;

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
    mRoute = LatlngData.getRoute();
    mapStyle = MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.ub__map_style);
  }

  OverlayMarker overlayMarker;

  public void onClick1(View view) {
    switch (view.getId()) {
      case R.id.btnAdd:
        zoomRoute(mRoute);
        drawRoute();

//        List<OverlayMarker> overlayMarkers = new ArrayList<>();
        Bitmap markerIcon = BitmapFactory.decodeResource(getResources(), R.drawable.car);
//        for (LatLng latLng : getRandomLocation(LatlngData.getRoute().get(0), 1000)) {
          overlayMarker = new OverlayMarker();
          overlayMarker.setIcon(markerIcon);
          overlayMarker.setLatLng(LatlngData.getRoute().get(0));
//          overlayMarkers.add(overlayMarker);
//        }
        mMarkerOverlayView.addMarker(overlayMarker, mMap.getProjection());
//        mMarkerOverlayView1.addMarker(overlayMarkers);
//        mMarkerOverlayView2.addMarker(overlayMarkers);

        //Two
        OverlayMarker overlayMarker1 = new OverlayMarker();
        overlayMarker1.setIcon(markerIcon);
        overlayMarker1.setLatLng(LatlngData.getRoute().get(20));
        mMarkerOverlayView.addMarker(overlayMarker1, mMap.getProjection());


        break;
      case R.id.btnRemove:
        mRouteOverlayView.removeRoute();
        if ( overlayMarker!=null) {
          overlayMarker.remove();
        }
        break;
    }
  }

  private void initUI() {
    mRouteOverlayView = findViewById(R.id.mapOverlayView);
    mMarkerOverlayView = findViewById(R.id.mapMarkerOverlayView);
    mMarkerOverlayView1 = findViewById(R.id.mapMarkerOverlayView1);
    mMarkerOverlayView2 = findViewById(R.id.mapMarkerOverlayView2);
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
//      mRouteOverlayView.onCameraMove(mMap);
      mMap.setOnCameraMoveListener(() -> {
            Projection projection = mMap.getProjection();
            CameraPosition cameraPosition = mMap.getCameraPosition();
            mRouteOverlayView.onCameraMove(projection, cameraPosition);
//          mMarkerOverlayView.onCameraMove(projection);
//          mMarkerOverlayView.invalidate();
//          mMarkerOverlayView1.onCameraMove(projection);
//          mMarkerOverlayView1.invalidate();
//          mMarkerOverlayView2.onCameraMove(projection);
//          mMarkerOverlayView2.invalidate();

        }
      );
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
    for (LatLng latLng : LatlngData.getRouteB()) {
      boundsBuilder.include(latLng);
    }

//    for (int i = 0; i < lstLatLngRoute.size()/4; i++) {
//      boundsBuilder.include(lstLatLngRoute.get(i));
//    }

    LatLngBounds latLngBounds = boundsBuilder.build();
    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0));
  }

  @Override
  public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
    switch (i) {
      case 0:
        mRoute = LatlngData.getRoute();
        break;
      case 1:
        mRoute = LatlngData.getTokyoRoute();
        break;
      case 2:
        mRoute = LatlngData.getNewYorkRoute();
        break;
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> adapterView) {
  }

  private void drawRoute() {
//    if (mSwitchCompat.isChecked()) {
    mRouteOverlayView.drawRoute(mRoute, mMap.getProjection(), mMap.getCameraPosition(), RouteType.PATH);
    mRouteOverlayView.drawRoute(LatlngData.getRouteB(), mMap.getProjection(), mMap.getCameraPosition(), RouteType.DASH);
    mRouteOverlayView.drawRoute(LatlngData.getRouteB(), mMap.getProjection(), mMap.getCameraPosition(), RouteType.ARC);
//    } else {
//      mMapOverlayView.drawArc(mRoute.get(0), mRoute.get(mRoute.size() - 1), mMap);
//    }
  }

  public List<LatLng> getRandomLocation(LatLng point, int radius) {

    List<LatLng> randomPoints = new ArrayList<>();
//    List<Float> randomDistances = new ArrayList<>();
    Location myLocation = new Location("");
    myLocation.setLatitude(point.latitude);
    myLocation.setLongitude(point.longitude);

    //This is to generate 10 random points
    for(int i = 0; i<50; i++) {
      double x0 = point.latitude;
      double y0 = point.longitude;

      Random random = new Random();

      // Convert radius from meters to degrees
      double radiusInDegrees = radius / 111000f;

      double u = random.nextDouble();
      double v = random.nextDouble();
      double w = radiusInDegrees * Math.sqrt(u);
      double t = 2 * Math.PI * v;
      double x = w * Math.cos(t);
      double y = w * Math.sin(t);

      // Adjust the x-coordinate for the shrinking of the east-west distances
      double new_x = x / Math.cos(y0);

      double foundLatitude = new_x + x0;
      double foundLongitude = y + y0;
      LatLng randomLatLng = new LatLng(foundLatitude, foundLongitude);
      randomPoints.add(randomLatLng);
//      Location l1 = new Location("");
//      l1.setLatitude(randomLatLng.latitude);
//      l1.setLongitude(randomLatLng.longitude);
//      randomDistances.add(l1.distanceTo(myLocation));
    }
    //Get nearest point to the centre
//    int indexOfNearestPointToCentre = randomDistances.indexOf(Collections.min(randomDistances));
    return randomPoints;
  }
}
