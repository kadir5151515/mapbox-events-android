package com.mapbox.android.core.location;

import android.app.PendingIntent;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.mapbox.android.core.location.Utils.checkNotNull;

class LocationEngineProxy<T> implements LocationEngine {
  private final LocationEngineImpl<T> locationEngineImpl;
  private Map<LocationEngineCallback<LocationEngineResult>, T> listeners;

  LocationEngineProxy(LocationEngineImpl<T> locationEngineImpl) {
    this.locationEngineImpl = locationEngineImpl;
  }

  @Override
  public void getLastLocation(@NonNull LocationEngineCallback<LocationEngineResult> callback) throws SecurityException {
    checkNotNull(callback, "callback == null");
    locationEngineImpl.getLastLocation(callback);
  }

  @Override
  public void requestLocationUpdates(@NonNull LocationEngineRequest request,
                                     @NonNull LocationEngineCallback<LocationEngineResult> callback,
                                     @Nullable Looper looper) throws SecurityException {
    checkNotNull(request, "request == null");
    checkNotNull(callback, "callback == null");
    T listener = getListener(callback);
    if (listener instanceof LocationCallbackTransport) {
      ((LocationCallbackTransport) listener).setFastInterval(request.getFastestInterval());
    }
    locationEngineImpl.requestLocationUpdates(request, listener,
      looper == null ? Looper.getMainLooper() : looper);
  }

  @Override
  public void requestLocationUpdates(@NonNull LocationEngineRequest request,
                                     PendingIntent pendingIntent) throws SecurityException {
    checkNotNull(request, "request == null");
    locationEngineImpl.requestLocationUpdates(request, pendingIntent);
  }

  @Override
  public void removeLocationUpdates(@NonNull LocationEngineCallback<LocationEngineResult> callback) {
    checkNotNull(callback, "callback == null");
    locationEngineImpl.removeLocationUpdates(removeListener(callback));
  }

  @Override
  public void removeLocationUpdates(PendingIntent pendingIntent) {
    locationEngineImpl.removeLocationUpdates(pendingIntent);
  }

  @VisibleForTesting
  int getListenersCount() {
    return listeners != null ? listeners.size() : 0;
  }

  @VisibleForTesting
  T getListener(@NonNull LocationEngineCallback<LocationEngineResult> callback) {
    if (listeners == null) {
      listeners = new ConcurrentHashMap<>();
    }

    T listener = listeners.get(callback);
    if (listener == null) {
      listener = locationEngineImpl.createListener(callback);
    }
    listeners.put(callback, listener);
    return listener;
  }

  @VisibleForTesting
  T removeListener(@NonNull LocationEngineCallback<LocationEngineResult> callback) {
    return listeners != null ? listeners.remove(callback) : null;
  }
}