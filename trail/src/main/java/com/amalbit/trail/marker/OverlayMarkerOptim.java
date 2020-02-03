package com.amalbit.trail.marker;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;

import com.google.android.libraries.maps.Projection;
import com.google.android.libraries.maps.model.CameraPosition;
import com.google.android.libraries.maps.model.LatLng;


public class OverlayMarkerOptim {

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

    private Matrix rotateMatrix = new Matrix();

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        if (onMarkerUpdate != null) onMarkerUpdate.onMarkerUpdate();
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
        if (!OverlayMarkerOptim.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final OverlayMarkerOptim objectToBeCompared = (OverlayMarkerOptim) obj;
        return this.markerId == objectToBeCompared.markerId;
    }

    public OnMarkerUpdate getOnMarkerUpdate() {
        return onMarkerUpdate;
    }

    public void setOnMarkerUpdate(OnMarkerUpdate onMarkerUpdate) {
        this.onMarkerUpdate = onMarkerUpdate;
    }

    public Matrix getRotateMatrix() {
        return rotateMatrix;
    }

    public void setRotateMatrix(Matrix rotateMatrix) {
        this.rotateMatrix = rotateMatrix;
    }

    public interface MarkerRemoveListner {
        void onRemove(OverlayMarkerOptim overlayMarker);
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

    public void onCameraMove(Projection projection, CameraPosition cameraPosition) {

    }

//  public void set

    public void translatePathMatrix(float dx, float dy) {
//    translatePath(path, drawPath, matrix, dx, dy);
//    routeOverlay.invalidate();
    }

}
