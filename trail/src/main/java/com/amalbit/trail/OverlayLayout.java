package com.amalbit.trail;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.amalbit.trail.contract.GooglemapProvider;
import com.amalbit.trail.marker.ViewOverlayView;
import com.google.android.gms.maps.GoogleMap;
import java.lang.ref.WeakReference;

public class OverlayLayout extends FrameLayout implements GooglemapProvider {

  @Nullable
  private WeakReference<GoogleMap> googleMapWeakReference;

  private RouteOverlayView routeOverlayView;

  private ViewOverlayView viewOverlayView;

  public OverlayLayout(@NonNull Context context) {
    super(context);
    init(context);
  }

  public OverlayLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public OverlayLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    LayoutParams matchParentParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    routeOverlayView = new RouteOverlayView(context);
    routeOverlayView.setLayoutParams(matchParentParams);
    routeOverlayView.addGoogleMapProvider(this);
    addView(routeOverlayView);

    viewOverlayView = new ViewOverlayView(context);
    viewOverlayView.setLayoutParams(matchParentParams);
    viewOverlayView.addGoogleMapProvider(this);
    addView(viewOverlayView);
  }

  public void addGoogleMap(GoogleMap googleMap) {
    this.googleMapWeakReference = new WeakReference<>(googleMap);
  }

  public RouteOverlayView getRouteOverlayView() {
    return routeOverlayView;
  }

  public ViewOverlayView getViewOverlayView() {
    return viewOverlayView;
  }


  public void onCameraMoved(){
    if (googleMapWeakReference != null &&
        googleMapWeakReference.get() != null) {
      routeOverlayView.onCameraMove();
      viewOverlayView.onCameraMove();
    }
  }

  @Nullable
  @Override
  public WeakReference<GoogleMap> getGoogleMapWeakReference() {
    return googleMapWeakReference;
  }
}
