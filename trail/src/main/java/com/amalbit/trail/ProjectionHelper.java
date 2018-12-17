package com.amalbit.trail;

import android.graphics.Point;
import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by amal.chandran on 04/11/17.
 */

class ProjectionHelper {

  private float x, y;

  private android.graphics.Point previousPoint;

  private boolean isRouteSet;

  private float previousZoomLevel = -1.0f;

  private boolean isZooming = false;

  private Projection mProjection;

  private CameraPosition mCameraPosition;

  protected LatLng mLineChartCenterLatLng;

  public void setCenterLatLng(LatLng lineChartCenterLatLng) {
    mLineChartCenterLatLng = lineChartCenterLatLng;
    isRouteSet = true;
  }

  public void onCameraMove(float x, float y, MapOverlayView mMapOverlayView) {
//    mCameraPosition = mMap.getCameraPosition();
//    if (previousZoomLevel != mCameraPosition.zoom) {
//      isZooming = true;
//    }
//    previousZoomLevel = mCameraPosition.zoom;
//    mProjection = mMap.getProjection();
//    android.graphics.Point point;
//    if (mLineChartCenterLatLng == null) {
//      point = new Point(mMapOverlayView.getWidth() / 2,
//          mMapOverlayView.getHeight() / 2);
//    } else {
//      point = mProjection.toScreenLocation(mLineChartCenterLatLng);
//    }
//
//    if (previousPoint != null) {
//      x = previousPoint.x - point.x;
//      y = previousPoint.y - point.y;
//    }

    if (isRouteSet) {
//      if (isZooming) {
//        mMapOverlayView.zoom(mCameraPosition.zoom);
//      }
      AdditiveAnimator.animate(mMapOverlayView).rotation(-mCameraPosition.bearing).start();
      AdditiveAnimator.animate(mMapOverlayView).translationXBy(-x).translationYBy(-y).start();
    }
//    previousPoint = point;
  }

  public void onCameraZoom(GoogleMap mMap, MapOverlayView mMapOverlayView) {
    mCameraPosition = mMap.getCameraPosition();
    if (previousZoomLevel != mCameraPosition.zoom) {
      isZooming = true;
    }
    previousZoomLevel = mCameraPosition.zoom;
    mProjection = mMap.getProjection();
    android.graphics.Point point;
    if (mLineChartCenterLatLng == null) {
      point = new Point(mMapOverlayView.getWidth() / 2,
          mMapOverlayView.getHeight() / 2);
    } else {
      point = mProjection.toScreenLocation(mLineChartCenterLatLng);
    }

    if (previousPoint != null) {
      x = previousPoint.x - point.x;
      y = previousPoint.y - point.y;
    }

    if (isRouteSet) {
      if (isZooming) {
        mMapOverlayView.zoom(mCameraPosition.zoom);
      }
      AdditiveAnimator.animate(mMapOverlayView).rotation(-mCameraPosition.bearing).start();
      AdditiveAnimator.animate(mMapOverlayView).translationXBy(-x).translationYBy(-y).start();

    }
    previousPoint = point;
  }

  public Projection getProjection() {
    return mProjection;
  }

  public CameraPosition getCameraPosition() {
    return mCameraPosition;
  }
}
