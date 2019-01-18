package com.amalbit.trail;

import android.graphics.drawable.Drawable;
import com.google.android.gms.maps.model.LatLng;

public class OverlayMarker {

  private LatLng latLng;

  private Drawable overLayDrawable;

  private float orientation;

  public LatLng getLatLng() {
    return latLng;
  }

  public void setLatLng(LatLng latLng) {
    this.latLng = latLng;
  }

  public Drawable getOverLayDrawable() {
    return overLayDrawable;
  }

  public void setOverLayDrawable(Drawable overLayDrawable) {
    this.overLayDrawable = overLayDrawable;
  }

  public float getOrientation() {
    return orientation;
  }

  public void setOrientation(float orientation) {
    this.orientation = orientation;
  }
}
