package com.example.backgroundstt;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.service.autofill.UserData;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telecom.Call;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.backgroundstt.MainActivity;
import com.example.backgroundstt.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Logger;

import static android.speech.SpeechRecognizer.ERROR_AUDIO;
import static android.speech.SpeechRecognizer.ERROR_CLIENT;
import static android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS;
import static android.speech.SpeechRecognizer.ERROR_NETWORK;
import static android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT;
import static android.speech.SpeechRecognizer.ERROR_NO_MATCH;
import static android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY;
import static android.speech.SpeechRecognizer.ERROR_SERVER;
import static android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
import static com.example.backgroundstt.App.CHANEL_ID;

public class Recognition extends RecognitionService {

    public static final int MSG_VOICE_RECO_READY = 0;
    public static final int MSG_VOICE_RECO_END = 1;
    public static final int MSG_VOICE_RECO_RESTART = 2;
    private SpeechRecognizer mSrRecognizer;
    boolean mBoolVoiceRecoStarted;
    protected AudioManager mAudioManager;
    Intent itIntent;//음성인식 Intent
    boolean end = false;

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("여기 오는가?");
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (SpeechRecognizer.isRecognitionAvailable(getApplicationContext())) { //시스템에서 음성인식 서비스 실행이 가능하다면
            itIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            itIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
            itIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN.toString());
            itIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
            itIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
            itIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            startListening();
        }
    }

    private Handler mHdrVoiceRecoState = new Handler() {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            Log.d("호출순서 : ","2번");
            switch (msg.what) {
                case MSG_VOICE_RECO_READY:
                    break;
                case MSG_VOICE_RECO_END: {
                    stopListening();
                    sendEmptyMessageDelayed(MSG_VOICE_RECO_RESTART, 1000);
                    break;
                }
                case MSG_VOICE_RECO_RESTART:
                    startListening();
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("호출순서 : ","3번");
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this
                , 0, notificationIntent, 0); //알람을 눌렀을 때 해당 엑티비티로

        Notification notification = new NotificationCompat.Builder(this, CHANEL_ID)
                .setContentTitle("STTModule")
                .setContentText("음성인식중...")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("호출순서 : ","9번");
        end = true;
        mSrRecognizer.destroy();
        mHdrVoiceRecoState.sendEmptyMessage(MSG_VOICE_RECO_READY); //음성인식 서비스 다시 시작
        if (mAudioManager != null)
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
    }

    @Override
    protected void onStartListening(Intent recognizerIntent, Callback listener) {
        Log.d("호출순서 : ","11번");
    }

    public void startListening() {
        Log.d("호출순서 : ","2번");
        if(!end){
            //음성인식을 시작하기 위해 Mute
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!mAudioManager.isStreamMute(AudioManager.STREAM_MUSIC)) {
                    mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                }
            } else {
                mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            }
            if (!mBoolVoiceRecoStarted) { // 최초의 실행이거나 인식이 종료된 후에 다시 인식을 시작하려 할 때
                if (mSrRecognizer == null) {
                    mSrRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
                    mSrRecognizer.setRecognitionListener(mClsRecoListener);
                }
                mSrRecognizer.startListening(itIntent);
            }
            mBoolVoiceRecoStarted = true;  //음성인식 서비스 실행 중
        }
        // 이 코드는 음성인식이 성공적으로 입력이 됬을 때 음성인식을 다시 시작하는 코드이다.
        // 스노우 보이가 되면 이 코드는 필요 없다.
        mSrRecognizer.startListening(itIntent);
    }

    public void stopListening() //Override 함수가 아닌 한번만 호출되는 함수 음성인식이 중단될 때
    {
        Log.d("호출순서 : ","6번");
        try {
            if (mSrRecognizer != null && mBoolVoiceRecoStarted) {
                mSrRecognizer.stopListening(); //음성인식 Override 중단을 호출
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        mBoolVoiceRecoStarted = false;  //음성인식 종료
    }


    @Override
    protected void onCancel(Callback listener) {
        Log.d("호출순서 : ","7번");
        mSrRecognizer.cancel();
    }

    @Override
    protected void onStopListening(Callback listener) { //음성인식 Override 함수의 종료부분
        Log.d("호출순서 : ","8번");
        mHdrVoiceRecoState.sendEmptyMessage(MSG_VOICE_RECO_RESTART); //음성인식 서비스 다시 시작
    }

    private RecognitionListener mClsRecoListener = new RecognitionListener() {
        @Override
        public void onRmsChanged(float rmsdB) {
            // 대시벨이 증가하거나 낮아질때 호출 됨
            Log.d("호출순서 : ","5번");
        }

        @Override
        public void onResults(Bundle results) {
            Log.d("호출순서 : ","10번");
            //Recognizer KEY를 사용하여 인식한 결과값을 가져오는 코드
            String key = "";
            key = SpeechRecognizer.RESULTS_RECOGNITION;
            ArrayList<String> mResult = results.getStringArrayList(key);
            final String[] rs = new String[mResult.size()];
            mResult.toArray(rs);
            Log.d("key", Arrays.toString(rs));
            startListening();
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d("호출순서 : ","4번");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d("호출순서 : ","12번");
        }

        @Override
        public void onError(int intError) {
            String message = "";
            switch (intError) {

                case ERROR_NETWORK_TIMEOUT:
                    //네트워크 타임아웃
                    message = "네트워크 타임아웃";
                    break;
                case ERROR_NETWORK:
                    message ="네트워크 에러";
                    break;
                case ERROR_AUDIO:
                    //녹음 에러
                    message ="녹음 에러";
                    break;
                case ERROR_SERVER:
                    //서버에서 에러를 보냄
                    message = "서버에서 에러를 보냄";
                    break;
                case ERROR_CLIENT:
                    //클라이언트 에러
                    message = "클라이언트 에러";
                    break;
                case ERROR_SPEECH_TIMEOUT:
                    //아무 음성도 듣지 못했을 때
                    message = "아무 음성도 듣지 못함";
                    mHdrVoiceRecoState.sendEmptyMessage(MSG_VOICE_RECO_END);
                    break;
                case ERROR_NO_MATCH:
                    //적당한 결과를 찾지 못했을 때
                    message = "적당한 결과를 찾이 못함";
                    mHdrVoiceRecoState.sendEmptyMessage(MSG_VOICE_RECO_END);
                    break;
                case ERROR_RECOGNIZER_BUSY:
                    //RecognitionService가 바쁠 때
                    message = "RecognitionService가 바쁨";
                    break;
                case ERROR_INSUFFICIENT_PERMISSIONS:
                    //uses - permission(즉 RECORD_AUDIO) 이 없을 때
                    message = "permission이 없음";
                    break;
            }
            Log.e("에러가 발상했습니다 : ", message);
        }
        @Override
        public void onBeginningOfSpeech() {
            Log.d("호출순서 : ","13번");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.d("호출순서 : ","14번");
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            Log.d("호출순서 : ","15번");
        }

        @Override
        public void onPartialResults(Bundle partialResults) { //부분 인식을 성공 했을 때
            Log.d("호출순서 : ","16번");
        }
    };
}
