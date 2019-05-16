package com.amalbit.animationongooglemap.projectionBased;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.LinearInterpolator;
import com.amalbit.animationongooglemap.R;
import com.amalbit.animationongooglemap.U;
import com.amalbit.animationongooglemap.marker.LatLngInterpolator;
import com.amalbit.trail.marker.OverlayMarkerOptim;
import com.amalbit.trail.marker.OverlayMarkerOptim.OnMarkerUpdate;
import com.amalbit.trail.marker.ViewOverlayView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.List;

public class ViewOverlayActivity extends BaseActivity implements OnMapReadyCallback, OnClickListener, OnMarkerUpdate {

  /**
   * DONE : Draw one marker on the screen. DONE : Add second marker, move it based on first markers movement. TODO :
   * Animate second marker just based on first marker position. TODO : Move it based on translate matrix if this
   * approach works.
   **/

  private GoogleMap mMap;

  private ViewOverlayView viewOverlayView;

  private Bitmap dot;
  private Bitmap yellowDot;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_view_overlay);

    dot = BitmapFactory.decodeResource(getResources(), R.drawable.ic_dot);
    yellowDot = BitmapFactory.decodeResource(getResources(), R.drawable.ic_dot_yellow);

    viewOverlayView = findViewById(R.id.viewOverly);
    findViewById(R.id.btnPlus).setOnClickListener(this);
    findViewById(R.id.btnMinus).setOnClickListener(this);
    findViewById(R.id.btnPrint).setOnClickListener(this);
    findViewById(R.id.btnOne).setOnClickListener(this);
    findViewById(R.id.btnMoveLeft).setOnClickListener(this);
    findViewById(R.id.btnMoveRight).setOnClickListener(this);
    findViewById(R.id.btnMoveTop).setOnClickListener(this);
    findViewById(R.id.btnMoveDown).setOnClickListener(this);

    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);
  }

  @Override
  public void onMapReady(GoogleMap map) {
    mMap = map;

    mMap.setOnMapLoadedCallback(() -> {
      mMap.setOnCameraMoveListener(() -> {
        viewOverlayView.onCameraMove(mMap);
//        print();
      });
      setMapBoundsRow(map);
      viewOverlayView.setCenterLatlng(map);
      addCenterMarker();
      addSecondMarker();

      addNormalMarker();

      mMap.setOnMapClickListener(this::moveToLatLngWithoutProjection);
    });
  }


  private LatLng pointOne = new LatLng(12.9715002, 77.6344856);
  private LatLng pointTwo = new LatLng(12.9715002, 77.6354856);
  private LatLng pointThree = new LatLng(12.9715002, 77.6364856);
  private LatLng pointFour = new LatLng(12.9715002, 77.6374856);

  private LatLng pointFive = new LatLng(12.9725002, 77.6344856);
  private LatLng pointSix = new LatLng(12.9725002, 77.6354856);
  private LatLng pointSeven = new LatLng(12.9725002, 77.6364856);
  private LatLng pointEight = new LatLng(12.9725002, 77.6374856);

  private void setMapBoundsRow(GoogleMap googleMap) {
    List<LatLng> latLngs = new ArrayList<>();
    latLngs.add(pointOne);
    latLngs.add(pointTwo);
    latLngs.add(pointThree);
    latLngs.add(pointFour);
    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
    for (LatLng latLngPoint : latLngs) {
      boundsBuilder.include(latLngPoint);
    }
    LatLngBounds latLngBounds = boundsBuilder.build();
//    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pointTwo, zoomLevel));
  }

  private void addNormalMarker() {
    mMap.addMarker(new MarkerOptions().position(pointOne));
    mMap.addMarker(new MarkerOptions().position(pointTwo));
    mMap.addMarker(new MarkerOptions().position(pointThree));
    mMap.addMarker(new MarkerOptions().position(pointFour));
    mMap.addMarker(new MarkerOptions().position(pointFive));
    mMap.addMarker(new MarkerOptions().position(pointSix));
    mMap.addMarker(new MarkerOptions().position(pointSeven));
    mMap.addMarker(new MarkerOptions().position(pointEight));
  }


  private void findScreenPointForFirstLatLng() {
    //Find distance between current center and first point.
    //Convert the latlng to pixel.

//    U.log("mapping", "Zoom : " + mMap.getCameraPosition().zoom);
//    U.log("mapping", "pointOne : " + mMap.getProjection().toScreenLocation(pointOne));
//    U.log("mapping", "pointTwo : " + mMap.getProjection().toScreenLocation(pointTwo));
//    U.log("mapping", "pointThree : " + mMap.getProjection().toScreenLocation(pointThree));
//    U.log("mapping", "pointFour : " + mMap.getProjection().toScreenLocation(pointFour));

    Point firstPoint = mMap.getProjection().toScreenLocation(pointOne);
    Point secondPoint = mMap.getProjection().toScreenLocation(pointTwo);
    Point thirdPoint = mMap.getProjection().toScreenLocation(pointThree);
    Point fourthPoint = mMap.getProjection().toScreenLocation(pointFour);

    Location firstLocation = new Location("pointOne");
    firstLocation.setLatitude(pointOne.latitude);
    firstLocation.setLongitude(pointOne.longitude);

    Location secondLocation = new Location("pointOne");
    secondLocation.setLatitude(pointTwo.latitude);
    secondLocation.setLongitude(pointTwo.longitude);

    Location thirdLocation = new Location("pointOne");
    thirdLocation.setLatitude(pointThree.latitude);
    thirdLocation.setLongitude(pointThree.longitude);

    Location fourthLocation = new Location("pointOne");
    fourthLocation.setLatitude(pointFour.latitude);
    fourthLocation.setLongitude(pointFour.longitude);

    U.log("distance", "-------------------------------------------------------------------------");
    U.log("distance", "Zoom level " + zoomLevel);
    U.log("distance", "-------------------------------------------------------------------------");
    U.log("distance", "1 to 2 latLng    -> " + Math.abs(pointOne.longitude - pointTwo.longitude));
    U.log("distance", "1 to 2 meters    -> " + firstLocation.distanceTo(secondLocation));
    U.log("distance", "1 to 2 pixels    -> " + Math.abs(firstPoint.x - secondPoint.x));
    U.log("distance", "-------------------------------------------------------------------------");
    U.log("distance", "2 to 3 latLng    -> " + Math.abs(pointTwo.longitude - pointThree.longitude));
    U.log("distance", "2 to 3 meters    -> " + secondLocation.distanceTo(thirdLocation));
    U.log("distance", "2 to 3 pixels    -> " + Math.abs(secondPoint.x - thirdPoint.x));
    U.log("distance", "-------------------------------------------------------------------------");
    U.log("distance", "3 to 4 latLng    -> " + Math.abs(pointThree.longitude - pointFour.longitude));
    U.log("distance", "3 to 4 meters    -> " + thirdLocation.distanceTo(fourthLocation));
    U.log("distance", "3 to 4 pixels    -> " + Math.abs(thirdPoint.x - fourthPoint.x));

    Point centerPoint = viewOverlayView.getCenterPoint();
    LatLng centerLatLng = mMap.getCameraPosition().target;
    double difference = centerLatLng.longitude - pointOne.longitude;

    viewOverlayView.getSecondMarker().setScreenPoint(new Point(centerPoint.x + 78, centerPoint.y));
    viewOverlayView.invalidate();

    OverlayMarkerOptim overlayMarker1 = new OverlayMarkerOptim();
//    overlayMarker1.setIcon(dot.copy(dot.getConfig(), true));
//    overlayMarker1.setMarkerId(2324);
//    overlayMarker1.setLatLng(pointOne);
//    overlayMarker1.setOnMarkerUpdate(ViewOverlayActivity.this);
  }


  private void addCenterMarker() {
    LatLng centerLatLng = mMap.getCameraPosition().target;

    OverlayMarkerOptim overlayMarker1 = new OverlayMarkerOptim();
    overlayMarker1.setIcon(yellowDot.copy(yellowDot.getConfig(), true));
    overlayMarker1.setMarkerId(2323);
    overlayMarker1.setLatLng(centerLatLng);
    overlayMarker1.setOnMarkerUpdate(ViewOverlayActivity.this);

    viewOverlayView.addCenterMarker(overlayMarker1, mMap.getProjection());
  }

  private void addSecondMarker() {
    LatLng centerLatLng = mMap.getCameraPosition().target;

    OverlayMarkerOptim overlayMarker1 = new OverlayMarkerOptim();
    overlayMarker1.setIcon(dot.copy(dot.getConfig(), true));
    overlayMarker1.setMarkerId(2324);
    overlayMarker1.setLatLng(centerLatLng);
    overlayMarker1.setOnMarkerUpdate(ViewOverlayActivity.this);

    viewOverlayView.addSecondMarker(overlayMarker1, mMap.getProjection());
  }

  @Override
  public void onMarkerUpdate() {
    viewOverlayView.invalidate();
  }


  private float anchorzoomLevel = 16;
  private float zoomLevel = 16;
  private static final float PIXEL_DISTANCE_ZOOMLEVEL = 163;

  private void print() {
    U.log("point", "-------------------------------------------------------------------------");
//    double average1pixDistanceX = 0;
    double average1pixDistanceY = 0;
    for (int i = 0; i < 10; i++) {
      LatLng centerLatlng = mMap.getCameraPosition().target;
      Point centerPoint = mMap.getProjection().toScreenLocation(centerLatlng);
      U.log("point", "centerPoint: " + centerPoint);
//      centerPoint.x = centerPoint.x + 1;
      centerPoint.y = centerPoint.y + 1;
//      centerPoint.y = centerPoint.y + 1;
      U.log("point", "next centerPoint: " + centerPoint);
      LatLng nextCenterLatLng = mMap.getProjection().fromScreenLocation(centerPoint);
      mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nextCenterLatLng, zoomLevel));

