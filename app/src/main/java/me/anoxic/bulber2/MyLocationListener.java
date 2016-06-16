package me.anoxic.bulber2;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

/**
 * My location listener. Used to get the current location
 * Created by Anoxic on 061616.
 */
public class MyLocationListener implements LocationListener {

    private Location currentLocation = null;

    public Location getCurrentLocation() {
        return currentLocation;
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void setCurrentLocation(Location location) {
        this.currentLocation = location;
    }
}
