package parkingsurvey.neotsys.com;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static net.daum.mf.map.api.MapView.CurrentLocationTrackingMode.TrackingModeOff;
import static net.daum.mf.map.api.MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading;

/**
 * Created by hyunchul on 2017-06-18.
 */

public class SurveyMap extends AppCompatActivity implements View.OnClickListener, MapView.MapViewEventListener, MapView.POIItemEventListener, MapView.OpenAPIKeyAuthenticationResultListener, MapView.CurrentLocationEventListener{

//        implements  MapView.MapViewEventListener, MapView.POIItemEventListener, MapView.OpenAPIKeyAuthenticationResultListener{
    final String LOG_TAG = "SurveyMap";
    final static String API_KEY = "0eb35613ae427d017a0707df53324d76";

    public MapView mMapView;

    private MapTraceConfirm mTraceConfrim = null;
    private LocationManager lm = null;

    private String sName = null;
    private String sPhone = null;
    private String sProjectID = null;

    private Button mDateBtn = null;
    private Button mMarkBtn = null;
    private FloatingActionButton fab;
    private FloatingActionButton mapTypeIcon;

    private TextView mSearchDay = null;

    private MapPOIItem today_marker = null;

    private boolean gpsFlag = false;
    private boolean satellite_mapIcon = true;

    private RadioButton r_day_btn, r_night_btn;

    private Double lat = 0.0d, lon = 0.0d;
    int date[] = new int[3];            // 0 : year, 1 : month, 2 : day

    String sDate = null, eDate = null;

//    private ArrayList<Double> lonlat = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.map_search);

        Init();
    }

    private void Init(){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        today_marker = new MapPOIItem();

        IntentSet();            //  조사한 지도를 확인하기 위해 project_id, name, phone number를 setting 함

        mDateBtn = (Button) findViewById(R.id.date_btn);
        mDateBtn.setOnClickListener(this);

        mMarkBtn = (Button) findViewById(R.id.mark_btn) ;
        mMarkBtn.setOnClickListener(this);

        r_day_btn = (RadioButton) findViewById(R.id.day_btn);
        r_night_btn = (RadioButton) findViewById(R.id.night_btn);
        r_day_btn.setOnClickListener(radioBtnClickListener);
        r_night_btn.setOnClickListener(radioBtnClickListener);
        r_day_btn.setChecked(true);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        mapTypeIcon = (FloatingActionButton) findViewById((R.id.mapType));
        mapTypeIcon.setOnClickListener(this);

        mSearchDay = (TextView) findViewById(R.id.search_day) ;
        mSearchDay.setText(nowGetDate());

//        MapLayout mapLayout = new MapLayout(CheckUserSearch.this);
//        mMapView = mapLayout.getMapView();

        mMapView = new MapView(SurveyMap.this);
        mMapView.setDaumMapApiKey(API_KEY);
        mMapView.setMapType(MapView.MapType.Satellite);
        mMapView.setShowCurrentLocationMarker(true);
        mMapView.setMapViewEventListener(this);
        mMapView.setPOIItemEventListener(this);
        mMapView.setCurrentLocationEventListener(this);
//        mMapView.setOpenAPIKeyAuthenticationResultListener(this);                 // 그냥 주석되어 있었음.
        LinearLayout container = (LinearLayout) findViewById(R.id.map_view);
        container.addView(mMapView);
    }

    private String nowGetDate() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy-MM-dd");
        String strDisNow = sdfNow.format(date);