//      average1pixDistanceX += nextCenterLatLng.longitude - centerLatlng.longitude;
      average1pixDistanceY += nextCenterLatLng.latitude - centerLatlng.latitude;
    }
//    U.log("point", "average1pixDistanceX" + (average1pixDistanceX / 100));
    U.log("point", "average1pixDistanceY" + (average1pixDistanceY / 100));
    U.log("point", "zoomlevel" + zoomLevel);

    U.log("point", "-------------------------------------------------------------------------");
  }

  private int getPixelPerZoomLevel() {
    return (int) (PIXEL_DISTANCE_ZOOMLEVEL * Math.pow(2, zoomLevel - anchorzoomLevel));
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btnPlus:
        zoomLevel += 1;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pointTwo, zoomLevel));
        break;
      case R.id.btnMinus:
        moveToLatLngWithoutProjection(pointEight);
//        moveToLatWithoutProjection(pointEight);
//        zoomLevel -= 1;
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pointTwo, zoomLevel));
        break;
      case R.id.btnPrint:
        print();
        break;
      case R.id.btnOne:
        findScreenPointForFirstLatLng();
        break;
      case R.id.btnMoveLeft:
        moveSecondMarkerLeft();
        break;
      case R.id.btnMoveRight:
        moveSecondMarkerRight();
        break;
      case R.id.btnMoveTop:
        moveSecondMarkerTop();
        break;
      case R.id.btnMoveDown:
        moveSecondMarkerDown();
        break;
    }
  }

