package com.amalbit.trail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import com.amalbit.trail.contract.AnimationCallback;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import java.util.List;

public class RouteOverlayView extends View {

  public enum AnimType {
    PATH,
    ARC
  }

  private static final int ZOOM_MIN_TO_STOP_ANIM = 10;

  int routeMainColor = Color.parseColor("#8863fb");

  int routeShadwoColor = Color.parseColor("#4e4878");

  private ProjectionHelper mProjectionHelper;

  private AnimationRouteHelper mAnimationRouteHelper;

  private AnimationArcHelper mAnimationArcHelper;

  private final Object mSvgLock = new Object();

  private float zoomAnchor;

  protected Paint paintTop = new Paint();

  protected Paint paintBottom = new Paint();

  protected Paint paintTopArc = new Paint();

  protected Paint paintBottomArc = new Paint();

  private Path mRoutePath = new Path();

  private Path mArcLoadingPath = new Path();

  private Bitmap pickUpBitmap;

  private Bitmap dropBitmap;

  private Point pickUpPoint;

  private Point dropPoint;

  private boolean isPathSetup;

  private boolean isArc;

  public RouteOverlayView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public RouteOverlayView(Context context) {
    super(context);
    init();
  }

  private void init() {
    mProjectionHelper = new ProjectionHelper();

    mAnimationRouteHelper = AnimationRouteHelper.getInstance(this);
    mAnimationArcHelper = AnimationArcHelper.getInstance(this);

    paintTop.setStyle(Paint.Style.STROKE);
    paintTop.setStrokeWidth(8);
    paintTop.setColor(routeMainColor);
    paintTop.setAntiAlias(true);
    paintTop.setStrokeJoin(Paint.Join.ROUND);
    paintTop.setStrokeCap(Paint.Cap.ROUND);

    paintTopArc.setStyle(Paint.Style.STROKE);
    paintTopArc.setStrokeWidth(8);
    paintTopArc.setColor(routeMainColor);
    paintTopArc.setAntiAlias(true);
    paintTopArc.setStrokeJoin(Paint.Join.ROUND);
    paintTopArc.setStrokeCap(Paint.Cap.ROUND);

    paintBottom.setStyle(Paint.Style.STROKE);
    paintBottom.setStrokeWidth(8);
    paintBottom.setColor(routeShadwoColor);
    paintBottom.setAntiAlias(true);
    paintBottom.setStrokeJoin(Paint.Join.ROUND);
    paintBottom.setStrokeCap(Paint.Cap.ROUND);

    paintBottomArc.setStyle(Paint.Style.STROKE);
    paintBottomArc.setStrokeWidth(8);
    paintBottomArc.setColor(routeShadwoColor);
    paintBottomArc.setAntiAlias(true);
    paintBottomArc.setStrokeJoin(Paint.Join.ROUND);
    paintBottomArc.setStrokeCap(Paint.Cap.ROUND);

    pickUpBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_pickup);
    pickUpBitmap = Bitmap.createScaledBitmap(pickUpBitmap, 52, 52, false);

    dropBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_drop);
    dropBitmap = Bitmap.createScaledBitmap(dropBitmap, 52, 52, false);

  }

  public void onCameraMove(GoogleMap map) {
    if(mProjectionHelper.mLineChartCenterLatLng == null) return;
    mProjectionHelper.onCameramove(map, this);
  }

  public void zoom(float zoom){
    if(!isPathSetup) return;
    if(zoom <= ZOOM_MIN_TO_STOP_ANIM) {
      stopAnimating();
      setVisibility(GONE);
    } else {
      setVisibility(VISIBLE);
      //startAnimating();
    }

    float zoomValue = (float) Math.pow(2f, (zoom - zoomAnchor));
    float routeWidth = (float) Math.pow(2f, (18 - zoom));

    AdditiveAnimator.animate(this).scaleX(zoomValue).scaleY(zoomValue)
        .setDuration(2).start();

    paintTop.setStrokeWidth(routeWidth);
    paintBottom.setStrokeWidth(routeWidth);

    paintTopArc.setStrokeWidth(routeWidth);
    paintBottomArc.setStrokeWidth(routeWidth);
    invalidate();
  }

  public void loadPath(LatLng fromLatlng, LatLng toLatlng, GoogleMap map) {
    isArc = true;
    Projection projection = mProjectionHelper.getProjection();
    this.zoomAnchor = mProjectionHelper.getCameraPosition().zoom;
    mProjectionHelper.setCenterlatLng(projection
        .fromScreenLocation(new Point(getWidth()/2, getHeight()/2)));
    onCameraMove(map);

    if (fromLatlng == null || toLatlng == null) return;

    mRoutePath.rewind();
    mArcLoadingPath.rewind();

    pickUpPoint = projection.toScreenLocation(fromLatlng);
    dropPoint = projection.toScreenLocation(toLatlng);

    mArcLoadingPath = Util.createCurvedPath(pickUpPoint.x, pickUpPoint.y, dropPoint.x, dropPoint.y, 180);

    PathMeasure pathMeasure = new PathMeasure(mArcLoadingPath, false);
    mAnimationArcHelper.length = pathMeasure.getLength();
    mAnimationArcHelper.dashValue = new float[] { mAnimationArcHelper.length, mAnimationArcHelper.length };

    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        mAnimationArcHelper.init();
        mAnimationArcHelper.play();
      }
    });

    isPathSetup = true;
  }

  public void setUpPath(List<LatLng> latLngs, GoogleMap map, AnimType animType) {
    //stopAnimating();
    Projection projection = map.getProjection();
    this.zoomAnchor = map.getCameraPosition().zoom;

    mProjectionHelper.setCenterlatLng(projection
        .fromScreenLocation(new Point(getWidth()/2, getHeight()/2)));
    onCameraMove(map);

    if (latLngs == null || latLngs.size() == 0) return;
    mRoutePath.rewind();
    mArcLoadingPath.rewind();

    pickUpPoint = projection.toScreenLocation(latLngs.get(0));
    dropPoint = projection.toScreenLocation(latLngs.get(latLngs.size() - 1));

    if( animType == AnimType.PATH ) {
      mRoutePath.moveTo(pickUpPoint.x, pickUpPoint.y);
      for (int i = 0; i < latLngs.size() - 1; i++) {
        float nextPointX = projection.toScreenLocation(latLngs.get(si(i + 1, latLngs))).x;
        float nextPointY = projection.toScreenLocation(latLngs.get(si(i + 1, latLngs))).y;
        mRoutePath.lineTo(nextPointX, nextPointY);
      }

      PathMeasure pathMeasure = new PathMeasure(mRoutePath, false);
      mAnimationRouteHelper.length = pathMeasure.getLength();
      mAnimationRouteHelper.dashValue =
          new float[] { mAnimationRouteHelper.length, mAnimationRouteHelper.length };

      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override public void run() {
          mAnimationRouteHelper.init();
          RouteOverlayView.this.startAnimating();
        }
      });
      isArc = false;
    } else { //arc
      mArcLoadingPath = Util.createCurvedPath(pickUpPoint.x, pickUpPoint.y, dropPoint.x, dropPoint.y, 240);

      PathMeasure pathMeasure = new PathMeasure(mArcLoadingPath, false);
      mAnimationArcHelper.length = pathMeasure.getLength();
      mAnimationArcHelper.dashValue = new float[] { mAnimationArcHelper.length, mAnimationArcHelper.length };

      PathEffect effect = new DashPathEffect(mAnimationArcHelper.dashValue, -mAnimationArcHelper.length);
      paintTopArc.setPathEffect(effect);
      invalidate();

      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          mAnimationArcHelper.init();
          mAnimationArcHelper.play();
        }
      });
      isArc = true;
    }

    isPathSetup = true;
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    synchronized (mSvgLock) {
      canvas.save();
      drawRoute(canvas);
      drawMarkers(canvas);
      canvas.restore();
    }
  }

  private void drawRoute(Canvas canvas) {
    if(mRoutePath == null) return;
    if(isArc) {
      if(mAnimationArcHelper.isFirstTimeDrawing) {
        canvas.drawPath(mArcLoadingPath, paintTopArc);
      } else {
        canvas.drawPath(mArcLoadingPath, paintBottomArc);
        canvas.drawPath(mArcLoadingPath, paintTopArc);
      }
    } else {
      if(mAnimationRouteHelper.isFirstTimeDrawing) {
        canvas.drawPath(mRoutePath, paintTop);
      } else {
        canvas.drawPath(mRoutePath, paintBottom);
        canvas.drawPath(mRoutePath, paintTop);
      }
    }
  }

  private void drawMarkers(Canvas canvas) {
    if( pickUpPoint != null && dropPoint != null) {
      canvas.drawBitmap(pickUpBitmap, pickUpPoint.x - pickUpBitmap.getWidth()/2, pickUpPoint.y - pickUpBitmap.getHeight()/2, null);
      canvas.drawBitmap(dropBitmap, dropPoint.x - dropBitmap.getWidth()/2, dropPoint.y - dropBitmap.getHeight()/2, null);

    }
  }

  /**
   * Given an index in datapointsFromInterpollator, it will make sure the the returned index is
   * within the array
   */
  private int si(int i, List<LatLng> list) {
    if (i > list.size() - 1) {
      return list.size() - 1;
    } else if (i < 0) {
      return 0;
    }
    return i;
  }

  //private void initAnimObjects() {
  //  mAnimationRouteHelper.init();
  //  mAnimationArcHelper.init();
  //}

  public void startAnimating() {
    mAnimationRouteHelper.play();
  }

  public void stopAnimating() {
    mAnimationRouteHelper.stop(new AnimationCallback() {
      @Override public void onFinish() {

      }
    });

    mAnimationArcHelper.stop(new AnimationCallback() {
      @Override public void onFinish() {

      }
    });
  }


}
