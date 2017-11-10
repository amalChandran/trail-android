package com.amalbit.trail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by amal.chandran on 07/11/17.
 */

public class TrailMarker {

  private LatLng mLatLng;

  private Bitmap mBitmap;

  public TrailMarker(Context context, LatLng latLng, int resource) {
    mLatLng = latLng;
    mBitmap = BitmapFactory.decodeResource(context.getResources(), resource);
  }

  public LatLng getLatLng() {
    return mLatLng;
  }

  public Bitmap getBitmap() {
    return mBitmap;
  }
}
