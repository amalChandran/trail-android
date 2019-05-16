package com.amalbit.animationongooglemap.projectionBased;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import java.util.ArrayList;
import java.util.List;

public class BaseActivity extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      Window w = getWindow();
      w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }
  }

  public void setMapBounds(GoogleMap googleMap) {
    List<LatLng> latLngs = new ArrayList<>();
    latLngs.add(new LatLng(12.9715002, 77.6374856));//NW
    latLngs.add(new LatLng(12.9703733, 77.6372037));//NE
    latLngs.add(new LatLng(12.9595674, 77.6366595));//SE
    latLngs.add(new LatLng(12.9595672, 77.6519803));//SW
    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
    for (LatLng latLngPoint : latLngs) {
      boundsBuilder.include(latLngPoint);
    }
    LatLngBounds latLngBounds = boundsBuilder.build();
    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
  }

}
