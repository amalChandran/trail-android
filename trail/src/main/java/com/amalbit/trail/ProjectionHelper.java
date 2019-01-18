package com.amalbit.trail;

import android.graphics.Point;
import android.util.Log;
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

  public Point point;

  public void setCenterLatLng(LatLng lineChartCenterLatLng) {
    mLineChartCenterLatLng = lineChartCenterLatLng;
    isRouteSet = true;
  }

  void onCameraMove(GoogleMap mMap, RouteOverlayView mRouteOverlayView) {
    if (isRouteSet) {
      CameraPosition mCameraPosition = mMap.getCameraPosition();
      if (previousZoomLevel != mCameraPosition.zoom) {
        isZooming = true;
      }
      previousZoomLevel = mCameraPosition.zoom;
      Projection mProjection = mMap.getProjection();

      point = mProjection.toScreenLocation(mLineChartCenterLatLng);

      if (previousPoint != null) {
        x = previousPoint.x - point.x;
        y = previousPoint.y - point.y;
      }
      if (isZooming) {
        mRouteOverlayView.scalePathMatrix(mCameraPosition.zoom);
        isZooming = false;
      }
      mRouteOverlayView.translatePathMatrix(-x, -y);

      Log.i("onCameraMove", "x,y : " + x + "," + y);
      previousPoint = point;
    }
  }
}
