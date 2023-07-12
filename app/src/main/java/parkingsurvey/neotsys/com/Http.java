package parkingsurvey.neotsys.com;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by ChoDoYoung on 2017. 3. 22..
 */

public class Http {

    private HttpURLConnection conn = null;

    private OutputStream opstream = null;

    private BufferedWriter writer = null;

    private String[] data = null;

    private String[] value = null;

    public Http(String[] data, String[] value) {
        this.data = data;
        this.value = value;
    }

    public String PostResponse(String url) {
        URL Url = null;
        String body = "";
        String return_str = null;
        StringBuilder result = new StringBuilder();

        try {
            if (data.length > 0) {
                for (int i = 0; i < data.length; i++) {
                    if (i == 0)
                        body = data[i] + "=" + URLEncoder.encode(value[i], "UTF-8");
                    else
                        body = body + "&" + data[i] + "=" + URLEncoder.encode(value[i], "UTF-8");
                }
            }

            Log.e("Http", "Http ----- Send URL ----- url = " + url + "?" + URLDecoder.decode(body, "UTF-8"));
            Url = new URL(url);

            conn = (HttpURLConnection) Url.openConnection();

            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", "" + Integer.toString(body.getBytes().length));

            opstream = conn.getOutputStream();
            writer = new BufferedWriter(new OutputStreamWriter(opstream, "UTF-8"));
            writer.write(body);
            writer.flush();

            BufferedReader bufreader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line = null;

            while ((line = bufreader.readLine()) != null) {
                result.append(line);
                Log.e("HotelDoor_Http", "HotelDoor_Http ----- response = " + result.toString());
            }
        } catch (SocketTimeoutException e) {
            result = null;
            e.printStackTrace();
        } catch (NullPointerException e) {
            result = null;
            e.printStackTrace();
        } catch (SocketException e) {
            result = null;
            e.printStackTrace();
        } catch (Exception e) {
            result = null;
            e.printStackTrace();
        } finally {
            PostDisconnect();
        }

        if (result != null)
            return_str = result.toString();

        return return_str;
    }

    public void PostDisconnect() {
        try {
            if (writer != null) {
                writer.close();
                writer = null;
            }

            if (opstream != null) {
                opstream.close();
                opstream = null;
            }

            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
