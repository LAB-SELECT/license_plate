package parkingsurvey.neotsys.com;

import android.content.Context;
import android.location.LocationManager;
import android.widget.ProgressBar;

/**
 * Created by hyunchul on 2017-07-01.
 */

public class GpsControl {

    private LocationManager lm = null;
    private boolean gpsFlag = false;

    private ProgressBar loading = null;

    public GpsControl(Context mContext){
//        loading = (ProgressBar) mContext.findViewById(R.id.loading);
    }

    public void getGps(){
//        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            gpsFlag = true;
//            loading.setVisibility(VISIBLE);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                        || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                    requestPermissions(Permission.Permission, 0);
//                } else {
//                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, mLocationListener);
//                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, mLocationListener);
//                }
//            } else {
//                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, mLocationListener);
//                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, mLocationListener);
//            }
//        } else {
//            gpsFlag = false;
//            if(loading.getVisibility() == VISIBLE)
//                loading.setVisibility(View.GONE);
//
//            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//            intent.addCategory(Intent.CATEGORY_DEFAULT);
//            startActivity(intent);
//        }
    }
}
