package com.amalbit.animationongooglemap.polylineBased;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import com.amalbit.animationongooglemap.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

  private static final LatLng POINT_A = new LatLng(12.922294704121231, 77.61939525604248);

  private static final LatLng POINT_B = new LatLng(12.933065305628435, 77.62390136718749);

  private List<LatLng> bangaloreRoute;

  private GoogleMap mMap;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);
    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    createRoute();
  }

  private void createRoute() {
    if (bangaloreRoute == null) {
      bangaloreRoute = new ArrayList<>();
    } else {
      bangaloreRoute.clear();
    }

    bangaloreRoute.add(new LatLng(12.922294704121231, 77.61939525604248));
    bangaloreRoute.add(new LatLng(12.924637088068884, 77.6180648803711));
    bangaloreRoute.add(new LatLng(12.925557304321782, 77.6200819015503));
    bangaloreRoute.add(new LatLng(12.927104933097784, 77.62081146240234));
    bangaloreRoute.add(new LatLng(12.928234277770715, 77.62111186981201));
    bangaloreRoute.add(new LatLng(12.92990737159723, 77.6218843460083));
    bangaloreRoute.add(new LatLng(12.9337554448302, 77.62342929840088));
    bangaloreRoute.add(new LatLng(12.9346338010532, 77.62390136718749));
    bangaloreRoute.add(new LatLng(12.935177543831987, 77.62437343597412));
    bangaloreRoute.add(new LatLng(12.934487408564122, 77.62561798095703));
    bangaloreRoute.add(new LatLng(12.934320102757125, 77.62589693069457));
    bangaloreRoute.add(new LatLng(12.933860011209374, 77.62572526931763));
    bangaloreRoute.add(new LatLng(12.934550148212828, 77.62460947036743));
    bangaloreRoute.add(new LatLng(12.933379005502244, 77.62398719787598));
    bangaloreRoute.add(new LatLng(12.933065305628435, 77.62390136718749));
  }


  /**
   * Manipulates the map once available.
   * This callback is triggered when the map is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera. In this case,
   * we just add a marker near Sydney, Australia.
   * If Google Play services is not installed on the device, the user will be prompted to install
   * it inside the SupportMapFragment. This method will only be triggered once the user has
   * installed Google Play services and returned to the app.
   */
  @Override
  public void onMapReady(GoogleMap map) {
    mMap = map;

    mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
      @Override
      public void onMapLoaded() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(POINT_A);
        builder.include(POINT_B);
        LatLngBounds bounds = builder.build();
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 200);

        mMap.moveCamera(cu);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);

        startAnim();
      }
    });
  }

  private void startAnim() {
    if (mMap != null) {
      MapAnimator.getInstance().animateRoute(mMap, bangaloreRoute);
    } else {
      Toast.makeText(getApplicationContext(), "Map not ready", Toast.LENGTH_LONG).show();
    }
  }

  public void resetAnimation(View view) {
    startAnim();
  }
}
