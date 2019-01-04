package com.amalbit.trail;

import android.animation.ObjectAnimator;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;
import android.widget.FrameLayout;
import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import com.amalbit.trail.util.Tlog;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.TimeUnit;

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

  public void onCameraMove(GoogleMap mMap, MapOverlayView mMapOverlayView) {
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
      AdditiveAnimator.animate(mMapOverlayView).rotation(-mCameraPosition.bearing).setDuration(2).start();
//      AdditiveAnimator.animate(mMapOverlayView).translationXBy(-x).translationYBy(-y).setDuration(1).start();
      mMapOverlayView.setTranslationX(mMapOverlayView.getTranslationX()-x);
      mMapOverlayView.setTranslationY(mMapOverlayView.getTranslationY()-y);
    }
    previousPoint = point;
  }

  public Projection getProjection() {
    return mProjection;
  }

  public CameraPosition getCameraPosition() {
    return mCameraPosition;
  }

  private long lastTime = 0;
  private void logTime(){
    if (lastTime == 0) {
      lastTime = System.currentTimeMillis();
    } else {
      long currentTime = System.currentTimeMillis();
      Tlog.log("Overlay update interval : " + (currentTime - lastTime));
      lastTime = currentTime;
    }
  }
}