//        SimpleDateFormat strQueryNow = new SimpleDateFormat("yyyyMMdd");
//        String strQueryDate = strQueryNow.format(date);
//        searchNowSetDate(strQueryDate);         // DB 조회 날짜 쿼리 셋팅

        if(r_day_btn.isChecked()){
            sDate = strDisNow + " 07:00:00";
            eDate = strDisNow + " 20:00:00";
        }
        else if(r_night_btn.isChecked()){
            sDate = strDisNow + " 20:00:00";

            // 현재날짜를 셋팅 한다.(야간시간을 적용하기 위해 현재날짜를 기준으로 오후8시(20시)부터 ~ 익일 오전6시59분59초까지(오전7시전까지)
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DATE, 1);
            eDate = sdfNow.format(cal.getTime()) + " 07:00:00";
        }

        return strDisNow;
    }

    private void IntentSet(){
//        Log.e(LOG_TAG, "now time: " + nowGetDate());

        sName = getIntent().getStringExtra("name");
        sPhone = getIntent().getStringExtra("phone");
        sProjectID = getIntent().getStringExtra("project_id");

        Log.e(LOG_TAG, "sName:" + sName + ", sPhone: " + sPhone + ", sProjectID: " + sProjectID);
    }

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (gpsFlag) {

                if(location.getProvider().equals(LocationManager.GPS_PROVIDER) ){
                    fab.setImageResource(R.drawable.gps_on);
//                    loading.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            Log.e(LOG_TAG, "onStatusChanged()");
        }

        @Override
        public void onProviderEnabled(String s) {
            Log.e(LOG_TAG, "onProviderEnabled()");
        }

        @Override
        public void onProviderDisabled(String s) {
            Log.e(LOG_TAG, "onProviderDisabled()");

            Enum<MapView.CurrentLocationTrackingMode> mapTrackingMode = TrackingModeOff;

            Log.e(LOG_TAG, "mapTrackingMode: " + mapTrackingMode + ", gpsFlag : " + gpsFlag);

            mapTrackingMode = mMapView.getCurrentLocationTrackingMode();
            if(mapTrackingMode == TrackingModeOnWithoutHeading && gpsFlag) {
                mMapView.setCurrentLocationTrackingMode(TrackingModeOff);
                fab.setImageResource(R.drawable.gps_off);
                lm.removeUpdates(mLocationListener);
            }
        }
    };

    private void currentGetGps(){
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.e(LOG_TAG, "currentGetGps()");
            gpsFlag = true;
//            loading.setVisibility(VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(Permission.Permission, 0);
                } else {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, mLocationListener);
//                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, mLocationListener);
                }
            } else {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, mLocationListener);
