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

    public String getFormattedLocation() {
        Location location = myLocationListener.getCurrentLocation();

        return location == null ? "" : String.format(context.getString(R.string.bulb_location_string_format),
                location.getLongitude(),
                location.getLatitude());
    }

    public void setLocation(Location location) {
        this.myLocationListener.setCurrentLocation(location);
    }
}
