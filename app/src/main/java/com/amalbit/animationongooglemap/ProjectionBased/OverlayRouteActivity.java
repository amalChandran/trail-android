package com.amalbit.animationongooglemap.ProjectionBased;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import com.amalbit.animationongooglemap.R;
import com.amalbit.animationongooglemap.data.Data;
import com.amalbit.trail.TrailSupportMapFragment;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import java.util.List;

public class OverlayRouteActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private MapStyleOptions mapStyle;

    private List<LatLng> route;

    TrailSupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projection_route);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (TrailSupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        route = Data.getRoute();
        //mRouteOverlayView = findViewById(R.id.linechart);

        mapStyle = MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.mapstyle);

    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        mMap.setMapStyle(mapStyle);

        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.setMaxZoomPreference(18);

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override public void onMapLoaded() {

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(Data.POINT_A);
                builder.include(Data.POINT_B);
                LatLngBounds bounds = builder.build();
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 200);

                mMap.moveCamera(cu);
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);

                mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                    @Override public void onCameraMove() {
                        mapFragment.onCameraMove(mMap);
                    }
                });

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override public void run() {
                        mapFragment.setUpPath(route, mMap);
                    }
                }, 3000);
            }
        });
    }

}
