package parkingsurvey.neotsys.com;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by hyunchul on 2017-03-31.
 */

public class CheckCarNumber {
    private final String TAG = "CheckCarNumber";

    private Context mContext = null;

    public CheckCarNumber(Context mContext){
        this.mContext = mContext;
    }

    public boolean setCarNumberCheck(String sCarNumber){

        String sZone[] = {"서울", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "인천", "대전", "대구", "광주", "부산", "제주", "울산"};
        String sJamo[] = {"가", "나", "다", "라", "마", "거", "너", "더", "러", "머", "버", "서", "어", "저", "고",
                "노", "도", "로", "모", "보", "소", "오", "조", "구", "누", "두", "루", "무", "부", "수",
                "우", "주", "바", "사", "아", "자", "하", "허", "호", "배", "-"};

        boolean bConfrim = false;
        boolean bZone = false, bNumeric = false, bJamo = false;
        int nCarNumber = sCarNumber.length();

        // 자동차 번호판의 자리수에 대해 문자열을 자를 위치를 정하는 변수
        int zS = 0, zE = 0;
        int nuS = 0, nuE = 0;
        int jaS = 0, jaE = 0;
        int numS = 0, numE = 0;

        String sTempZone = null;

        //[ example
        // 충남12가1234 (9자리)
        // 경북62부2772 (9자리)
        // 대구1가1234  (8자리)
        // 56오5143  (7자리)
        // 123가1234 (8자리) --> 2019년 3자리 숫자 추가로 남구청 주차조사부터 적용함.
        //]

        // 신규 숫자 3자리 추가로 인해 처음 첫 글자가 숫자인지 확인한다. 191118_남구청 주차조사로 인한 코드 추가
        // [8자리는 아래구문 if 구문으로 처리한다. ]
        // [1. 차량번호 전체가 8자리일 경우, 처음 3자리가 숫자이면 신규로 추가된 차량번호로 인지 ]
        // [2. 처음 3자리가 숫자가 아니면 기존 8자리(대구1가1234) 차량번호 인지하는 루틴으로 처리한다.]
        boolean bCarNumberOnlyNumeric = false; // 8자리일경우에만 사용할거임
        if(nCarNumber == 8){
            String tmp_zone = sCarNumber.substring(0, 3);

            if(isNumeric(tmp_zone)){
                nuS = 0;
                nuE = 3;
                jaS = 3;
                jaE = 4;
                numS = 4;
                numE = 8;
                bCarNumberOnlyNumeric = true;
            }
            else{
                zS = 0;
                zE = 2;
                nuS = 2;
                nuE = 3;
                jaS = 3;
                jaE = 4;
                numS = 4;
                numE = 8;
                bCarNumberOnlyNumeric = false;
            }
        }
        else if(nCarNumber == 9){
            zS = 0;
            zE = 2;
            nuS = 2;
            nuE = 4;
            jaS = 4;
            jaE = 5;
            numS = 5;
            numE = 9;
            bCarNumberOnlyNumeric = false;
        }
        else if(nCarNumber == 7){
            nuS = 0;
            nuE = 2;
            jaS = 2;
            jaE = 3;
            numS = 3;
            numE = 7;
            bCarNumberOnlyNumeric = true;
        }

        if( (nCarNumber == 9 || nCarNumber == 8) && !bCarNumberOnlyNumeric ){
            sTempZone = sCarNumber.substring(zS, zE);

            // 1차 지역 조사
            for(int i = 0; i<sZone.length; i++){

                if(sZone[i].equals(sTempZone)){
//                    Log.e(TAG, "첫번재 지역 값 일치");
                    bZone = true;           // 지역 조사가 잘 되었는지 확인
                    break;
                }
            }

            if(!bZone){
//                Toast.makeText(this, getString(R.string.error_zone), Toast.LENGTH_SHORT).show();

                return bConfrim;
            }

            // 2차 가운데 숫자 조사
            sTempZone = sCarNumber.substring(nuS, nuE);
//            Log.e(TAG, "sTempZone: " + sTempZone);
            bNumeric = isNumeric(sTempZone);
            if(!bNumeric){
//                Toast.makeText(this, getString(R.string.error_inputcarnumber), Toast.LENGTH_SHORT).show();
                bConfrim = bNumeric;

                return bConfrim;
            }

            Log.e(TAG, "sTempZone: " + sTempZone + ", bNumeric: " + bNumeric);

            // 3차 자음 조사
            sTempZone = sCarNumber.substring(jaS, jaE);
//            Log.e(TAG, "sTempZone: " + sTempZone);
            for(int j=0; j<sJamo.length; j++){
                if(sJamo[j].equals(sTempZone)){
                    bJamo = true;
                    break;
                }
            }

            if(!bJamo){
//                Toast.makeText(this, getString(R.string.error_inputcarnumber), Toast.LENGTH_SHORT).show();

                bConfrim = bJamo;
                return bConfrim;
            }

            // 4차 숫자
            sTempZone = sCarNumber.substring(numS, numE);
//            Log.e(TAG, "sTempZone: " + sTempZone);

            bNumeric = isNumeric(sTempZone);

            if(!bNumeric){
                Toast.makeText(mContext, mContext.getString(R.string.error_inputcarnumber), Toast.LENGTH_SHORT).show();
                bConfrim = bNumeric;

                return bConfrim;
            }

            bConfrim = true;                // 최종 완료(8 ~ 9자리)

        } else if( (nCarNumber == 7 || nCarNumber == 8) && bCarNumberOnlyNumeric){         //
            // 1차 처음 숫자 조사
            sTempZone = sCarNumber.substring(nuS, nuE);
            bNumeric = isNumeric(sTempZone);
            if(!bNumeric){
                bConfrim = bNumeric;

                return bConfrim;
            }

            Log.e(TAG, "sTempZone: " + sTempZone + ", bNumeric: " + bNumeric);

            // 2차 자음 조사
            sTempZone = sCarNumber.substring(jaS, jaE);

            for(int j=0; j<sJamo.length; j++){
                if(sJamo[j].equals(sTempZone)){
                    bJamo = true;
                    break;
                }
            }

            if(!bJamo){

                bConfrim = bJamo;
                return bConfrim;
            }

            // 3차 숫자 조사
            sTempZone = sCarNumber.substring(numS, numE);

            bNumeric = isNumeric(sTempZone);
            if(!bNumeric){
                bConfrim = bNumeric;

                return bConfrim;
            }

            bConfrim = true;                // 최종 완료(7자리 or 8자리)
        }


        return bConfrim;
    }

    private boolean isNumeric(String s){
        return s.matches("[-+]?\\d*\\.?\\d+");
    }
}
