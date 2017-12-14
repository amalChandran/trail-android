package com.amalbit.animationongooglemap.ProjectionBased;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import com.amalbit.animationongooglemap.R;
import com.amalbit.animationongooglemap.common.BaseCompatActivity;
import com.amalbit.animationongooglemap.data.Data;
import com.amalbit.trail.RouteOverlayView;
import com.amalbit.trail.TrailSupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import java.util.List;

public class OverlayRouteActivity extends BaseCompatActivity implements OnMapReadyCallback,
    AdapterView.OnItemSelectedListener {

    private GoogleMap mMap;

    private MapStyleOptions mapStyle;

    private List<LatLng> route;

    private TrailSupportMapFragment mapFragment;

    private Spinner mSpinner;

    private SwitchCompat mSwitchCompat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projection_route);

        mSpinner = findViewById(R.id.spinner_location);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
            R.array.array_place, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(this);

        mSwitchCompat = findViewById(R.id.switch_btn);

        mapFragment = (TrailSupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        route = Data.getRoute();

        mapStyle = MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.mapstyle);
    }

    @Override
    public void onMapReady(final GoogleMap map) {
        mMap = map;
        mMap.setMapStyle(mapStyle);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(false);
        mMap.setMaxZoomPreference(18);

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override public void onMapLoaded() {
                zoomRoute(route);
                mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                    @Override public void onCameraMove() {
                        mapFragment.onCameraMove(mMap);
                    }
                });

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override public void run() {
                        mapFragment.setUpPath(route, mMap, getCurrentAnimType());
                        mSwitchCompat.setChecked(true);
                        mSwitchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                mapFragment.setUpPath(route, mMap, getCurrentAnimType());
                            }
                        });
                    }
                }, 1000);
            }
        });
    }

    public void zoomRoute(List<LatLng> lstLatLngRoute) {

        if (mMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 100;
        LatLngBounds latLngBounds = boundsBuilder.build();

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding));
    }

    @Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (i) {
            case 0:
                route = Data.getRoute();
                break;
            case 1:
                route = Data.getTokyoRoute();
                break;
            case 2:
                route = Data.getNewYorkRoute();
                break;
        }

        zoomRoute(route);
        mapFragment.setUpPath(route, mMap, getCurrentAnimType());
    }

    @Override public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private RouteOverlayView.AnimType getCurrentAnimType() {
        if(mSwitchCompat.isChecked()) {
            return RouteOverlayView.AnimType.PATH;
        } else {
            return RouteOverlayView.AnimType.ARC;
        }
    }
}