//                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, mLocationListener);
            }
        } else {
//            gpsFlag = false;
//            if(loading.getVisibility() == VISIBLE)
//                loading.setVisibility(View.GONE);

            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivity(intent);
        }
    }

    @Override
    public void onMapViewInitialized(MapView mapView) {
        Log.e(LOG_TAG, "onMapViewInitialized");

        mMapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(35.857811, 128.557050), true);
    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {
        Log.e(LOG_TAG, "onMapViewCenterPointMoved");
    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int i) {
        Log.e(LOG_TAG, "onMapViewZoomLevelChanged");
    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {
        Log.e(LOG_TAG, "onMapViewSingleTapped");
    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {
        Log.e(LOG_TAG, "onMapViewDoubleTapped");
    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {
        Log.e(LOG_TAG, "onMapViewLongPressed");
    }

    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {
        Log.e(LOG_TAG, "onMapViewDragStarted" + ", gpsFlag: " + gpsFlag);

        if(gpsFlag) {           // 현재 위치를 해제하고 싶을 때(GPS 리스너를 삭제하고자 할 때...
            gpsFlag = false;

            mMapView.setCurrentLocationTrackingMode(TrackingModeOff);
            fab.setImageResource(R.drawable.gps_off);
            lm.removeUpdates(mLocationListener);
        }
    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {
        Log.e(LOG_TAG, "onMapViewDragEnded");
    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {
        Log.e(LOG_TAG, "onMapViewMoveFinished");
    }

    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
        Log.e(LOG_TAG, "onPOIItemSelected");
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {
        Log.e(LOG_TAG, "onCalloutBalloonOfPOIItemTouched");
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {
        Log.e(LOG_TAG, "onCalloutBalloonOfPOIItemTouched");
    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {
        Log.e(LOG_TAG, "onDraggablePOIItemMoved");
    }

    @Override
    public void onDaumMapOpenAPIKeyAuthenticationResult(MapView mapView, int i, String s) {
        Log.e(LOG_TAG, "onDaumMapOpenAPIKeyAuthenticationResult");
    }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {
        Log.e(LOG_TAG, "onCurrentLocationUpdate");

        MapView.CurrentLocationTrackingMode mapTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff;

        mapTrackingMode = mapView.getCurrentLocationTrackingMode();
        Log.e(LOG_TAG, "current tracking mode = " + mapTrackingMode + ", gpsFlag : " + gpsFlag);

        if(mapTrackingMode == TrackingModeOnWithoutHeading ){
            gpsFlag = true;
            fab.setImageResource(R.drawable.gps_on);                    // TrackingModeOnWithoutHeading 모드일 때 지도위의 gps on/off 아이콘이 off 일 경우 있음.
        } else if(mapTrackingMode == TrackingModeOff && gpsFlag == true){
            gpsFlag = false;
            fab.setImageResource(R.drawable.gps_off);
            mapView.setCurrentLocationTrackingMode(mapTrackingMode);
        }
    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {
        Log.e(LOG_TAG, "onCurrentLocationDeviceHeadingUpdate");
    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {
        Log.e(LOG_TAG, "onCurrentLocationUpdateFailed");
    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {
        Log.e(LOG_TAG, "onCurrentLocationUpdateCancelled");
    }


    private void calendarSetDate(){
        GregorianCalendar calendar = new GregorianCalendar();
        date[0] = calendar.get(Calendar.YEAR);
        date[1] = calendar.get(Calendar.MONTH);
        date[2] = calendar.get(Calendar.DAY_OF_MONTH);
    }

    private String seachNowGetDate(){
        return sDate;
    }

    private void searchNowSetDate(String s){
        sDate = s;
    }

    private DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            try {
                DecimalFormat decimalFormat = new DecimalFormat("00");      // 포맷 2자리 만드는 규칙 example : 7월2일(72) -> 0702
                DecimalFormat yearFormat = new DecimalFormat("0000");
//            String msg = yearFormat.format(year) + decimalFormat.format(month + 1) + decimalFormat.format(dayOfMonth) ;
//            searchNowSetDate(msg);

                String txtMsg = String.format("%d-%d-%d", year, month+1, dayOfMonth);
                mSearchDay.setText(txtMsg);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                Date mDate = dateFormat.parse(txtMsg);
                if(r_day_btn.isChecked()){
                    sDate = txtMsg + " 07:00:00";
                    eDate = txtMsg + " 20:00:00";
                }
                else if(r_night_btn.isChecked()){
                    // 현재날짜를 셋팅 한다.(야간시간을 적용하기 위해 현재날짜를 기준으로 오후8시(20시)부터 ~ 익일 오전6시59분59초까지(오전7시전까지)
                    sDate = txtMsg + " 20:00:00";
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(mDate);
                    cal.add(Calendar.DATE, 1);
                    eDate = dateFormat.format(cal.getTime()) + " 07:00:00";
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    };

    private void gpsOff(){
        gpsFlag = false;

        mMapView.setCurrentLocationTrackingMode(TrackingModeOff);
        fab.setImageResource(R.drawable.gps_off);
        lm.removeUpdates(mLocationListener);
    }

    private void change_mapType(){
        if(satellite_mapIcon){
            mapTypeIcon.setImageResource(R.drawable.satellite_map);
            satellite_mapIcon = false;
            mMapView.setMapType(MapView.MapType.Standard);
        }else{
            mapTypeIcon.setImageResource(R.drawable.normal_map);
            satellite_mapIcon = true;
            mMapView.setMapType(MapView.MapType.Satellite);
        }
    }

    RadioButton.OnClickListener radioBtnClickListener = new RadioButton.OnClickListener(){
        @Override
        public void onClick(View view) {
            try {
                if(r_day_btn.isChecked()){
                    sDate = mSearchDay.getText() + " 07:00:00";
                    eDate = mSearchDay.getText() + " 20:00:00";
                }
                else if(r_night_btn.isChecked()) {
                    sDate = mSearchDay.getText() + " 20:00:00";
                    // 검색날짜를 셋팅 한다.(야간시간을 적용하기 위해 현재날짜를 기준으로 오후8시(20시)부터 ~ 익일 오전6시59분59초까지(오전7시전까지)
                    String txtMsg = mSearchDay.getText().toString();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    Date mDate = dateFormat.parse(txtMsg);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(mDate);
                    cal.add(Calendar.DATE, 1);
                    eDate = dateFormat.format(cal.getTime()) + " 07:00:00";
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Log.d(LOG_TAG, "sDate: " + sDate + ", eDate: " + eDate);
        }
    };

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.date_btn:
                calendarSetDate();
                new DatePickerDialog(this, dateSetListener, date[0], date[1], date[2]).show();

                break;

            case R.id.mark_btn:
                int nSearchLength = mSearchDay.length();

                if(nSearchLength < 1){
                    Toast.makeText(SurveyMap.this, "날짜를 선택하십시오!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(gpsFlag) {
                    gpsOff();
                }

                mMapView.removeAllPOIItems();

                mTraceConfrim = new MapTraceConfirm();
                mTraceConfrim.execute();

                break;

            case R.id.fab:

                if(gpsFlag){           // 현재 위치를 해제하고 싶을 때(GPS 리스너를 삭제하고자 할 때...
                    gpsOff();
                } else{
                    CurrentLocationMapMove();
                }

                break;
            case R.id.mapType:
                change_mapType();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mTraceConfrim != null && mTraceConfrim.getStatus() != AsyncTask.Status.FINISHED)
                    mTraceConfrim.cancel(true);

                Log.d(LOG_TAG, "back key");
                lm.removeUpdates(mLocationListener);
                finish();
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void CurrentLocationMapMove(){

        // 현재 GPS를 받아와서 custom marker로 찍어준다.
        currentGetGps();

        // 다음지도 트랙킹 API 사용하여 현재 위치 적용(지속적으로 현재위치를 찍어준다.)
//        mMapView.setCurrentLocationEventListener(this);
//        mMapView.setPOIItemEventListener(this);


//        mMapView.setMapType(MapView.MapType.Satellite);
        mMapView.setShowCurrentLocationMarker(true);
        mMapView.setCurrentLocationTrackingMode(TrackingModeOnWithoutHeading);
    }

    private class MapTraceConfirm extends AsyncTask<String, Integer, String> {
        private final String TAG = "MapTraceConfirm";

        String sValue = null;
        String mssql_driver = "net.sourceforge.jtds.jdbc.Driver";
        String mssql_connection = "jdbc:jtds:sqlserver://39.121.110.142/Parking_camera";
        String mssql_id = "daegu";
        String pw = "daegu";

        Map<Integer, Double> coordinate = new HashMap<Integer, Double>();

        private Connection conn = null;
        private Statement stmt = null;
        private ResultSet lon_lat = null;

        @Override
        protected String doInBackground(String... params) {
            String result = "success";

//            String sQuery =  "select latitude, longitude from parking_data where researcher_name = '" + sName +"' and project_id = '"
//                    + sProjectID + "' and researcher_phone = '" + sPhone+ "' and CONVERT(char(8), research_date, 112)" + " between '" + seachNowGetDate() + "' and '" + seachNowGetDate()+ "'";

            String sQuery =  "select latitude, longitude from parking_data where researcher_name = '" + sName +"' and project_id = '"
                    + sProjectID + "' and researcher_phone = '" + sPhone + "' and  research_date > '" + sDate + "' and research_date < '" + eDate + "'";

            Log.d(LOG_TAG, "sQuery: " + sQuery);
            try{
                Class.forName(mssql_driver).newInstance();

                conn = DriverManager.getConnection(mssql_connection, mssql_id, pw);

                if(conn != null)
                    stmt = conn.createStatement();

                if(stmt == null){
                    Log.e(TAG, "stmt value null");
                    return sValue ;
                }


                lon_lat = stmt.executeQuery(sQuery);

                if(lon_lat == null){
                    Log.e(TAG, "lon_lat null");
                    return sValue;
                }

                int nPos = 0;
                while(lon_lat.next()){
                    coordinate.put(nPos, lon_lat.getDouble(1));
                    nPos++;
                    coordinate.put(nPos, lon_lat.getDouble(2));
                    nPos++;
                }

                if(nPos == 0){
                    result = "no_data";
                }

                Log.e(TAG, "doInBackground");

            }catch (SQLException s){
                s.printStackTrace();
            }catch(Exception e){
                e.printStackTrace();
            } finally {
                try{
                    if(lon_lat != null){
                        lon_lat.close();
                        lon_lat = null;
                    }

                    if(stmt != null){
                        stmt.close();
                        stmt = null;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if(s.equals("success")){
                if(coordinate.size() > 0){
                    mMapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(coordinate.get(0), coordinate.get(1)), true);
                    today_marker.setItemName("조사포인트");

                    today_marker.setTag(1);
                    today_marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
                    today_marker.setCustomImageResourceId(R.drawable.track_green);
                    today_marker.setCustomImageAutoscale(false);
                    today_marker.setCustomImageAnchor(0.5f, 1.0f);

                    for(int i=0, j=1; i<coordinate.size();) {
                        today_marker.setMapPoint(MapPoint.mapPointWithGeoCoord(coordinate.get(i), coordinate.get(j)));
                        i = i + 2;
                        j = j + 2;
                        mMapView.addPOIItem(today_marker);
                    }
                }
            } else if(s.equals("no_data")){
                Toast.makeText(SurveyMap.this, "선택하신 날짜는 조사 내역이 없습니다.!", Toast.LENGTH_LONG).show();
            }

            Log.e(TAG, "onPostExecute");

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            Log.e(TAG, "onCancelled");
        }
    }
}

