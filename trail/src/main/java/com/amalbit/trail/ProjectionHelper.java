package com.amalbit.trail;

import android.graphics.Point;
import android.util.Log;
import android.widget.FrameLayout;
import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
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

  LatLng mLineChartCenterLatLng;

  public void setCenterlatLng(LatLng lineChartCenterLatLng) {
    mLineChartCenterLatLng = lineChartCenterLatLng;
    isRouteSet = true;
  }

  public void onCameramove(GoogleMap mMap, RouteOverlayView mRouteOverlayView) {
    if(previousZoomLevel != mMap.getCameraPosition().zoom)
    {
      isZooming = true;
    }

    previousZoomLevel = mMap.getCameraPosition().zoom;

    Projection projection = mMap.getProjection();

    android.graphics.Point point;
    if(mLineChartCenterLatLng == null) {
      point = new Point(mRouteOverlayView.getWidth()/2,
          mRouteOverlayView.getHeight()/2);
    } else {
      point = projection.toScreenLocation(mLineChartCenterLatLng);
    }

    if (previousPoint != null) {
      x = previousPoint.x - point.x;
      y = previousPoint.y - point.y;
      Log.i("onCameraMove", "dx,dy : (" + x + "," + y + ")");
    }

    if (isRouteSet) {
      if(isZooming) {
        mRouteOverlayView.zoom(mMap.getCameraPosition().zoom);
      }

      //FrameLayout frameLayout = (FrameLayout) mRouteOverlayView.getParent();
      AdditiveAnimator.animate(mRouteOverlayView).rotation(-mMap.getCameraPosition().bearing).setDuration(2).start();
      AdditiveAnimator.animate(mRouteOverlayView).translationXBy(-x).translationYBy(-y).setDuration(2).start();
    }

    previousPoint = point;
  }

}
