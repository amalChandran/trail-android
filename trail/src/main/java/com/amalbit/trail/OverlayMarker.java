package com.amalbit.trail;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import com.google.android.gms.maps.model.LatLng;

public class OverlayMarker {

  public static int count = 0;

  private int markerId = -1;

  private LatLng latLng;

  private float bearing;

  private Bitmap icon;

  private Point screenPoint;

  private MarkerRemoveListner markerRemoveListner;

  private OnMarkerUpdate onMarkerUpdate;

  private ValueAnimator translateValueAnimator;

  private ValueAnimator rotateValueAnimator;

  public LatLng getLatLng() {
    return latLng;
  }

  public void setLatLng(LatLng latLng) {
    //TODO update the canvas
    if (onMarkerUpdate!= null) onMarkerUpdate.onMarkerUpdate();
    this.latLng = latLng;
  }

  public Bitmap getIcon() {
    return icon;
  }

  public void setIcon(Bitmap icon) {
    this.icon = icon;
  }

  public void rotateIcon(float degrees) {
//    int width = icon.getWidth();
//    int height = icon.getHeight();
//
//    Matrix matrix = new Matrix();
//    matrix.postRotate(degrees);
//
//    Bitmap scaledBitmap = Bitmap.createScaledBitmap(icon, width, height, true);
//    icon = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
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

  public int getMarkerId() {
    return markerId;
  }

  public void setMarkerId(int markerId) {
    this.markerId = markerId;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!OverlayMarker.class.isAssignableFrom(obj.getClass())) {
      return false;
    }
    final OverlayMarker objectToBeCompared = (OverlayMarker) obj;
    return this.markerId == objectToBeCompared.markerId;
  }

  public OnMarkerUpdate getOnMarkerUpdate() {
    return onMarkerUpdate;
  }

  public void setOnMarkerUpdate(OnMarkerUpdate onMarkerUpdate) {
    this.onMarkerUpdate = onMarkerUpdate;
  }

  public interface MarkerRemoveListner {
    void onRemove(OverlayMarker overlayMarker);
  }

  public interface OnMarkerUpdate {
    public void onMarkerUpdate();
  }

  public float getBearing() {
    return bearing;
  }

  public void setBearing(float bearing) {
    this.bearing = bearing;
  }

  public ValueAnimator getTranslateValueAnimator() {
    return translateValueAnimator;
  }

  public void setTranslateValueAnimator(ValueAnimator translateValueAnimator) {
    this.translateValueAnimator = translateValueAnimator;
  }

  public ValueAnimator getRotateValueAnimator() {
    return rotateValueAnimator;
  }

  public void setRotateValueAnimator(ValueAnimator rotateValueAnimator) {
    this.rotateValueAnimator = rotateValueAnimator;
  }

//  private Bitmap rotateBitmap(Bitmap bitmap, float bearing) {
//
//    Matrix matrix = new Matrix();
//    matrix.setRotate(bearing);
//    bitmap.getWidth();
//    bitmap.getHeight();
//    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//  }


}
