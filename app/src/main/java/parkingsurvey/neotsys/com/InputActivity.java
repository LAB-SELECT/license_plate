package parkingsurvey.neotsys.com;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import static android.view.View.VISIBLE;
import static android.Manifest.permission.CAMERA;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by ChoiHyunChul on 2017. 3. 27..
 */

public class InputActivity extends AppCompatActivity implements View.OnClickListener,
        CameraBridgeViewBase.CvCameraViewListener2 {

    private final String TAG = "InputActivity";

    private LinearLayout fl = null;

    private InputMethodManager imm;

    private TextView name = null;

    private EditText carNumber = null;

    private TextView lat = null;
    private TextView lng = null;
    private TextView check_count = null;

    private Button model = null;
    private Button legal = null;
    private Button modelName = null;
    private Button legalName = null;
    private Button gps = null;
    private Button register = null;

    private ProgressBar loading = null;

    private ArrayList<String> modelList = new ArrayList<>();
    private ArrayList<String> legalList = new ArrayList<>();

    private boolean gpsFlag = false;
    private boolean isNet = false;              // NetWork 상태 확인(데이터 요금이 남아 있는지 확인 또는 정상적으로 데이터베이스 연결이 되었는지 확인

    private String sProjectID = null;
    private String sPhone = null;
    private String sName = null;
    private String sendModel = null;
    private String sendLegal = null;
    private String sendLat = null;
    private String sendLng = null;
    private String search_count = null;

    private String InputErrorDuplicate = "Duplicate_InputError";
    private String NETWORK_SEND_SUCCESS = "OK";
    private String NETWORK_SEND_FAIL = "FAIL";

    private LocationManager lm = null;

    private AlertDialog alertKind = null;
    private AlertDialog alertLegal = null;

    private GetInfo getInfo = null;

    private CheckCarNumber mCheckCarNumber;

    private View btnOpenView = null;
    private Button btnOpen = null;

    private Mat matInput;
    private Mat matResult;
    private CameraBridgeViewBase mOpenCvCameraView;

    // 실시간 번호판 탐지 코드
    private CameraBridgeViewBase m_CameraView;
    DHDetectionModel detectionModel;
    GpuDelegate delegate = new GpuDelegate();
    public Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);
    private Bitmap onFrame; // yolo input
    private Bitmap onFrame2; // clova input
    String onFrame2_base64;
    String before_image = "";
    boolean cFlag=true;
    Call<JsonObject> call;
    public interface OCRService {
        @Headers({
                "Content-Type: application/json; charset=utf-8",
                "X-OCR-SECRET: "
        })
        @POST("general")
        Call<JsonObject> doOCR(@Body JsonObject requestBody);
    }
    private static final String BASE_URL = "https://k3jyg1t7lb.apigw.ntruss.com/custom/v1/21307/d6c07e9b3b323a6498af50bc24fc0211e8acd512b02e7df2899181011329a6c7/";

    private final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private final OCRService ocrService = retrofit.create(OCRService.class);

//    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);


