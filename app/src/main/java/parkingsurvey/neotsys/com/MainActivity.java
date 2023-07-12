package parkingsurvey.neotsys.com;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import static com.kakao.util.maps.helper.Utility.getPackageInfo;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private Button next = null;

    private GetInfo getInfo = null;

    private ArrayList<String> userInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //getAppKeyHash();

        PermissionCheck();
    }

    private void getAppKeyHash(){
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String something = new String(Base64.encode(md.digest(), 0));
                Log.d("Hash key", something);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e("name not found", e.toString());
        }
    }

    private void PermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(Permission.Permission, 0);
            } else {
                Init();
            }
        } else {
            Init();
        }
    }

    private void Init() {
        next = (Button) findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent InputActivity = new Intent(MainActivity.this, InputActivity.class);
                startActivity(InputActivity);
            }
        });

        getInfo = new GetInfo();
        getInfo.execute();
    }

    private String GetPhoneNumber() {
        TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String result = "";

        try{
            Log.e(TAG, "고유 ID : " + telManager.getDeviceId());

//            String hash = getKeyHash(getApplicationContext());
//            Log.e(TAG, "해쉬 : " + hash);

            if(telManager.getLine1Number() != null && !telManager.getLine1Number().equals("")) {
                result = telManager.getLine1Number();

                if(telManager.getLine1Number().startsWith("+82"))
                    result = result.replace("+82", "0");
            }
        }catch (SecurityException e){
            e.printStackTrace();
        }

//        return "01046791508";
        return result;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Init();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(Permission.Permission, 0);
                    }
                }
                return;
        }
    }

    public static String getKeyHash(final Context context) throws NoSuchAlgorithmException {
        PackageInfo packageInfo = getPackageInfo(context, PackageManager.GET_SIGNATURES);
        if (packageInfo == null)
            return null;

        for (Signature signature : packageInfo.signatures) {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(signature.toByteArray());
            return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
        }
        return null;
    }

    private class GetInfo extends AsyncTask<String, Integer, String> {

        private Connection conn = null;

        private Statement stmt = null;
        private ResultSet rs = null;

        ResultSetMetaData rsmd = null;

        @Override
        protected String doInBackground(String... strings) {
            String result = null;
            int nRowCnt = 0, index = 1;

            String oracle_driver = "oracle.jdbc.driver.OracleDriver";
            String mssql_driver = "net.sourceforge.jtds.jdbc.Driver";
            String oracle_connection = "jdbc:oracle:thin:@218.233.95.187:1521:orcl";
            String mssql_connection = "jdbc:jtds:sqlserver://39.121.110.142/Parking_camera";
            String oracle_id = "egisneo";
            String mssql_id = "daegu";
            String pw = "daegu";

            boolean whoSql = true;

            String sqlDriver = whoSql ? mssql_driver : oracle_driver;
            String sqlConnection = whoSql ? mssql_connection : oracle_connection;
            String sqlId = whoSql ? mssql_id : oracle_id;

            try {
                Class.forName(sqlDriver).newInstance();

                conn = DriverManager.getConnection(sqlConnection, sqlId, pw);
                Log.e(TAG,  "DB 접속 성공");

                if(conn != null){
                    stmt = conn.createStatement();
                    rs = stmt.executeQuery("select project_id, researcher_name, researcher_phone from parking_researcher where researcher_phone = '" + GetPhoneNumber() + "'");

                    rsmd = rs.getMetaData();
                    nRowCnt = rsmd.getColumnCount();

                    userInfo = new ArrayList<>();

                    Log.e(TAG, "nRowCnt ----- = " + nRowCnt);

                    while (rs.next()) {
                        userInfo.add(rs.getString("project_id"));
                        userInfo.add(rs.getString("researcher_name"));
                        userInfo.add(rs.getString("researcher_phone"));

//                    result = rs.getString("project_id");
//                    result += rs.getString("researcher_name");
                    }

                    result = userInfo.get(1);           // user name;

                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null)
                        rs.close();

                    if (stmt != null)
                        stmt.close();

                    if (conn != null) {
                        conn.close();
                    }else{

                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (s != null) {
                Intent InputActivity = new Intent(MainActivity.this, InputActivity.class);
                InputActivity.putExtra("project_id", userInfo.get(0));
                InputActivity.putExtra("name", s);
                InputActivity.putExtra("phone", userInfo.get(2));
                startActivity(InputActivity);
                finish();
            } else {
                try{
                    Thread.sleep(2 * 1000);
                }catch(Exception e){
                    e.printStackTrace();
                }

                Toast.makeText(MainActivity.this, getString(R.string.error), Toast.LENGTH_SHORT).show();

                finish();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            try {
                if (rs != null)
                    rs.close();

                if (stmt != null)
                    stmt.close();

                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
