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

  float getBearing(LatLng begin, LatLng end) {
    double lat = Math.abs(begin.latitude - end.latitude);
    double lng = Math.abs(begin.longitude - end.longitude);

    if (begin.latitude < end.latitude && begin.longitude < end.longitude) {
      return (float) (Math.toDegrees(Math.atan(lng / lat)));
    } else if (begin.latitude >= end.latitude && begin.longitude < end.longitude) {
      return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
    } else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude) {
      return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
    } else if (begin.latitude < end.latitude && begin.longitude >= end.longitude) {
      return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
    }
    return -1;
  }

  public static final int STRAIGHT_ANGLE = 180;
  public static final int FULL_ROTATION = 360;

  float calcMinAngle(float markerCurrentRotation, float markerNextRotation) {
    float angleDifference = (Math.abs(markerNextRotation - markerCurrentRotation));
    if (angleDifference > STRAIGHT_ANGLE) {
      if (markerCurrentRotation < 0) {
        markerNextRotation = (-FULL_ROTATION + angleDifference) + markerCurrentRotation;
      } else {
        markerNextRotation = (FULL_ROTATION - angleDifference) + markerCurrentRotation;
      }
    }
    return markerNextRotation > FULL_ROTATION
        ? markerNextRotation - FULL_ROTATION
        : markerNextRotation;
  }


}