//    static {
//        System.loadLibrary("opencv_java4");
//        System.loadLibrary("neoparking_camera");
//    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // javacamera
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.input);

        // javacamera

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(InputActivity.this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCheckCarNumber = new CheckCarNumber(InputActivity.this);

        isNet = true;

        name = (TextView) findViewById(R.id.name);
        name.setText(getIntent().getStringExtra("name"));
        sName = getIntent().getStringExtra("name");

        sProjectID = getIntent().getStringExtra("project_id");
        sPhone = getIntent().getStringExtra("phone");

        model = (Button) findViewById(R.id.model);
        model.setOnClickListener(this);
        legal = (Button) findViewById(R.id.legal);
        legal.setOnClickListener(this);

        carNumber = (EditText) findViewById(R.id.carNumber);

        modelName = (Button) findViewById(R.id.modelName);
        legalName = (Button) findViewById(R.id.legalName);

        modelName.setOnClickListener(this);
        legalName.setOnClickListener(this);

        gps = (Button) findViewById(R.id.gps);
        gps.setOnClickListener(this);

        fl = (LinearLayout) findViewById(R.id.fl);
        fl.setOnClickListener(this);

        lat = (TextView) findViewById(R.id.lat);
        lng = (TextView) findViewById(R.id.lng);

        check_count = (TextView) findViewById(R.id.check_count);

        register = (Button) findViewById(R.id.register);
        register.setOnClickListener(this);

        btnOpenView = (View) findViewById(R.id.include_btnOpen);
        btnOpen = (Button) btnOpenView.findViewById(R.id.btnMap);
        btnOpen.setOnClickListener(this);

        loading = (ProgressBar) findViewById(R.id.loading);

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        Log.e(TAG, "onCreate -----");
        getInfo = new GetInfo("INFO");
        getInfo.execute();

        try {
            detectionModel = new DHDetectionModel(this, options);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    // javacamera

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matInput = inputFrame.rgba();
        Mat input = matInput.clone();

        Mat toDetImage = new Mat();
        Size sz = new Size(256, 192);
        Imgproc.resize(matInput, toDetImage, sz);
        onFrame = Bitmap.createBitmap(toDetImage.cols(), toDetImage.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(toDetImage, onFrame);
        //long yolo_s = System.currentTimeMillis();     // 모델 추론 시간 확인을 위한 코드
        float[][] proposal = detectionModel.getProposal(onFrame, input);
        //long yolo_e = System.currentTimeMillis();
        //inferenceTime[0] = yolo_e-yolo_s;

        if(proposal[1][4] < 0.5){ // reject inference
            return matInput;
        }

        int w = matInput.width();
        int h = matInput.height();
        float[] coord = new float[8];
        float x_center= proposal[1][0];
        float y_center = proposal[1][1];
        float width = proposal[1][2];
        float height = proposal[1][3];

        coord[0]= (float) (x_center-0.5*width);
        coord[1]= (float) (y_center-0.5*height);
        coord[2]= (float) (x_center+0.5*width);
        coord[3]= (float) (y_center-0.5*height);
        coord[4]= (float) (x_center+0.5*width);
        coord[5]= (float) (y_center+0.5*height);
        coord[6]= (float) (x_center-0.5*width);
        coord[7]= (float) (y_center+0.5*height);

        int w_ = (int) (0.01 * w);
        int h_ = (int) (0.01 * h);

        int pt1_x = (int) ((w * coord[0] - w_) > 0 ? (w * coord[0] - w_) : (w * coord[0]));
        int pt1_y = (int) ((h * coord[1] - h_) > 0 ? (h * coord[1] - h_) : (h * coord[1]));


        int pt3_x = (int) ((w * coord[4] + w_) < w ? (w * coord[4] + w_) : (w * coord[4]));
        int pt3_y = (int) ((h * coord[5] + h_) < h ? (h * coord[5] + h_) : (h * coord[5]));

        int new_w = (int) (pt3_x-pt1_x);
        int new_h = (int) (pt3_y-pt1_y);


        if (((pt1_x < m_CameraView.getLeft()+200) || ((pt1_x + new_w) > m_CameraView.getRight()-400)) || ((pt1_y < m_CameraView.getTop()+200) || ((pt1_y + new_h) > m_CameraView.getBottom()-200)) || (new_w < 50)) {
            Log.d("log:: ", "Out of Bound");
        } else {
            Imgproc.rectangle(matInput, new Point(pt1_x, pt1_y), new Point(pt3_x, pt3_y),
                    new Scalar(0, 255, 0), 10);

            Log.d("log:: ", "pt1_x: " + pt1_x + " pt1_y: " + pt1_y + " new_w: " + new_w + " new_h: " + new_h);

            Rect roi = new Rect(pt1_x, pt1_y, new_w, new_h);
            Log.d("log:: ", "x: " + roi.x + " y: " + roi.y + " w: " + roi.width + " h: " + roi.height);
            Log.d("input log:: ", "cols: " + input.cols() + " rows: " + input.rows());
            if (roi.x + roi.width > input.cols() || roi.x < 0 || roi.width < 0 || roi.y + roi.height > input.rows() || roi.y < 0 || roi.height < 0)
                return matInput;
            Mat croppedImage = new Mat(input, roi);
//            Mat toDetImage2 = new Mat();
//            Size sz2 = new Size(128, 128);
//            Imgproc.resize(croppedImage, toDetImage2, sz2);
//            onFrame2 = Bitmap.createBitmap(toDetImage2.cols(), toDetImage2.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(toDetImage2, onFrame2);
            Mat toplateImage = new Mat();
            Size sz1 = new Size(1024, 2048);
            Imgproc.resize(croppedImage, toplateImage, sz1);
            onFrame2 = Bitmap.createBitmap(toplateImage.cols(), toplateImage.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(toplateImage, onFrame2);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            onFrame2.compress(Bitmap.CompressFormat.PNG, 200, baos);
            byte[] bImage = baos.toByteArray();
            onFrame2_base64 = Base64.encodeToString(bImage, 0);

            if (!(onFrame2_base64.equals(before_image))) {
                carPlate_num(onFrame2_base64);
                before_image = onFrame2_base64;
            }

        }


        if ( matResult == null )

            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

//        ConvertRGBtoGray(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());

        return matResult;
    }

    private void carPlate_num (String onFrame2_base64) {
        if (cFlag) {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("version", "V2");
            requestBody.addProperty("requestId", UUID.randomUUID().toString());
            requestBody.addProperty("timestamp", System.currentTimeMillis());

            JsonObject image = new JsonObject();
            image.addProperty("format", "png");
            image.addProperty("name", "carPlate");
            image.addProperty("data", onFrame2_base64);

            JsonArray images = new JsonArray();
            images.add(image);

            requestBody.add("images", images);
            //Log.e("json 파일", String.valueOf(requestBody));

            call = ocrService.doOCR(requestBody);
            cFlag = false;
            Log.e("json 파일", String.valueOf(cFlag));
        } else {
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    String strs="";
                    if (response.isSuccessful()) {
                        JsonObject result = response.body();
                        //Log.e("json 파일", String.valueOf(result));
                        JsonArray imagesArr = result.getAsJsonArray("images");
                        //Log.e("json 파일", String.valueOf(imagesArr));
                        JsonObject firstImageObj = (JsonObject) imagesArr.get(0);
                        //Log.e("json 파일", String.valueOf(firstImageObj));
                        JsonArray fieldsArr = firstImageObj.getAsJsonArray("fields");
                        //Log.e("json 파일", String.valueOf(fieldsArr));
                        for (int i=0; i<fieldsArr.size(); i++){
                            JsonObject job = (JsonObject) fieldsArr.get(i);
                            //Log.e("json 파일", String.valueOf(job));
                            strs = strs + job.get("inferText");
                            //.e("json 파일", String.valueOf(job.get("inferText")));
                        }
                        //Log.e("json 파일", strs);

                        String carPlate_num = strs.replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣0-9]", "");
                        // 탐지한 번호판 입력
                        carNumber.setText(carPlate_num);
                        //Toast.makeText(getApplicationContext(), strs, Toast.LENGTH_LONG).show();
                        //Toast.makeText(getApplicationContext(), carPlate_num, Toast.LENGTH_LONG).show();
                        Log.e("텍스트 인식", "성공");

                    } else {
                        Log.e("텍스트 인식", "실패");
                    }
                    cFlag = true;
                    Log.e("json 파일", String.valueOf(cFlag));
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("전송", "실패: ");
                    cFlag = false;
                    Log.e("json 파일", String.valueOf(cFlag));
                }
            });
        }
    }


    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }


    //여기서부턴 퍼미션 관련 메소드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;


    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        }else{
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder( InputActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }

    // 원래 코드
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (gpsFlag) {
                if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                    lat.setText(Double.toString(location.getLatitude()));
                    lng.setText(Double.toString(location.getLongitude()));
                    sendLat = Double.toString(location.getLatitude());
                    sendLng = Double.toString(location.getLongitude());
                    loading.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };


    private void CreateDialogKind() {
        final String items[] = new String[modelList.size()];

        for (int i=0; i<modelList.size(); i++) {
            items[i] = modelList.get(i);
        }

        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        alt_bld.setTitle(getString(R.string.model));
        alt_bld.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                modelName.setText(items[item]);
                sendModel = items[item];
                alertKind.dismiss();
            }
        });
        alertKind = alt_bld.create();
        alertKind.show();
    }

    private void CreateDialogLegal() {
        final String items[] = new String[legalList.size()];

        for (int i=0; i<legalList.size(); i++) {
            items[i] = legalList.get(i);
        }

        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        alt_bld.setTitle(getString(R.string.legal));
        alt_bld.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                legalName.setText(items[item]);
                sendLegal = items[item];
                alertLegal.dismiss();
            }
        });
        alertLegal = alt_bld.create();
        alertLegal.show();
    }

    private String GetDate() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strNow = sdfNow.format(date);

        return strNow;
    }

    private boolean GetInputCheck() {

        boolean isCarNumberCheck = false;

        if(gpsFlag == false){
            return false;
        }

        if (carNumber.length() == 0) {
            Toast.makeText(this, getString(R.string.error_carnumber), Toast.LENGTH_SHORT).show();
            loading.setVisibility(View.GONE);

            return false;
        } else {
//            isCarNumberCheck = setCarNumberCheck(carNumber.getText().toString());
            isCarNumberCheck = mCheckCarNumber.setCarNumberCheck(carNumber.getText().toString());
            if (!isCarNumberCheck) {           // 자동차 번호 판별
                Toast.makeText(this, getString(R.string.error_inputcarnumber), Toast.LENGTH_SHORT).show();
                loading.setVisibility(View.GONE);
                return false;
            }
        }

        if (sendModel == null || sendModel.equals("선택")) {
            Toast.makeText(this, getString(R.string.error_model), Toast.LENGTH_SHORT).show();
            loading.setVisibility(View.GONE);
            return false;
        } else if (sendLegal == null || sendLegal.equals("선택")) {
            Toast.makeText(this, getString(R.string.error_legal), Toast.LENGTH_SHORT).show();
            loading.setVisibility(View.GONE);
            return false;
        }  else if (sendLat == null) {
            Toast.makeText(this, getString(R.string.error_lat), Toast.LENGTH_SHORT).show();
            loading.setVisibility(View.GONE);
            return false;
        } else if (sendLng == null) {
            Toast.makeText(this, getString(R.string.error_lng), Toast.LENGTH_LONG).show();
            loading.setVisibility(View.GONE);
            return false;
        }

        return true;
    }

    private void keyBoardHidden(){
        imm.hideSoftInputFromWindow(carNumber.getWindowToken(), 0);
    }

    private void getGps(){
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsFlag = true;
            loading.setVisibility(VISIBLE);
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
            gpsFlag = false;
            if(loading.getVisibility() == VISIBLE)
                loading.setVisibility(View.GONE);

            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivity(intent);
        }
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.fl:
                keyBoardHidden();
                break;

            case R.id.model:
            case R.id.modelName:
                keyBoardHidden();
                CreateDialogKind();
                break;
            case R.id.legal:
            case R.id.legalName:
                keyBoardHidden();
                CreateDialogLegal();
                break;
            case R.id.gps:
                keyBoardHidden();
                getGps();
                break;
            case R.id.register:
                getGps();

                if (GetInputCheck()) {
                    loading.setVisibility(VISIBLE);
                    getInfo = new GetInfo("REGISTER");
                    getInfo.execute();
                }
                break;
            case R.id.btnMap:
                Intent intent = new Intent(this, SurveyMap.class);
                intent.putExtra("project_id", sProjectID);
                intent.putExtra("name", sName);
                intent.putExtra("phone", sPhone);
                startActivity(intent);
                break;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (getInfo != null && getInfo.getStatus() != AsyncTask.Status.FINISHED)
                    getInfo.cancel(true);
                break;
        }

        return super.onKeyUp(keyCode, event);
    }

    private class GetInfo extends AsyncTask<String, Integer, String> {

        private Connection conn = null;

        private Statement stmt = null;
        private ResultSet kind = null;
        private ResultSet legal = null;
        private ResultSet count = null;
        private ResultSet dupli = null;
//        private ResultSet save = null;

        private int nSaveResult = 0;

        private String sendName = null;
        private String sendCarNumber = null;
        private String who = null;

        public GetInfo(String who) {
            this.who = who;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            this.sendName = name.getText().toString();
            this.sendCarNumber = carNumber.getText().toString();
        }

        @Override
        protected String doInBackground(String... strings) {
            String sValue = null;
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

            String query = "select count(*) from parking_data where researcher_name = '" + sendName +"' and project_id = '" + sProjectID + "' and researcher_phone = '" + sPhone+ "'";   // 사용자 카운트

            try {
                Class.forName(sqlDriver).newInstance();

                conn = DriverManager.getConnection(sqlConnection, sqlId, pw);

                if(conn != null)
                    stmt = conn.createStatement();

                if(stmt == null){
                    Log.e(TAG, "stmt value null");
                    return sValue ;
                }

                if (who.equals("INFO")) {

                    kind = stmt.executeQuery("select research_item_text from parking_research where research_name = 'car_kind' and project_id = '" + sProjectID + "'");

                    modelList.add("선택");
                    legalList.add("선택");

                    if(kind == null){
                        Log.e(TAG, "kind value null");
                        return sValue;
                    }

                    while (kind.next()) {
                        Log.e(TAG, "reset ----- car kind = " + kind.getString(1));
                        modelList.add(kind.getString(1));
                    }

                    legal = stmt.executeQuery("select research_item_text from parking_research where research_name = 'ox' and project_id = '" + sProjectID + "'");

                    if(legal == null){
                        Log.e(TAG, "legal value null");
                        return sValue;
                    }

                    while (legal.next()) {
                        Log.e(TAG, "reset ----- legal kind = " + legal.getString(1));
                        legalList.add(legal.getString(1));
                    }
                } else if(who.equals("REGISTER")){

                    if(whoSql){                 // MSSQL 정보
//                        String confirmInsertQuery = "select count(*) as row_count from parking_data where car_no = '" + sendCarNumber + "'and researcher_name = '" + sendName +"' and research_date = Convert(DateTime, '" + GetDate() + "', 120)" +
//                                "group by car_no, researcher_name, research_date having count(*) >= 1"; // 최종 등록하기전에 중복 데이터 있는지 확인

//                        String dateTime = "2017-04-14 16:48:39";          // 테스트 하기 위해 넣어 놓은 코드
                        String confirmInsertQuery = "select count(*) as row_count from parking_data where project_id = '" + sProjectID + "' and car_no = '" + sendCarNumber + "'and researcher_name = '" + sendName +"' and research_date = Convert(DateTime, '" + GetDate() + "', 120)" +
                                "group by car_no, researcher_name, research_date having count(*) >= 1"; // 최종 등록하기전에 중복 데이터 있는지 확인

//                        Log.e(TAG, "쿼리문 : " + confirmInsertQuery);

                        dupli = stmt.executeQuery(confirmInsertQuery);

                        int dupli_count = 0;

                        if(dupli != null){
                            while(dupli.next()){
                                dupli_count = dupli.getInt(1);
                                Log.e(TAG, "중복 카운드 개수:" + dupli_count);
                            }
                        }

                        if(dupli_count >= 1){
                            sValue = InputErrorDuplicate;
                            return sValue;
                        }

                        nSaveResult = stmt.executeUpdate("insert into parking_data (research_date, project_id, researcher_name, " +
                                "researcher_phone, car_kind, parking_ox, car_no, longitude, latitude)" +
                                "values(Convert(DateTime, '" + GetDate() + "', 120), '"+ sProjectID + "', " +
                                " '" + sendName + "', '" + sPhone + "', '" + sendModel + "', '" + sendLegal + "', '" + sendCarNumber + "', " +
                                "'" + sendLng + "', '" + sendLat + "')");

                    } else{                     // 오라클 정보
                        String date_expFormat = "YYYY-MM-DD HH24:MI:SS";

                        nSaveResult = stmt.executeUpdate("insert into parking_data (research_date, project_id, researcher_name, " +
                                "researcher_phone, car_kind, parking_ox, car_no, longitude, latitude)" +
                                "values(TO_DATE('" + GetDate() + "', ' " + date_expFormat +"'), '"+ sProjectID + "', " +
                                "'" + sendName + "', '" + sPhone + "', '" + sendModel + "', '" + sendLegal + "', '" + sendCarNumber + "', " +
                                "'" + sendLng + "', '" + sendLat + "')");
                    }

                    if(nSaveResult == 1){
                        isNet = true;
                        sValue = NETWORK_SEND_SUCCESS;


                    } else{
                        isNet = false;
                        sValue = NETWORK_SEND_FAIL;
                    }

                    Log.e(TAG, "업데이트 결과 : " + nSaveResult);
                }

                count = stmt.executeQuery(query);

                while (count.next()) {
                    Log.e(TAG, "조회 ----- count = " + count.getString(1));
                    search_count = count.getString(1);
                }

            } catch (SQLException e) {
                Log.e(TAG, "연결 : " + e.getErrorCode());
                if(e.getErrorCode() == 0){
                    isNet = false;
                }

            } catch (Exception e){
                e.printStackTrace();
            }finally {
                try {
                    if (kind != null)
                        kind.close();

                    if (legal != null)
                        legal.close();

                    if (stmt != null)
                        stmt.close();

                    if (conn != null)
                        conn.close();

                    if(count != null)
                        count.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            return sValue;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (who.equals("REGISTER")) {

                if(s != null) {
                    if (s.equals(InputErrorDuplicate)) {
                        Toast.makeText(InputActivity.this, "현재 위치에 중복된 차량정보가 있습니다.", Toast.LENGTH_SHORT).show();

                        loading.setVisibility(View.GONE);
                        return;
                    }else if(s.equals(NETWORK_SEND_FAIL) && !isNet){
                        Toast.makeText(InputActivity.this, "데이터베이스 연결이 되지 않았습니다. 네트워크를 확인하십시오", Toast.LENGTH_SHORT).show();
                    }else if(s.equals(NETWORK_SEND_SUCCESS) && isNet){
                        carNumber.setText("");
                        sendCarNumber = null;

                        lat.setText(sendLat);
                        lng.setText(sendLng);

                        Toast.makeText(InputActivity.this, "전송하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else{
                    if(!isNet){
                        Toast.makeText(InputActivity.this, "데이터베이스 연결이 되지 않았습니다. 네트워크를 확인하십시오", Toast.LENGTH_SHORT).show();
                    }
                }

            } else {        // who == INFO

                modelName.setText(modelList.get(0));
                legalName.setText(legalList.get(0));
            }

            check_count.setText(search_count);

            loading.setVisibility(View.GONE);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            try {
                if (kind != null)
                    kind.close();

                if (legal != null)
                    legal.close();

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
