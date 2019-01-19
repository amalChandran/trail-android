package com.amalbit.trail;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.amalbit.trail.util.Util;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;

public class RouteOverlayView extends View {

  /**
   * Debug
   * **/

  private int debugNumColumns, debugNumRows;

  private int debugCellWidth, debugCellHeight;

  private Paint debugGridPaing = new Paint();

  public enum AnimType {
    PATH,
    ARC
  }

  public static final class MarkerGravity {

    public static final int CENTER = 1;
    public static final int BOTTOM = 2;
  }

  private AnimType currentAnimType;

  private static final int ZOOM_MIN_TO_STOP_ANIM = 10;

  private static final int ZOOM_MAX_TO_STOP_ANIM = 22;

  private static final int ARC_CURVE_RADIUS = 450;

  private static final int DEFAULT_EMPTY = 0;

  private static final int STROKE_WIDTH_DP = 6;

  private static final int STROKE_WIDTH_MIN = 6;

  private int routeShadowColor;

  private ProjectionHelper mProjectionHelper;

  private AnimationRouteHelper mAnimationRouteHelper;

  private AnimationArcHelper mAnimationArcHelper;

  private final Object mSvgLock = new Object();

  private float zoomAnchor;

  protected Paint paintTop;

  protected Paint paintBottom;

  protected Paint paintTopArc;

  protected Paint paintBottomArc;

  protected Paint paintShadow;

  private Path mRoutePath;

  private Path mRoutePathDraw;

  private Path mArcPath;

  private Path mShadowPath;

  private Bitmap pickUpBitmap;

  private Bitmap dropBitmap;

  private int markerGravity;

  private Point pickUpPoint;

  private Point dropPoint;

  private boolean isPathSetup;

  private boolean isArc;

  protected int routeMainColor;

  protected int routeSecondaryColor;

  protected float mScaleFactor;

  private float mStrokeWidth;

  private Matrix mMatrix = new Matrix();

  private RectF rectF = new RectF();

