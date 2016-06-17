package me.anoxic.bulber2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

/**
 * A location manager. Used as an mediator between the main application and the location
 * Created by Anoxic on 061616.
 */
public class MyLocationManager {
    Context context;
    MyLocationListener myLocationListener;

    MyLocationManager(Context context) {
        this.context = context;

        LocationManager mLocManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        myLocationListener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context,
                    context.getString(R.string.bulb_location_request_fail),
                    Toast.LENGTH_SHORT);
            return;
        }

        mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
    }

    /**
     * Returns a string of a pair of latitude and longitude, in the format of #[lat,long]
     *
     * @return a string of current location, formatted as #[lat,long]
     */
    public String getFormattedLocation() {
        Location location = myLocationListener.getCurrentLocation();

        return location == null ? "" : String.format(context.getString(R.string.bulb_location_string_format),
                location.getLatitude(),
                location.getLongitude());
    }

    /**
     * Returns a string of current location of latitude and longitude, along with a given name, in the format of #[name,lat,long]
     *
     * @return a string of current location, formatted as #[name,lat,long]
     */
    public String getFormattedLocationWithName(String name) {
        Location location = myLocationListener.getCurrentLocation();

        return location == null ? "" : String.format(context.getString(R.string.bulb_location_string_format_with_name),
                name,
                location.getLatitude(),
                location.getLongitude());
    }

    public void setLocation(Location location) {
        this.myLocationListener.setCurrentLocation(location);
    }

    public Location getLocation() {
        return this.myLocationListener.getCurrentLocation();
    }


}
