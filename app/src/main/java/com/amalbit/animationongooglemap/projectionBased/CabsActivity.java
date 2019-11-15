package com.amalbit.animationongooglemap.projectionBased;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import com.amalbit.animationongooglemap.R;
import com.amalbit.animationongooglemap.U;
import com.amalbit.animationongooglemap.data.CarData.Car;
import com.amalbit.animationongooglemap.data.LatlngData;
import com.amalbit.animationongooglemap.marker.LatLngInterpolator;
import com.amalbit.animationongooglemap.marker.Repeat;
import com.amalbit.trail.marker.MarkerOverlayView;
import com.amalbit.trail.marker.OverlayMarker;
import com.amalbit.trail.marker.OverlayMarker.OnMarkerUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;

public class CabsActivity extends BaseActivity implements OnMapReadyCallback, OnMarkerUpdate {

  private GoogleMap mMap;

  private MarkerOverlayView markerOverlayView;

  private Bitmap markerIcon;

  private Repeat repeat;

  private ImageView imgTest;

  private float lastBearing = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_cabs);
    markerIcon = BitmapFactory.decodeResource(getResources(), R.drawable.car);

    imgTest = findViewById(R.id.imgTest);
    imgTest.setOnClickListener(v -> {
//      imgTest.setImageBitmap(
//          rotateBitmap1(markerIcon, )
//      );

      float bearing = 30;//ThreadLocalRandom.current().nextInt(0, 360 + 1);
      ValueAnimator rotateValueAnimator = ValueAnimator.ofFloat(lastBearing, bearing);
      rotateValueAnimator.addUpdateListener(animation -> {
        float v1 = (float) animation.getAnimatedValue();
        U.log("rotation", "from " + lastBearing + "| to " + bearing + " || " + "fraction : " + v);
        imgTest.setImageBitmap(rotateBitmap1(markerIcon, v1));
      });
//      rotateValueAnimator.setFloatValues(0, 1); // Ignored.
      rotateValueAnimator.setDuration(200);
      rotateValueAnimator.start();
    });

    markerOverlayView = findViewById(R.id.mapMarkerOverlayView);
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);
  }

  @Override
  public void onMapReady(GoogleMap map) {
    mMap = map;

    mMap.setOnMapLoadedCallback(() -> {
      mMap.setOnCameraMoveListener(() -> markerOverlayView.onCameraMove(mMap));

      setMapBounds(map);

      ArrayList<Car> indiranagarRoutes = LatlngData.getIndiranagarRoutes();
      repeat = new Repeat(() -> addMarkerWithAnimation(indiranagarRoutes), 1000);

      markerOverlayView.post(() -> {
        repeat.startUpdates();
      });
    });
  }

  public void addMarkerWithAnimation(List<Car> cars) {
    U.log("doublemarker", "-------------------------------addMarkerWithAnimation----------------------------");
    U.log("doublemarker", "Cars count : " + cars.size());
    U.log("doublemarker", "Markers count : " + markerOverlayView.getMarkerCount());
    U.log("doublemarker", "---------------------------------------------------------------------------------");
    runOnUiThread(() -> {
      for (Car car : cars) {
        final OverlayMarker overlayMarker = markerOverlayView.findMarkerById(car.getCarId());
        if (overlayMarker == null) {
          OverlayMarker overlayMarker1 = new OverlayMarker();
          overlayMarker1.setIcon(markerIcon.copy(markerIcon.getConfig(), true));
          overlayMarker1.setMarkerId(car.getCarId());
          overlayMarker1.setLatLng(car.getLatLng());
          overlayMarker1.setOnMarkerUpdate(CabsActivity.this);

          ValueAnimator translateValueAnimator = new ValueAnimator();
          translateValueAnimator.setInterpolator(new LinearInterpolator());
          overlayMarker1.setTranslateValueAnimator(translateValueAnimator);

          ValueAnimator rotateValueAnimator = new ValueAnimator();
          rotateValueAnimator.setInterpolator(new LinearInterpolator());
          overlayMarker1.setRotateValueAnimator(rotateValueAnimator);

          markerOverlayView.addMarker(overlayMarker1, mMap.getProjection());
        } else {
          final LatLng startLatLng = overlayMarker.getLatLng();
          final LatLng endLatLng = car.getLatLng();
          float bearing = 0;
//          overlayMarker.setIcon(markerIcon);
          bearing = getBearing(startLatLng, endLatLng);

          ValueAnimator valueAnimator = overlayMarker.getTranslateValueAnimator();
          valueAnimator.removeAllUpdateListeners();
          valueAnimator.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();
            LatLng newPosition = new LatLngInterpolator.Linear().interpolate(v, startLatLng, endLatLng);
            overlayMarker.setLatLng(newPosition);
            markerOverlayView.updateMarker(overlayMarker, mMap.getProjection());
          });
          valueAnimator.setFloatValues(0, 1); // Ignored.
          valueAnimator.setDuration(1000);
          valueAnimator.start();

          overlayMarker.setLatLng(endLatLng);
          markerOverlayView.updateMarker(overlayMarker, mMap.getProjection());

          float lastBearing = overlayMarker.getBearing();
          ValueAnimator rotateValueAnimator = overlayMarker.getRotateValueAnimator();
          rotateValueAnimator.removeAllUpdateListeners();
          rotateValueAnimator.cancel();

          final float brearingFinal = bearing;

          rotateValueAnimator = ValueAnimator.ofFloat(lastBearing, calcMinAngle(lastBearing, bearing));
          rotateValueAnimator.addUpdateListener(animation -> {
            float v = (float) animation.getAnimatedValue();
            overlayMarker.setBearing(v);
            overlayMarker.rotateIcon(v);
//            U.log("rotation", "from " + lastBearing + "| to " + brearingFinal + " || " + "fraction : " + v);
            markerOverlayView.updateMarkerAngle(overlayMarker);
          });
          rotateValueAnimator.setDuration(200);
          rotateValueAnimator.start();

          overlayMarker.setRotateValueAnimator(rotateValueAnimator);
        }
      }
    });
  }


  private Bitmap rotateBitmap(Bitmap original, float degrees) {
    int width = original.getWidth();
    int height = original.getHeight();

    Matrix matrix = new Matrix();
    matrix.preRotate(degrees);

    Bitmap workingBitmap = Bitmap.createBitmap(original);
    Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);

    Bitmap rotatedBitmap = Bitmap.createBitmap(mutableBitmap, 0, 0, width, height, matrix, true);
    Canvas canvas = new Canvas(rotatedBitmap);
    canvas.drawBitmap(original, 5.0f, 0.0f, null);

    return rotatedBitmap;
  }

  public Bitmap rotateBitmap1(Bitmap original, float degrees) {
    int width = original.getWidth();
    int height = original.getHeight();

    Matrix matrix = new Matrix();

    matrix.postRotate(degrees);

    Bitmap scaledBitmap = Bitmap.createScaledBitmap(original, width, height, true);
//    original.recycle();

    return Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
  }

  @Override
  protected void onPause() {
    super.onPause();
    repeat.stopUpdates();
  }

  @Override
  public void onMarkerUpdate() {
    markerOverlayView.invalidate();
  }
}
