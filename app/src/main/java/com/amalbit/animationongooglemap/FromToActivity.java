package com.amalbit.animationongooglemap;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import com.amalbit.animationongooglemap.marker.LatLngInterpolator;
import com.amalbit.animationongooglemap.marker.Repeat;
import com.amalbit.animationongooglemap.data.CarData.Car;
import com.amalbit.animationongooglemap.data.LatlngData;
import com.amalbit.trail.MarkerOverlayView;
import com.amalbit.trail.OverlayMarker;
import com.amalbit.trail.OverlayMarker.OnMarkerUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;

public class FromToActivity extends AppCompatActivity implements OnMapReadyCallback, OnMarkerUpdate {

  private GoogleMap mMap;

  private MarkerOverlayView markerOverlayView;

  private List<LatLng> fromTO = new ArrayList<>();

  private Bitmap markerIcon;

  private Repeat repeat;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_from_to);
    markerIcon = BitmapFactory.decodeResource(getResources(), R.drawable.car);
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

      setMapBounds();

      ArrayList<Car> indiranagarRoutes = LatlngData.getIndiranagarRoutes();
      repeat = new Repeat(() -> addMarkerWithAnimation(indiranagarRoutes), 1000);
      repeat.startUpdates();

      mMap.setOnMapClickListener(latLng -> {
        Log.i("LatlngClick", latLng.latitude + "," + latLng.longitude);
        if (fromTO.size() >= 2) {
          fromTO.clear();
          mMap.clear();
        }

        fromTO.add(latLng);
        addMarker(fromTO);
        if (fromTO.size() == 2) {
          getDirections(new com.google.maps.model.LatLng(fromTO.get(0).latitude, fromTO.get(0).longitude),
              new com.google.maps.model.LatLng(fromTO.get(1).latitude, fromTO.get(1).longitude));
        }
      });
    });
  }

  public void setMapBounds() {
    List<LatLng> latLngs = new ArrayList<>();
    latLngs.add(new LatLng(12.9715002, 77.6374856));//NW
    latLngs.add(new LatLng(12.9703733, 77.6372037));//NE
    latLngs.add(new LatLng(12.9595674, 77.6366595));//SE
    latLngs.add(new LatLng(12.9595672, 77.6519803));//SW
    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
    for (LatLng latLngPoint : latLngs) {
      boundsBuilder.include(latLngPoint);
    }
    LatLngBounds latLngBounds = boundsBuilder.build();
    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
  }

  public void addMarkerWithAnimation(List<Car> cars) {
    runOnUiThread(() -> {
      for (Car car : cars) {
        final OverlayMarker overlayMarker = markerOverlayView.findMarkerById(car.getCarId());
        if (overlayMarker == null) {
          OverlayMarker overlayMarker1 = new OverlayMarker();
          overlayMarker1.setIcon(markerIcon);
          overlayMarker1.setMarkerId(car.getCarId());
          overlayMarker1.setLatLng(car.getLatLng());
          overlayMarker1.setOnMarkerUpdate(FromToActivity.this);
          markerOverlayView.addMarker(overlayMarker1, mMap.getProjection());
        } else {
          final LatLng startLatLng = overlayMarker.getLatLng();
          final LatLng endLatLng = car.getLatLng();

          ValueAnimator valueAnimator = new ValueAnimator();
          valueAnimator.setInterpolator(new LinearInterpolator());
          valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
            private LatLng lastLatlng = null;
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
              float v = animation.getAnimatedFraction();
              float bearing= 0;
              LatLng newPosition = new LatLngInterpolator.Linear().interpolate(v, startLatLng, endLatLng);
              if (lastLatlng ==  null) {
                bearing = getBearing(startLatLng, newPosition);
              } else {
                bearing = getBearing(lastLatlng, startLatLng);
              }
              overlayMarker.setLatLng(newPosition);
              overlayMarker.setBearing(bearing);
              markerOverlayView.updateMarker(overlayMarker, mMap.getProjection());

              lastLatlng = newPosition;
            }
          });
          valueAnimator.setFloatValues(0, 1); // Ignored.
          valueAnimator.setDuration(1000);
          valueAnimator.start();

        }
      }
    });
  }

  TypeEvaluator<LatLng> typeEvaluator =
      (fraction, startValue, endValue) ->
          new LatLngInterpolator.Linear().interpolate(fraction, startValue, endValue);

  private float getBearing(LatLng begin, LatLng end) {
    double lat = Math.abs(begin.latitude - end.latitude);
    double lng = Math.abs(begin.longitude - end.longitude);

    if (begin.latitude < end.latitude && begin.longitude < end.longitude)
      return (float) (Math.toDegrees(Math.atan(lng / lat)));
    else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
      return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
    else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
      return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
    else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
      return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
    return -1;
  }
  public void addMarker(List<LatLng> latLngs) {
    for (LatLng latLng : latLngs) {
      mMap.addMarker(new MarkerOptions().position(latLng));
    }
  }

  public void getDirections(com.google.maps.model.LatLng fromLatlng, com.google.maps.model.LatLng toLatlng) {
    AsyncTask.execute(new Runnable() {
      @Override
      public void run() {
        try {
          DateTime now = new DateTime();
          DirectionsResult result = DirectionsApi.newRequest(getGeoContext())
              .mode(TravelMode.DRIVING)
              .origin(fromLatlng)
              .destination(toLatlng)
              .departureTime(now)
              .await();
          List<LatLng> decodedPath = PolyUtil.decode(result.routes[0].overviewPolyline.getEncodedPath());
          writeToFile(decodedPath, getApplicationContext());
          runOnUiThread(() -> mMap.addPolyline(new PolylineOptions().addAll(decodedPath)));
        } catch (Exception e) {
          Log.i("Directions", e.getLocalizedMessage());
        }
      }
    });
  }

  @Override
  protected void onPause() {
    super.onPause();
    repeat.stopUpdates();
  }

  private GeoApiContext getGeoContext() {
    GeoApiContext geoApiContext = new GeoApiContext();
    return geoApiContext.setQueryRateLimit(3)
        .setApiKey("AIzaSyC26e2qMdpjUGtTdo5yt7ll4W_oHDoY_yI")
        .setConnectTimeout(1, TimeUnit.SECONDS)
        .setReadTimeout(1, TimeUnit.SECONDS)
        .setWriteTimeout(1, TimeUnit.SECONDS);
  }

  private void writeToFile(List<LatLng> decodedPath, Context context) {
    try {
      File root = new File(Environment.getExternalStorageDirectory().toString());
      Random random = new Random();
      int max = 10000000;
      int min = 1;
      int randomNum = random.nextInt(max - min + 1) + min;
      File gpxfile = new File(root, randomNum + ".txt");
      FileWriter writer = new FileWriter(gpxfile);
//      BufferedWriter bw = new BufferedWriter(writer);
      for (LatLng latLng : decodedPath) {
//        route.add(new LatLng(12.94695, 77.64058));
        writer.append("route.add(new LatLng(");
        writer.append(String.valueOf(latLng.latitude));
        writer.append(", ");
        writer.append(String.valueOf(latLng.longitude));
        writer.append("));");
        writer.append("\n");
      }
      writer.flush();
      writer.close();
    }
    catch (IOException e) {
      Log.e("Exception", "File write failed: " + e.toString());
    }
  }

  @Override
  public void onMarkerUpdate() {
    markerOverlayView.invalidate();
  }
}
