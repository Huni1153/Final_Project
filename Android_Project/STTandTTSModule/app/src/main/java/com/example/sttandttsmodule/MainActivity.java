package com.example.sttandttsmodule;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener
{
    Intent intent;
    SpeechRecognizer mRecognizer;
    Button sttBtn;
    TextView textView;
    final int PERMISSION = 1;

    private TextToSpeech tts;
    private Button btn_Speak;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if ( Build.VERSION.SDK_INT >= 23 )
        { // 퍼미션 체크
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO},PERMISSION);
        }
        // xml의 버튼과 텍스트 뷰 연결
        textView = (TextView)findViewById(R.id.sttResult);
        sttBtn = (Button) findViewById(R.id.sttStart);

        // RecognizerIntent 객체 생성
        intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");
        Log.i("sd","이거 타입 뭐냐 : " + this.getClass().getName());
        // 버튼을 클릭 이벤트 - 객체에 Context와 listener를 할당한 후 실행
        sttBtn.setOnClickListener(v ->
        {
            mRecognizer=SpeechRecognizer.createSpeechRecognizer(this);
            //Log.i("sd","이거 타입 뭐냐 : " + this.getClass().getName());
            mRecognizer.setRecognitionListener(listener);
            mRecognizer.startListening(intent);
        });

        // tts 모듈
        tts = new TextToSpeech(this, this);
        btn_Speak = findViewById(R.id.ttsStart);
        //txtText = findViewById(R.id.sttResult);

        btn_Speak.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v)
            {
                speakOut();
            }
        });
    }

    // RecognizerIntent 객체에 할당할 listener 생성
    private RecognitionListener listener = new RecognitionListener()
    {
        @Override
        public void onReadyForSpeech(Bundle params)
        {
            Toast.makeText(getApplicationContext(),"음성인식을 시작합니다.",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {}

        @Override
        public void onError(int error)
        {
            String message;
            switch (error)
            {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER가 바쁨";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }
            Toast.makeText(getApplicationContext(), "에러가 발생하였습니다. : " + message,Toast.LENGTH_SHORT).show();
        }

        @Override public void onResults(Bundle results)
        {
            // 말을 하면 ArrayList에 단어를 넣고 textView에 단어를 이어줍니다.
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for(int i = 0; i < matches.size() ; i++)
            {
                textView.setText(matches.get(i));
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {}

        @Override
        public void onEvent(int eventType, Bundle params) {}
    };


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void speakOut()
    {
        CharSequence text = textView.getText();
        tts.setPitch((float) 1.0);
        tts.setSpeechRate((float) 1.0);
        tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"id1");
    }
    @Override
    public void onDestroy()
    {
        if (tts != null)
        {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)

    @Override
    public void onInit(int status)
    {

        if (status == TextToSpeech.SUCCESS)
        {
            int result = tts.setLanguage(Locale.KOREA);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
            {
                Log.e("TTS", "This Language is not supported");
            }
            else
            {
                btn_Speak.setEnabled(true);
                speakOut();
            }
        }
        else
        {
            Log.e("TTS", "Initilization Failed!");
        }
    }
}