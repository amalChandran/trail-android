package com.amalbit.trail;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import java.util.List;

public class RouteOverlayView extends View {

  private static final int ZOOM_MIN_TO_STOP_ANIM = 10;

  private ProjectionHelper mProjectionHelper;

  private float length;

  private boolean isFirstTimeDrawing;

  private final Object mSvgLock = new Object();

  int routeMainColor = Color.parseColor("#8863fb");

  int routeShadwoColor = Color.parseColor("#4e4878");

  float[] dashValue;

  private float zoomAnchor;

  private Paint paintTop = new Paint();

  private Paint paintBottom = new Paint();

  private Path mRoutePath = new Path();

  private Bitmap pickUpBitmap;

  private Bitmap dropBitmap;

  private Point pickUpPoint;

  private Point dropPoint;

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

    paintTop.setStyle(Paint.Style.STROKE);
    paintTop.setStrokeWidth(8);
    paintTop.setColor(routeMainColor);
    paintTop.setAntiAlias(true);
    paintTop.setStrokeJoin(Paint.Join.ROUND);
    paintTop.setStrokeCap(Paint.Cap.ROUND);

    paintBottom.setStyle(Paint.Style.STROKE);
    paintBottom.setStrokeWidth(8);
    paintBottom.setColor(routeShadwoColor);
    paintBottom.setAntiAlias(true);
    paintBottom.setStrokeJoin(Paint.Join.ROUND);
    paintBottom.setStrokeCap(Paint.Cap.ROUND);

    pickUpBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_pickup);
    pickUpBitmap = Bitmap.createScaledBitmap(pickUpBitmap, 52, 52, false);

    dropBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_drop);
    dropBitmap = Bitmap.createScaledBitmap(dropBitmap, 52, 52, false);

  }

  public void onCameraMove(GoogleMap map) {
    if(mProjectionHelper.mLineChartCenterLatLng == null) return;

    mProjectionHelper.onCameramove(map, this);
  }

  float zoomValue = 1f;
  float routeWidth = 2f;
  public void zoom(float zoom){
    if(!isPathSetup) return;
    if(zoom <= ZOOM_MIN_TO_STOP_ANIM) {
      stopAnimating();
      setVisibility(GONE);
    } else {
      setVisibility(VISIBLE);
      startAnimating();
    }

    zoomValue = (float) Math.pow(2f, (zoom - zoomAnchor));
    routeWidth = (float) Math.pow(2f, (18 - zoom));

    AdditiveAnimator.animate(this).scaleX(zoomValue).scaleY(zoomValue)
        .setDuration(2).start();

    paintTop.setStrokeWidth(routeWidth);
    paintBottom.setStrokeWidth(routeWidth);
    invalidate();
  }

  boolean isPathSetup;
  boolean isAnimating;
  public void setUpPath(List<LatLng> latLngs, GoogleMap map) {
    Projection projection = map.getProjection();
    this.zoomAnchor = map.getCameraPosition().zoom;

    mProjectionHelper.setCenterlatLng(projection
        .fromScreenLocation(new Point(getWidth()/2, getHeight()/2)));
    onCameraMove(map);

    if (latLngs == null || latLngs.size() == 0) return;
    mRoutePath.rewind();

    pickUpPoint = projection.toScreenLocation(latLngs.get(0));
    dropPoint = projection.toScreenLocation(latLngs.get(latLngs.size() - 1));

    mRoutePath.moveTo(pickUpPoint.x, pickUpPoint.y);
    for (int i = 0; i < latLngs.size() - 1; i++) {
      float nextPointX = projection.toScreenLocation(latLngs.get(si(i + 1, latLngs))).x;
      float nextPointY = projection.toScreenLocation(latLngs.get(si(i + 1, latLngs))).y;
      mRoutePath.lineTo(nextPointX, nextPointY);
    }

    PathMeasure pathMeasure = new PathMeasure(mRoutePath, false);
    length = pathMeasure.getLength();
    dashValue = new float[] { length, length };

    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        RouteOverlayView.this.initAnimObjects();
        RouteOverlayView.this.startAnimating();
      }
    });

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
    if(isFirstTimeDrawing) {
      canvas.drawPath(mRoutePath, paintTop);
    } else {
      canvas.drawPath(mRoutePath, paintBottom);
      canvas.drawPath(mRoutePath, paintTop);
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

  private AnimatorSet animatorSet;
  private AnimatorSet repeatAnimatorSet;
  private ObjectAnimator pointAnimator;
  private ObjectAnimator pointAnimator1;
  private ValueAnimator colorAnimation;

  private void initAnimObjects() {
    pointAnimator = ObjectAnimator.ofFloat(this, "update", 1f, 0f);
    pointAnimator.setDuration(2000);
    pointAnimator.setInterpolator(new DecelerateInterpolator());
    pointAnimator.addListener(new Animator.AnimatorListener() {
      @Override public void onAnimationStart(Animator animator) {
        isFirstTimeDrawing = true;
      }

      @Override public void onAnimationEnd(Animator animator) {
        isFirstTimeDrawing = false;
      }

      @Override public void onAnimationCancel(Animator animator) {

      }

      @Override public void onAnimationRepeat(Animator animator) {

      }
    });

    pointAnimator1 = ObjectAnimator.ofFloat(this, "update1", 0f, 1f);
    pointAnimator1.setDuration(2000);
    pointAnimator1.setInterpolator(new DecelerateInterpolator());

    colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), routeShadwoColor, routeMainColor);
    colorAnimation.setDuration(1500); // milliseconds
    colorAnimation.setStartDelay(1000);
    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animator) {
        paintBottom.setColor((int)animator.getAnimatedValue());
        invalidate();
      }
    });
    colorAnimation.addListener(new Animator.AnimatorListener() {
      @Override public void onAnimationStart(Animator animator) {

      }

      @Override public void onAnimationEnd(Animator animator) {
        PathEffect effect = new DashPathEffect(new float[] { length, length }, length);
        paintTop.setPathEffect(effect);
        paintBottom.setColor(routeShadwoColor);
        invalidate();
      }

      @Override public void onAnimationCancel(Animator animator) {

      }

      @Override public void onAnimationRepeat(Animator animator) {

      }
    });

    repeatAnimatorSet = new AnimatorSet();
    animatorSet = new AnimatorSet();

    repeatAnimatorSet.playTogether(pointAnimator1, colorAnimation);
    repeatAnimatorSet.addListener(new AnimatorListenerAdapter() {

      private boolean mCanceled;

      @Override
      public void onAnimationStart(Animator animation) {
        mCanceled = false;
      }

      @Override
      public void onAnimationCancel(Animator animation) {
        mCanceled = true;
      }

      @Override
      public void onAnimationEnd(Animator animation) {
        if (!mCanceled) {
          animation.start();
        } else {

        }
      }

    });



    animatorSet.playSequentially(pointAnimator, repeatAnimatorSet);
    animatorSet.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationCancel(Animator animation) {
        super.onAnimationCancel(animation);
        repeatAnimatorSet.cancel();
      }
    });
  }

  private void startAnimating() {

    if(isAnimating) return;

    animatorSet.start();

    isAnimating = true;
  }

  private void stopAnimating() {
    clearAnimation();
    if(animatorSet != null) {
      //repeatAnimatorSet.end();
      repeatAnimatorSet.cancel();
      //animatorSet.removeAllListeners();
      //animatorSet.end();
      animatorSet.cancel();
    }
    isAnimating = false;
  }


  public void setUpdate(float update) {
    PathEffect effect = new DashPathEffect(dashValue, length * update);
    paintTop.setPathEffect(effect);
    invalidate();
  }

  public void setUpdate1(float update) {
    PathEffect effect = new DashPathEffect(dashValue, -length * update);
    paintTop.setPathEffect(effect);
    invalidate();
  }

  //TODO Make a class with animating values.
}
