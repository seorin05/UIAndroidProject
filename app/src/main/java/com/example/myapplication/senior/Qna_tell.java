package com.example.myapplication.senior;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Qna_tell extends AppCompatActivity {

    private static final String TAG = "QnaTellActivity";
    private static final int SPEECH_REQUEST_CODE = 100;

    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    private boolean isSpeaking = false;
    private boolean isTtsEnabled = true;

    private DatabaseReference dbRef;

    // UI
    private MaterialButton btnVoiceInput;
    private MaterialButton volumeOnBtn;
    private TextView statusText;
    private TextView questionText;

    private String recognizedText = "";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qna_tell);

        dbRef = FirebaseDatabase.getInstance().getReference();

        // 뷰 초기화
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        volumeOnBtn = findViewById(R.id.volumeOnBtn);
        statusText = findViewById(R.id.statusText);
        questionText = findViewById(R.id.question);

// questionTextView에 Main에서 전달된 질문 띄우기
        String questionFromMain = getIntent().getStringExtra("questionText");
        if (questionFromMain != null) {
            questionText.setText(questionFromMain);
        }
        // 초기 상태 UI
        isTtsEnabled = true;
        updateVolumeButtonUi();

        // TTS 초기화
        initializeTextToSpeech();

        // 화면 진입 시 안내 멘트 재생 (TTS가 준비되면 실행)
        waitForTtsReady(() -> speak("문답 화면입니다. 질문에 대한 답변을 말씀해주세요.", true));

        // 음성 입력 버튼 클릭
        btnVoiceInput.setOnClickListener(v -> {
            // TTS가 끝난 후 STT 시작
            speak("질문에 대한 답변을 말씀해주세요.", false);
        });

        // 음성 ON/OFF 버튼
        volumeOnBtn.setOnClickListener(v -> toggleTts());
    }

    // ================= TTS =================
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.KOREAN);
                isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED);

                if (isTtsReady) {
                    textToSpeech.setSpeechRate(0.8f);
                }
            }
        });

        // TTS 종료 후 콜백 설정
        textToSpeech.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) { }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    if ("start_speech".equals(utteranceId)) {
                        startSpeechRecognition();
                    }
                });
            }

            @Override
            public void onError(String utteranceId) { }
        });
    }

    private void speak(String text, boolean force) {
        if (!isTtsEnabled || !isTtsReady || textToSpeech == null) return;

        if (force && isSpeaking) {
            textToSpeech.stop();
            isSpeaking = false;
        }

        String utteranceId = force ? "force" : "start_speech";

        textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId
        );
        isSpeaking = true;
    }

    private void stopSpeaking() {
        if (textToSpeech != null && isSpeaking) {
            textToSpeech.stop();
            isSpeaking = false;
        }
    }

    private void toggleTts() {
        isTtsEnabled = !isTtsEnabled;

        if (!isTtsEnabled) {
            stopSpeaking();
        }

        updateVolumeButtonUi();
    }

    private void updateVolumeButtonUi() {
        if (volumeOnBtn == null) return;

        if (isTtsEnabled) {
            volumeOnBtn.setText("🔇 음성 중단하기");
            volumeOnBtn.setGravity(Gravity.START);
        } else {
            volumeOnBtn.setText("🔈 음성 재생하기");
            volumeOnBtn.setGravity(Gravity.END);
        }
    }

    // ================= STT =================
    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "답변을 말씀해주세요.");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "음성 인식을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Speech recognition error: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                recognizedText = results.get(0);
                statusText.setText("인식 완료!");

                // Firebase 저장
                saveAnswerToFirebase(recognizedText);
            }
        } else {
            speak("음성 인식에 실패했습니다. 다시 시도해주세요.", true);
        }
    }

    // ================= Firebase 저장 =================
    private void saveAnswerToFirebase(String text) {
        if (text == null || text.trim().isEmpty()) {
            Toast.makeText(this, "답변이 비어있습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> answerData = new HashMap<>();
        answerData.put("answerText", text.trim());
        answerData.put("answered", true);
        answerData.put("timestamp", System.currentTimeMillis());

        dbRef.child("users")
                .child(uid)
                .child("answers")
                .child("answerText")
                .setValue(answerData)
                .addOnSuccessListener(v -> {
                    speak("답변이 저장되었습니다.", true);
                    new Handler().postDelayed(this::finish, 2000);
                })
                .addOnFailureListener(e -> {
                    speak("답변 저장에 실패했습니다.", true);
                    Log.e(TAG, "Firebase 저장 실패: " + e.getMessage());
                });
    }

    // ================= TTS 준비 후 콜백 =================
    private void waitForTtsReady(Runnable runnable) {
        new Handler().postDelayed(() -> {
            if (isTtsReady) {
                runnable.run();
            } else {
                waitForTtsReady(runnable);
            }
        }, 100);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSpeaking();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}