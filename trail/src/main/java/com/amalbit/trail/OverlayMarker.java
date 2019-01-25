package com.amalbit.trail;

import android.graphics.Bitmap;
import android.graphics.Point;
import com.google.android.gms.maps.model.LatLng;

public class OverlayMarker {

  private LatLng latLng;

  private Bitmap icon;

  private Point screenPoint;

  private MarkerRemoveListner markerRemoveListner;

  public LatLng getLatLng() {
    return latLng;
  }

  public void setLatLng(LatLng latLng) {
    this.latLng = latLng;
  }

  public Bitmap getIcon() {
    return icon;
  }

  public void setIcon(Bitmap icon) {
    this.icon = icon;
  }

  public Point getScreenPoint() {
    return screenPoint;
  }

  public void setScreenPoint(Point screenPoint) {
    this.screenPoint = screenPoint;
  }

  public MarkerRemoveListner getMarkerRemoveListner() {
    return markerRemoveListner;
  }

  public void setMarkerRemoveListner(MarkerRemoveListner markerRemoveListner) {
    this.markerRemoveListner = markerRemoveListner;
  }

  public void remove() {
    if (markerRemoveListner != null) {
      markerRemoveListner.onRemove(this);
    }
  }

  public interface MarkerRemoveListner {
    void onRemove(OverlayMarker overlayMarker);
  }
}
