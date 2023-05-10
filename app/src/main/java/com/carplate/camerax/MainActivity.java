package com.carplate.camerax;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import com.google.android.gms.location.LocationRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import org.tensorflow.lite.nnapi.NnApiDelegate;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.Manifest.permission.CAMERA;

import com.example.myapplication.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
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

public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {



    private int m_Camidx = 0;//front : 1, back : 0
    private CameraBridgeViewBase m_CameraView;

    private static final int CAMERA_PERMISSION_CODE = 200;


    private Button button; // 카메라 캡쳐버튼
    private Button fileButton;
    private Button testButton;
    private Boolean activationButton; // 캡쳐버튼 활성 유무
    private ImageView imageView; // 시각화
    private Mat matInput;
    public TextView textView, tvTime, tvNowTime; // textView: 번호판 tvTime: 추론시간
    public TextView tvLat, tvLong;
    public TextView tvTest, tvSearch;

    public NnApiDelegate nnApiDelegate = null;
    GpuDelegate delegate = new GpuDelegate();

    public Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);
    public Interpreter.Options options1 = (new Interpreter.Options()).setNumThreads(4);


    long start,end; // 전체 추론시간
    boolean cFlag=true;
    Call<JsonObject> call;
    long[] inferenceTime = new long[3]; // 모델별 추론시간

    private Bitmap onFrame; // yolo input
    private Bitmap onFrame2; // alignment input
    private Bitmap onFrame3; // char input
    private Bitmap onFrame4;
    String onFrame4_base64;
    String infer_result = "";

    // 모델 정의
    DHDetectionModel detectionModel;
    AlignmentModel alignmentModel;

    public interface OCRService {
        @Headers({
                "Content-Type: application/json; charset=utf-8",
                "X-OCR-SECRET: QmlsQVJod3l1RlBEeWtoRmNFQnBXeHNYd2hBalVCYWQ= "
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

    // 현재 시간
    long mNow;
    Date mDate;
    SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    ArrayList<String> list = new ArrayList<>();

    // gps
    private FusedLocationProviderClient mFusedLocationProviderClient = null;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private static final int REQUEST_PERMISSION_LOCATION = 10;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // test
    String carNum;
    DBHelper dbHelper = new DBHelper(MainActivity.this, 1);

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    // 권한을 모두 가지고 있는지 확인
    private boolean hasPermissions() {
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // 권한 요청 대화상자 표시
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean isAllGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }
            if (isAllGranted) {
                // 권한이 모두 승인되었을 때 실행할 코드
            } else {
                // 권한이 거부되었을 때 처리할 코드
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Permission::","onCreate");

        if (!hasPermissions()) {
            requestPermissions();
        }

        // gps
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setMaxWaitTime(2000);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        // 화면 구성요소
        m_CameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        activationButton = false;
        button = (Button) findViewById(R.id.button_capture);
        fileButton = (Button) findViewById(R.id.button_file);
        testButton = (Button) findViewById(R.id.button_test);
        textView = (TextView) findViewById(R.id.textView);
        tvLat = (TextView) findViewById(R.id.tvLat);
        tvLong = (TextView) findViewById(R.id.tvLong);
        imageView = (ImageView) findViewById(R.id.imageView);
        tvTime = (TextView) findViewById(R.id.tvTime);
        tvNowTime = (TextView) findViewById(R.id.tvNowTime);
        tvTest = (TextView) findViewById(R.id.test_tv);
        tvSearch = (TextView) findViewById(R.id.search_tv);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!activationButton) {
                    activationButton = true;
                    m_CameraView.enableView();
                    button.setText("중지");

                } else {
                    activationButton = false;
                    m_CameraView.disableView();
                    button.setText("시작");
                    stoplocationUpdates();
                }
            }
        });

        fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeFile(list);
            }
        });

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("test", "testButton");
                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setTitle("Test");
                alert.setMessage("DB에 저장할 번호판을 작성하세요");

                final EditText et = new EditText(MainActivity.this);
                alert.setView(et);

                alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        carNum = et.getText().toString();
                        dbHelper.insert(carNum);
                        Toast.makeText(MainActivity.this, "success", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });

                alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int wghich) {
                        dialog.dismiss();
                    }
                });

                alert.show();
            }
        });

        m_CameraView.setVisibility(SurfaceView.VISIBLE);
        m_CameraView.setCvCameraViewListener(this);
        m_CameraView.setCameraIndex(m_Camidx);

        try{

            detectionModel = new DHDetectionModel(this, options);
            //Toast.makeText(this.getApplicationContext(), options.toString(), Toast.LENGTH_LONG).show();
            alignmentModel = new AlignmentModel(this, options1);

        }
        catch(IOException e){
            e.printStackTrace();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean _Permission = false; //변수 추가
        Log.d("Permission::","onStart");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){//최소 버전보다 버전이 높은지 확인

            if(checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_CODE);
                _Permission = true;
            }
            else if(checkSelfPermission(CAMERA)== PackageManager.PERMISSION_GRANTED){
                _Permission = true;
            }
        }

        if(_Permission){
            //여기서 카메라뷰 받아옴
            Log.d("Permission::","Permission true, onCameraPermission으로 감");
            onCameraPermissionGranted();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d("Permission::","onResume");
        if (OpenCVLoader.initDebug()) {
            m_LoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Log.d("Permission::","onPause");
        if (m_CameraView != null)
            m_CameraView.disableView();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (m_CameraView != null)
            m_CameraView.disableView();
    }

    private BaseLoaderCallback m_LoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    m_CameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    protected void onCameraPermissionGranted() {
        Log.d("Permission::","onCameraPermissionGranted 함수");
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

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(m_CameraView);
    }

    private void setFullScreen(){
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();

    }
    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @SuppressLint("LongLogTag")
    @Override

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        start = System.currentTimeMillis();
        matInput = inputFrame.rgba();
        Mat input = matInput.clone();
        Log.d("input log:: ","cols: "+input.cols()+" rows: "+input.rows());

        if(activationButton){

            Mat toDetImage = new Mat();
            Size sz = new Size(256, 192);
            Imgproc.resize(matInput, toDetImage, sz);
            onFrame = Bitmap.createBitmap(toDetImage.cols(), toDetImage.rows(), Bitmap.Config.ARGB_8888);

            Utils.matToBitmap(toDetImage, onFrame);
            long yolo_s = System.currentTimeMillis();
            float[][] proposal = detectionModel.getProposal(onFrame, input);
            long yolo_e = System.currentTimeMillis();
            inferenceTime[0] = yolo_e-yolo_s;


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


            //Imgproc.rectangle(matInput, new Point(m_CameraView.getLeft()+200, m_CameraView.getTop()+200), new Point(m_CameraView.getRight()-400, m_CameraView.getBottom()-200),new Scalar(0, 255, 0), 10);

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
                Mat toDetImage2 = new Mat();
                Size sz2 = new Size(128, 128);
                Imgproc.resize(croppedImage, toDetImage2, sz2);
                onFrame2 = Bitmap.createBitmap(toDetImage2.cols(), toDetImage2.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(toDetImage2, onFrame2);
                Mat toplateImage = new Mat();
                Size sz3 = new Size(256, 128);
                Imgproc.resize(croppedImage, toplateImage, sz3);
                onFrame4 = Bitmap.createBitmap(toplateImage.cols(), toplateImage.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(toplateImage, onFrame4);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                onFrame4.compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] bImage = baos.toByteArray();
                onFrame4_base64 = Base64.encodeToString(bImage, 0);

                long align_s = System.currentTimeMillis();
                float[] coord2 = alignmentModel.getCoordinate(onFrame2);
                long align_e = System.currentTimeMillis();
                inferenceTime[1] = align_e - align_s;
                float[] new_coord2 = new float[8];

                // sigmoid
                for (int i = 0; i < 8; i++) {
                    new_coord2[i] = (float) (Math.exp(-(coord2[i])) + 1);
                    new_coord2[i] = 1 / new_coord2[i];
                    new_coord2[i] = new_coord2[i] * 128;
                }

                // perspective transformation
                Mat outputImage = new Mat(256, 128, CvType.CV_8UC3);
                List<Point> src_pnt = new ArrayList<Point>();

                Point p0 = new Point(new_coord2[0], new_coord2[1]);
                Point p1 = new Point(new_coord2[2], new_coord2[3]);
                Point p2 = new Point(new_coord2[4], new_coord2[5]);
                Point p3 = new Point(new_coord2[6], new_coord2[7]);

                src_pnt.add(p0);
                src_pnt.add(p1);
                src_pnt.add(p2);
                src_pnt.add(p3);

                Mat startM = Converters.vector_Point2f_to_Mat(src_pnt);
                List<Point> dst_pnt = new ArrayList<Point>();
                Point p4 = new Point(0, 0);
                Point p5 = new Point(255, 0);
                Point p6 = new Point(255, 127);
                Point p7 = new Point(0, 127);
                dst_pnt.add(p4);
                dst_pnt.add(p5);
                dst_pnt.add(p6);
                dst_pnt.add(p7);
                Mat endM = Converters.vector_Point2f_to_Mat(dst_pnt);
                Mat M = Imgproc.getPerspectiveTransform(startM, endM);
                Size size2 = new Size(256, 128);
                Imgproc.warpPerspective(toDetImage2, outputImage, M, size2, Imgproc.INTER_CUBIC + Imgproc.CV_WARP_FILL_OUTLIERS);
                onFrame3 = Bitmap.createBitmap(outputImage.cols(), outputImage.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(outputImage, onFrame3);

                // char prediction
                long char_s = System.currentTimeMillis();
                //String result = charModel.getString(onFrame3);
                //String result2 = result.substring(0, 3) + " " + result.substring(3);
                long char_e = System.currentTimeMillis();
                inferenceTime[2] = char_e - char_s;
                end = System.currentTimeMillis();
                double fps = Math.round(((1.0 / (end - start)) * 1000 * 100.0)) / 100.0;
                infer_result = fps + "  fps";
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // gps
                        startLocationUpdates();

                        tvTime.setText(infer_result);
                        imageView.setImageBitmap(onFrame4);
                        carPlate_num(onFrame4_base64);

                        carNum = textView.getText().toString();
                        if(dbHelper.getResult(carNum)) {
//                            Toast.makeText(MainActivity.this, "exist", Toast.LENGTH_SHORT).show();
                            tvSearch.setText("관내차량입니다.");
                        }else{
//                            Toast.makeText(MainActivity.this, "no exist", Toast.LENGTH_SHORT).show();
                            tvSearch.setText("관내차량이 아닙니다.");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        else{

            m_CameraView.enableView();
        }
        return matInput;
    }

    private String getTime(){
        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        return mFormat.format(mDate);
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // 권한이 거부되었을 경우 사용자에게 권한에 대한 이유를 설명합니다.
            new AlertDialog.Builder(this)
                    .setTitle("위치 권한 필요")
                    .setMessage("이 앱을 사용하려면 위치 권한이 필요합니다.")
                    .setPositiveButton("확인", (dialogInterface, i) -> {
                        // 권한 요청 팝업을 표시합니다.
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                    })
                    .create()
                    .show();
        } else {
            // 권한 요청 팝업을 표시합니다.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    protected void startLocationUpdates() {
        Log.d("TAG", "startLocationUpdates()");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("TAG", "startLocationUpdates() 두 위치 권한중 하나라도 없는 경우 ");
            return;
        }
        Log.d("TAG", "startLocationUpdates() 위치 권한이 하나라도 존재하는 경우");
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Log.d("TAG", "onLocationResult()");
            locationResult.getLastLocation();
            onLocationChanged(locationResult.getLastLocation());
        }
    };

    public void onLocationChanged(Location location) {
        Log.d("TAG", "onLocationChanged()");
        mLastLocation = location;
        tvNowTime.setText(getTime());
        tvLat.setText("위도 : " + mLastLocation.getLatitude());
        tvLong.setText("경도 : " + mLastLocation.getLongitude());
        list.add(tvNowTime.getText() + ", " + textView.getText() + ", "
                + mLastLocation.getLatitude() + ", " + mLastLocation.getLongitude() + ", " + tvSearch.getText());
    }

    private void stoplocationUpdates() {
        Log.d("TAG", "stoplocationUpdates()");
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

    public void writeFile(ArrayList<String> list) {

        // 파일 저장 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없을 경우 권한 요청 팝업 띄우기
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            // 권한이 있을 경우 파일 쓰기 시작
            // 외부 저장소(External Storage)가 마운트(인식) 되었을 때 동작
            // getExternalStorageState() 함수를 통해 외부 저장장치가 Mount 되어 있는지를 확인
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                // 다운로드 폴더에 "tagging.txt" 이름으로 txt 파일 저장
                // Environment.DIRECTORY_DOWNLOADS - 기기의 기본 다운로드 폴더
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), "gps" + ".txt");
                try {
                    FileWriter fw = new FileWriter(file, false);
                    for (int i = 0; i < list.size(); i++) {
                        fw.write(list.get(i));
                        fw.write(System.getProperty("line.separator"));
                    }
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "ERROR", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "ERROR", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void carPlate_num (String onFrame4) {
        if (cFlag) {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("version", "V2");
            requestBody.addProperty("requestId", UUID.randomUUID().toString());
            requestBody.addProperty("timestamp", System.currentTimeMillis());

            JsonObject image = new JsonObject();
            image.addProperty("format", "png");
            image.addProperty("name", "carPlate");
            image.addProperty("data", onFrame4);

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
                        Pattern pattern = Pattern.compile("[ㄱ-ㅎㅏ-ㅣ가-힣]");
                        Matcher matcher = pattern.matcher(carPlate_num);

                        if (matcher.find()) {
                            System.out.println("한글이 있는 인덱스: " + matcher.start());
                            System.out.println(carPlate_num.length());
                            carPlate_num = carPlate_num.substring(0, matcher.start() + 5).replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣0-9]", "");
                        } else {
                            System.out.println("문자열에 한글이 없습니다.");
                        }

                        textView.setText(carPlate_num);
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
}