//  Zoom level	average lng/lng per pixel
//    17	0.000003352761269
//    16	0.00000637024641
//    15	0.00001252926886
//    14	0.00002435110509
//    13	0.00004895031452
//    12	0.00009790062904
//    11	0.0001961365342
//    10	0.0003922730684
//    9	  0.0007845461369
//    8	  0.0015694275498390198
//    7	  0.0031388550996780396
//    6	  0.006277710199356079
//    5	  0.012555420398712158
//    4	  0.025110840797424174
//    3	  0.05022168159484863
//    2	  0.05022168159484863
//    1	  0.05022168159484835


//  private void moveToLngWithoutProjection(LatLng latLng) {
//    //(Difference between longitude / 0.00001252926886 )
//    int dx = (int) (Math.abs(viewOverlayView.getSecondMarker().getLatLng().longitude - latLng.longitude) / 0.00001252926886);
//    Point predictedPointOnScreen = new Point(viewOverlayView.getSecondMarker().getScreenPoint().x + dx, viewOverlayView.getSecondMarker().getScreenPoint().y);
//    viewOverlayView.getSecondMarker().setScreenPoint(predictedPointOnScreen);
//    viewOverlayView.invalidate();
//  }
//
//  private void moveToLatWithoutProjection(LatLng latLng) {
//    //(Difference between latitude / 0.00001252926886 )
//    int dy = (int) ((viewOverlayView.getSecondMarker().getLatLng().latitude - latLng.latitude) / 0.00001252926886);
//    Point predictedPointOnScreen = new Point(viewOverlayView.getSecondMarker().getScreenPoint().x, viewOverlayView.getSecondMarker().getScreenPoint().y + dy);
//    viewOverlayView.getSecondMarker().setScreenPoint(predictedPointOnScreen);
//    viewOverlayView.invalidate();
//  }

  private static final double ZOOM_16 = 0.00000637024641;
  private void moveToLatLngWithoutProjection(final LatLng latLng) {

    ValueAnimator valueAnimator = new ValueAnimator();
    valueAnimator.setInterpolator(new LinearInterpolator());
    valueAnimator.addUpdateListener(animation -> {
      float v = animation.getAnimatedFraction();
      LatLng newPosition = new LatLngInterpolator.Linear().interpolate(v, viewOverlayView.getSecondMarker().getLatLng(), latLng);

      //(Difference between latitude / 0.00001252926886 )
      int dx = (int) ((viewOverlayView.getSecondMarker().getLatLng().longitude - newPosition.longitude) / ZOOM_16);
//    int x =

      int dy = (int) ((viewOverlayView.getSecondMarker().getLatLng().latitude - newPosition.latitude) / ZOOM_16);
      Point predictedPointOnScreen = new Point(viewOverlayView.getSecondMarker().getScreenPoint().x - dx, viewOverlayView.getSecondMarker().getScreenPoint().y + dy);
      viewOverlayView.getSecondMarker().setScreenPoint(predictedPointOnScreen);
      viewOverlayView.getSecondMarker().setLatLng(newPosition);
      viewOverlayView.invalidate();
    });
    valueAnimator.setFloatValues(0, 1);
    valueAnimator.reverse();
    valueAnimator.setRepeatCount(10);
    valueAnimator.setDuration(1000);
    valueAnimator.start();

  }

  private void moveSecondMarkerTop() {
    Point centerPoint = new Point(viewOverlayView.getCenterMarker().x, viewOverlayView.getCenterMarker().y);
    centerPoint.x = viewOverlayView.getSecondMarker().getScreenPoint().x;
    centerPoint.y = viewOverlayView.getSecondMarker().getScreenPoint().y - getPixelPerZoomLevel();
    viewOverlayView.getSecondMarker().setScreenPoint(centerPoint);
    viewOverlayView.invalidate();
  }

  private void moveSecondMarkerDown() {
    Point centerPoint = new Point(viewOverlayView.getCenterMarker().x, viewOverlayView.getCenterMarker().y);
    centerPoint.x = viewOverlayView.getSecondMarker().getScreenPoint().x;
    centerPoint.y = viewOverlayView.getSecondMarker().getScreenPoint().y + getPixelPerZoomLevel();
    viewOverlayView.getSecondMarker().setScreenPoint(centerPoint);
    viewOverlayView.invalidate();
  }

  private void moveSecondMarkerLeft() {
    Point centerPoint = new Point(viewOverlayView.getCenterMarker().x, viewOverlayView.getCenterMarker().y);
    centerPoint.y = viewOverlayView.getSecondMarker().getScreenPoint().y;
    centerPoint.x = viewOverlayView.getSecondMarker().getScreenPoint().x - getPixelPerZoomLevel();
    viewOverlayView.getSecondMarker().setScreenPoint(centerPoint);
    viewOverlayView.invalidate();

//    ValueAnimator valueAnimator = new ValueAnimator();
//    valueAnimator.setInterpolator(new LinearInterpolator());
//    valueAnimator.addUpdateListener(animation -> {
//      Point centerPoint = new Point(viewOverlayView.getCenterMarker().x, viewOverlayView.getCenterMarker().y);
//      float v = (float)animation.getAnimatedValue();
//      centerPoint.x = (int)(v);
//      viewOverlayView.getSecondMarker().setScreenPoint(centerPoint);
//      viewOverlayView.invalidate();
//      U.log("moveSecondMarker", ""+centerPoint);
//    });
//    valueAnimator.setFloatValues(viewOverlayView.getSecondMarker().getScreenPoint().x, viewOverlayView.getSecondMarker().getScreenPoint().x - 163); // Ignored.
//    valueAnimator.setDuration(1000);
//    valueAnimator.start();
  }

  private void moveSecondMarkerRight() {
    Point centerPoint = new Point(viewOverlayView.getCenterMarker().x, viewOverlayView.getCenterMarker().y);
    centerPoint.y = viewOverlayView.getSecondMarker().getScreenPoint().y;
    centerPoint.x = viewOverlayView.getSecondMarker().getScreenPoint().x + getPixelPerZoomLevel();
    viewOverlayView.getSecondMarker().setScreenPoint(centerPoint);
    viewOverlayView.invalidate();
//    Point centerPoint = new Point(viewOverlayView.getCenterMarker().x, viewOverlayView.getCenterMarker().y);
//
//    ValueAnimator valueAnimator = new ValueAnimator();
//    valueAnimator.setInterpolator(new LinearInterpolator());
//    valueAnimator.addUpdateListener(animation -> {
//      float v = (float)animation.getAnimatedValue();
//      centerPoint.x = (int)v;
//      viewOverlayView.getSecondMarker().setScreenPoint(centerPoint);
//      viewOverlayView.invalidate();
//      U.log("moveSecondMarker", ""+centerPoint);
//    });
//    valueAnimator.setFloatValues(viewOverlayView.getSecondMarker().getScreenPoint().x, viewOverlayView.getSecondMarker().getScreenPoint().x + 163); // Ignored.
//    valueAnimator.setDuration(1000);
//    valueAnimator.start();
  }
}