  public RouteOverlayView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs, context);
    setUpDebugProps();
  }

  public RouteOverlayView(Context context) {
    super(context);
    init(null, context);
    setUpDebugProps();
  }

  private void init(@Nullable AttributeSet attrSet, Context context) {
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    mProjectionHelper = new ProjectionHelper();
    mAnimationRouteHelper = AnimationRouteHelper.getInstance(this);
    mAnimationArcHelper = AnimationArcHelper.getInstance(this);
    mRoutePath = new Path();
    mRoutePathDraw =  new Path();
    mArcPath = new Path();
    mShadowPath = new Path();

    if (attrSet != null) {
      TypedArray ta = getContext().obtainStyledAttributes(attrSet, R.styleable.RouteOverlayView);
      int pickUpResourceId = ta.getResourceId(R.styleable.RouteOverlayView_routeStartMarkerImg, DEFAULT_EMPTY);
      if (pickUpResourceId != DEFAULT_EMPTY) {
        pickUpBitmap = BitmapFactory.decodeResource(getResources(), pickUpResourceId);
      }

      int dropResourceId = ta.getResourceId(R.styleable.RouteOverlayView_routeEndMarkerImg, DEFAULT_EMPTY);
      if (dropResourceId != DEFAULT_EMPTY) {
        dropBitmap = BitmapFactory.decodeResource(getResources(), dropResourceId);
      }

      markerGravity = ta.getInteger(R.styleable.RouteOverlayView_markerGravity, MarkerGravity.CENTER);

      routeMainColor = ta.getColor(
          R.styleable.RouteOverlayView_routePrimaryColor,
          getResources().getColor(R.color.routePrimaryColor)
      );

      routeSecondaryColor = ta.getColor(
          R.styleable.RouteOverlayView_routeSecondaryColor,
          getResources().getColor(R.color.routeSecondaryColor)
      );

      routeShadowColor = ta.getColor(
          R.styleable.RouteOverlayView_routeShadowColor,
          getResources().getColor(R.color.routeShadowColor)
      );

      ta.recycle();
    } else {
      routeMainColor = getResources().getColor(R.color.routePrimaryColor);
      routeSecondaryColor = getResources().getColor(R.color.routeSecondaryColor);
      routeShadowColor = getResources().getColor(R.color.routeShadowColor);
    }

    mStrokeWidth = Util.convertDpToPixel(STROKE_WIDTH_DP, getContext());

    paintTop = new Paint();
    paintTop.setStyle(Paint.Style.STROKE);
    paintTop.setStrokeWidth(mStrokeWidth);
    paintTop.setColor(routeMainColor);
    paintTop.setAntiAlias(true);
    paintTop.setStrokeJoin(Paint.Join.ROUND);
    paintTop.setStrokeCap(Cap.ROUND);

    paintTopArc = new Paint();
    paintTopArc.setStyle(Paint.Style.STROKE);
    paintTopArc.setStrokeWidth(mStrokeWidth);
    paintTopArc.setColor(routeMainColor);
    paintTopArc.setAntiAlias(true);
    paintTopArc.setStrokeJoin(Paint.Join.ROUND);
    paintTopArc.setStrokeCap(Paint.Cap.ROUND);

    paintBottom = new Paint();
    paintBottom.setStyle(Paint.Style.STROKE);
    paintBottom.setStrokeWidth(mStrokeWidth);
    paintBottom.setColor(routeSecondaryColor);
    paintBottom.setAntiAlias(true);
    paintBottom.setStrokeJoin(Paint.Join.ROUND);
    paintBottom.setStrokeCap(Cap.ROUND);

    paintBottomArc = new Paint();
    paintBottomArc.setStyle(Paint.Style.STROKE);
    paintBottomArc.setStrokeWidth(mStrokeWidth);
    paintBottomArc.setColor(routeSecondaryColor);
    paintBottomArc.setAntiAlias(true);
    paintBottomArc.setStrokeJoin(Paint.Join.ROUND);
    paintBottomArc.setStrokeCap(Paint.Cap.ROUND);

    paintShadow = new Paint();
    paintShadow.setStyle(Paint.Style.STROKE);
    paintShadow.setStrokeWidth(mStrokeWidth);
    paintShadow.setColor(routeShadowColor);
  }

  private void setUpDebugProps(){
    debugGridPaing.setColor(Color.GRAY);
    debugGridPaing.setStyle(Paint.Style.FILL_AND_STROKE);
  }

  private void calculateDimensionForDebugGrid() {
    debugNumColumns = getWidth() / 20;
    debugNumRows = getHeight() / 20;
    debugCellWidth = getWidth() / debugNumColumns;
    debugCellHeight = getHeight() / debugNumRows;
    invalidate();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    calculateDimensionForDebugGrid();
  }

  public void onCameraMove(GoogleMap map) {
    if (mProjectionHelper.mLineChartCenterLatLng == null) {
      return;
    }
    mProjectionHelper.onCameraMove(map, this);
  }


  protected void scalePathMatrix(float zoom) {
    if (!isPathSetup) return;
    mScaleFactor = (float) Math.pow(2f, (zoom - zoomAnchor));
    zoomPath(mScaleFactor);
    zoomAnchor = zoom;
  }

  private void zoomPath(float scaleFactor) {
    mRoutePathDraw.computeBounds(rectF, true);
    Log.i("Path center", "\n x = " + rectF.centerX() + "\n y = "  + rectF.centerY());
    Matrix matrix = new Matrix();
    matrix.postScale(scaleFactor, scaleFactor, rectF.centerX(), rectF.centerY());
    mMatrix.postConcat(matrix);
    mRoutePathDraw.rewind();
    mRoutePathDraw.addPath(mRoutePath);
    mRoutePathDraw.transform(mMatrix);
    invalidate();
  }

  public void translatePathMatrix(float dx, float dy) {
    mRoutePathDraw.computeBounds(rectF, true);//TODO remove. this was added just for debug purposes.
    Matrix matrix = new Matrix();
    matrix.postTranslate(dx, dy);
    mMatrix.postConcat(matrix);
    mRoutePathDraw.rewind();
    mRoutePathDraw.addPath(mRoutePath);
    mRoutePathDraw.transform(mMatrix);
    invalidate();
  }

  public void drawArc(LatLng fromLocation, LatLng toLocation, GoogleMap map) {
    if (fromLocation == null
        || toLocation == null
        || map == null) {
      throw new IllegalArgumentException("Parameters cannot be null.");
    }
    List<LatLng> latLngs = new ArrayList<>();
    latLngs.add(fromLocation);
    latLngs.add(toLocation);
    setUpPath(latLngs, map, AnimType.ARC);
  }

  public void drawPath(List<LatLng> latLngs, GoogleMap map) {
    if (latLngs == null
        || latLngs.size() < 2
        || map == null) {
      throw new IllegalArgumentException("Parameters cannot be null or latLngs array less than 2.");
    }
    setUpPath(latLngs, map, AnimType.PATH);
  }

  private void setUpPath(List<LatLng> latLngs, GoogleMap map, AnimType animType) {
    currentAnimType = animType;
    Projection projection = map.getProjection();
    this.zoomAnchor = map.getCameraPosition().zoom;

    clearAnimation();
    mRoutePath = new Path();
    mArcPath = new Path();

    pickUpPoint = projection.toScreenLocation(latLngs.get(0));
    dropPoint = projection.toScreenLocation(latLngs.get(latLngs.size() - 1));

    if (animType == AnimType.PATH) {
      mRoutePath.moveTo(pickUpPoint.x, pickUpPoint.y);
      for (int i = 0; i < latLngs.size() - 1; i++) {
        float nextPointX = projection.toScreenLocation(latLngs.get(si(i + 1, latLngs))).x;
        float nextPointY = projection.toScreenLocation(latLngs.get(si(i + 1, latLngs))).y;
        mRoutePath.lineTo(nextPointX, nextPointY);
      }

      PathMeasure pathMeasure = new PathMeasure(mRoutePath, false);
      mAnimationRouteHelper.length = pathMeasure.getLength();
      mAnimationRouteHelper.dashValue =
          new float[]{mAnimationRouteHelper.length, mAnimationRouteHelper.length};

      mAnimationRouteHelper.play();
      isArc = false;
      mRoutePath.computeBounds(rectF, true);
      mProjectionHelper.setCenterLatLng(
          map.getProjection()
              .fromScreenLocation(
                  new Point((
                      (int)rectF.centerX()),
                      (int)(rectF.centerY()))
              ));
    } else { //arc
      mArcPath = Util.createCurvedPath(pickUpPoint.x, pickUpPoint.y, dropPoint.x, dropPoint.y, ARC_CURVE_RADIUS);
      mShadowPath = Util.createShadowPath(pickUpPoint.x, pickUpPoint.y, dropPoint.x, dropPoint.y);

      PathMeasure pathMeasure = new PathMeasure(mArcPath, false);
      mAnimationArcHelper.arcLength = pathMeasure.getLength();
      mAnimationArcHelper.arcdDashValue = new float[]{mAnimationArcHelper.arcLength, mAnimationArcHelper.arcLength};
      PathEffect effect = new DashPathEffect(mAnimationArcHelper.arcdDashValue, -mAnimationArcHelper.arcLength);
      paintTopArc.setPathEffect(effect);

      PathMeasure shadowPathMeasure = new PathMeasure(mShadowPath, false);
      mAnimationArcHelper.shadowLength = shadowPathMeasure.getLength();
      mAnimationArcHelper.shadowDashValue = new float[]{mAnimationArcHelper.shadowLength,
          mAnimationArcHelper.shadowLength};
      PathEffect shadowEffect = new DashPathEffect(mAnimationArcHelper.shadowDashValue,
          -mAnimationArcHelper.shadowLength);
      paintShadow.setPathEffect(shadowEffect);

      mAnimationArcHelper.play();
      isArc = true;
    }
    isPathSetup = true;

    invalidate();
    onCameraMove(map);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    synchronized (mSvgLock) {
      drawPathBorder(canvas);
      drawShadow(canvas);
      drawRoute(canvas);
      drawMarkers(canvas);
    }
  }

  private void drawDebugDrid(Canvas canvas) {
    int width = getWidth();
    int height = getHeight();
    for (int i = 1; i < debugNumColumns; i++) {
      canvas.drawLine(i * debugCellWidth, 0, i * debugCellWidth, height, debugGridPaing);
    }

    for (int i = 1; i < debugNumRows; i++) {
      canvas.drawLine(0, i * debugCellHeight, width, i * debugCellHeight, debugGridPaing);
    }
  }

  private void drawPathBorder(Canvas canvas) {
//    drawDebugDrid(canvas);
    canvas.drawRect(rectF, paintTopArc);
    canvas.drawCircle(rectF.centerX(), rectF.centerY(), 20, paintTopArc);
    if (mProjectionHelper != null && mProjectionHelper.point!= null) {
      canvas.drawCircle(mProjectionHelper.point.x, mProjectionHelper.point.y, 10, paintBottomArc);
    }
  }

  private void drawShadow(Canvas canvas) {
    if (currentAnimType != null
        && currentAnimType == AnimType.ARC) {
      canvas.drawPath(mShadowPath, paintShadow);
    }
  }

  private void drawRoute(Canvas canvas) {
    if (mRoutePathDraw == null) {
      return;
    }
    if (isArc) {
      if (mAnimationArcHelper.animStarted) {
        if (mAnimationArcHelper.isFirstTimeDrawing) {
          canvas.drawPath(mArcPath, paintTopArc);
        } else {
          canvas.drawPath(mArcPath, paintBottomArc);
          canvas.drawPath(mArcPath, paintTopArc);
        }
      }
    } else {
      if (mAnimationRouteHelper.isFirstTimeDrawing) {
        canvas.drawPath(mRoutePathDraw, paintTop);
      } else {
        canvas.drawPath(mRoutePathDraw, paintBottom);
        canvas.drawPath(mRoutePathDraw, paintTop);
      }
    }
  }

  private void drawMarkers(Canvas canvas) {
    if (pickUpPoint != null
        && dropPoint != null
        && pickUpBitmap != null
        && dropBitmap != null) {
      drawMarker(canvas, pickUpBitmap, new Point(pickUpPoint.x, pickUpPoint.y), markerGravity);
      drawMarker(canvas, dropBitmap, new Point(dropPoint.x, dropPoint.y), markerGravity);
    }
  }

  private void drawMarker(Canvas canvas, Bitmap bitmap, Point point, @Nullable int gravity) {
    if (gravity == MarkerGravity.CENTER) {
      point.x = point.x - bitmap.getWidth() / 2;
      point.y = point.y - bitmap.getHeight() / 2;
    } else { // bottom, for now
      point.x = point.x - bitmap.getWidth() / 2;
      point.y = point.y - bitmap.getHeight();
    }
    canvas.drawBitmap(bitmap, point.x, point.y, null);
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

  public void stopAllAnimation() {
    mAnimationRouteHelper.stop(() -> {
    });
    mAnimationArcHelper.stop(() -> {
    });
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    stopAllAnimation();
  }
}
