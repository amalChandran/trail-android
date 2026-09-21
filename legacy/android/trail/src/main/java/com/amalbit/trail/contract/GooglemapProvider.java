package com.amalbit.trail.contract;

import com.google.android.gms.maps.GoogleMap;
import java.lang.ref.WeakReference;

public interface GooglemapProvider {
  public WeakReference<GoogleMap> getGoogleMapWeakReference();
}

