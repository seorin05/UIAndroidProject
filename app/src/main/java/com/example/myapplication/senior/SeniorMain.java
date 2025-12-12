package com.example.myapplication.senior;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech; // TTS 기능
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class SeniorMain extends AppCompatActivity {

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_senior_main);

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // 기존 버튼들
        Button todoButton = findViewById(R.id.btn_todo);
        Button scheButton = findViewById(R.id.btn_schedule);
        Button qnaButton = findViewById(R.id.btn_qa);
        Button callButton = findViewById(R.id.btn_call);

        MaterialButton stopVoiceButton = findViewById(R.id.VoulumOnBtn);

        Button listenAgainButton = findViewById(R.id.btn_listen_again);


        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.KOREAN);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "한국어를 지원하지 않는 기기입니다.");
                    } else {
                        speakGuide();
                    }
                } else {
                    Log.e("TTS", "TTS 초기화 실패");
                }
            }
        });

        stopVoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTTS();
            }
        });

        listenAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speakGuide();
            }
        });

        todoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTTS();
                Intent intent = new Intent(getApplicationContext(), SeniorTodo.class);
                startActivity(intent);
            }
        });

        scheButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTTS();
                Intent intent = new Intent(getApplicationContext(), ScheduleMainActivity.class);
                startActivity(intent);
            }
        });

        qnaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTTS();
                Intent intent = new Intent(getApplicationContext(), Qna_main.class);
                startActivity(intent);
            }
        });

        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTTS();
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:"));
                startActivity(intent);
            }
        });
    }

    private void speakGuide() {
        if (tts != null) {
            String text = "반갑습니다. 원하시는 기능을 선택해주세요. " +
                    "일정을 확인하시려면 일정 버튼을, " +
                    "할 일을 보시려면 할 일 버튼을 눌러주세요. " +
                    "문답을 하시려면 문답 버튼을, " +
                    "위급한 상황에는 긴급 통화 버튼을 누르시면 됩니다. " +
                    "설명을 다시 듣고 싶으시면, 맨 아래의 안내 다시 듣기 버튼을 눌러주세요.";

            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GuideID");
        }
    }

    private void stopTTS() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTTS();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}