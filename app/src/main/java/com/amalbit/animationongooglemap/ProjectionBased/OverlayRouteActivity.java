package com.amalbit.animationongooglemap.ProjectionBased;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import com.amalbit.animationongooglemap.R;
import com.amalbit.animationongooglemap.data.LatlngData;
import com.amalbit.trail.Route;
import com.amalbit.trail.RouteOverlayView;
import com.amalbit.trail.RouteOverlayView.RouteType;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import java.util.List;

public class OverlayRouteActivity extends BaseActivity implements OnMapReadyCallback {

  private static String TAG = "OverlayRouteActivity";

  private GoogleMap mMap;

  private MapStyleOptions mapStyle;

  private List<LatLng> mRoute;

  private RouteOverlayView mRouteOverlayView;

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
    mapStyle = MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.map_style);
  }

  public void onClick1(View view) {
    switch (view.getId()) {
      case R.id.btnAdd:
        mRouteOverlayView.removeRoutes();
        zoomRoute(mRoute);
        break;
      case R.id.btnRemove:
        mRouteOverlayView.removeRoutes();
        break;
    }
  }

  private void initUI() {
    mRouteOverlayView = findViewById(R.id.mapOverlayView);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
        R.array.array_place, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
  }

  @Override
  public void onMapReady(final GoogleMap map) {
    mMap = map;
    mMap.setMapStyle(mapStyle);
    mMap.getUiSettings().setRotateGesturesEnabled(false);
    mMap.getUiSettings().setTiltGesturesEnabled(false);

    mMap.setOnMapLoadedCallback(() -> {
      mMap.setPadding(0, 0, 0, 250);
      zoomRoute(mRoute);
      drawRoutes();
      mMap.setOnCameraMoveListener(() -> {
            Projection projection = mMap.getProjection();
            CameraPosition cameraPosition = mMap.getCameraPosition();
            mRouteOverlayView.onCameraMove(projection, cameraPosition);

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

    LatLngBounds latLngBounds = boundsBuilder.build();

    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100), 200, new CancelableCallback() {
      @Override
      public void onFinish() {
        drawRoutes();
      }

      @Override
      public void onCancel() {

      }
    });
  }

  private void drawRoutes() {
    Route normalRoute = new Route.Builder(mRouteOverlayView)
        .setRouteType(RouteType.PATH)
        .setCameraPosition(mMap.getCameraPosition())
        .setProjection(mMap.getProjection())
        .setLatLngs(mRoute)
        .setBottomLayerColor(Color.YELLOW)
        .setTopLayerColor(Color.RED)
        .create();

    Route dashRoute = new Route.Builder(mRouteOverlayView)
        .setRouteType(RouteType.DASH)
        .setCameraPosition(mMap.getCameraPosition())
        .setProjection(mMap.getProjection())
        .setLatLngs(LatlngData.getRouteB())
        .setDashColor(Color.BLACK)
        .create();

    Route arcRoute = new Route.Builder(mRouteOverlayView)
        .setRouteType(RouteType.ARC)
        .setCameraPosition(mMap.getCameraPosition())
        .setProjection(mMap.getProjection())
        .setLatLngs(LatlngData.getRouteB())
        .setBottomLayerColor(Color.GRAY)
        .setTopLayerColor(Color.BLACK)
        .setRouteShadowColor(Color.GRAY)
        .create();
  }
}
