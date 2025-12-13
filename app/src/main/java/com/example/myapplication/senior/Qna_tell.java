package com.example.myapplication.senior;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityQnaTellBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Qna_tell extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private ActivityQnaTellBinding binding;

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private TextToSpeech textToSpeech;
    private DatabaseReference dbRef;

    private boolean isTtsReady = false;
    private boolean isSpeaking = false;
    private boolean isTtsEnabled = true;

    private String recognizedText = "";
    private String currentDate;
    private String questionText = "";
    private String currentGuideScript = "";

    private Button volumeOnBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityQnaTellBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbRef = FirebaseDatabase.getInstance().getReference();

        // ===== 날짜 =====
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
                .format(new Date());

        String receivedDate = getIntent().getStringExtra("date");
        if (receivedDate != null) currentDate = receivedDate;

        // ===== 질문 받기 (메인에서 전달) =====
        questionText = getIntent().getStringExtra("questionText");
        if (questionText == null) questionText = "";

        // 화면에 질문 표시
        binding.question.setText(questionText);

        // TTS에서 사용할 안내 문구
        currentGuideScript =
                "질문입니다. " + questionText +
                        " 준비되면 시작하기 버튼을 누르고 답변해주세요." +
                        "안내를 다시 들으려면 안내 다시 듣기 버튼을 눌러주세요.";


        // ===== TTS 상태 =====
        isTtsEnabled = TtsStateManager.isTtsEnabled(this);

        // ===== 음성 ON/OFF 버튼 =====
        volumeOnBtn = findViewById(R.id.volumeOnBtn);
        updateVolumeButtonUi();
        volumeOnBtn.setOnClickListener(v -> toggleTts());

        initializeTextToSpeech();
        checkPermissionAndInitialize();
        setupListeners();

        // ===== re 버튼 : 음성 다시 듣기 =====
        binding.re.setOnClickListener(v -> {
            speak(currentGuideScript, true);
        });
    }

    // ================= TTS =================
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.KOREAN);
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;

                if (isTtsReady) {
                    textToSpeech.setSpeechRate(0.8f);

                    textToSpeech.setOnUtteranceProgressListener(
                            new UtteranceProgressListener() {
                                @Override
                                public void onStart(String utteranceId) {
                                    isSpeaking = true;
                                    runOnUiThread(() ->
                                            binding.statusText.setText("안내 중...")
                                    );
                                }

                                @Override
                                public void onDone(String utteranceId) {
                                    isSpeaking = false;
                                    runOnUiThread(() ->
                                            binding.statusText.setText("시작하기 버튼을 눌러주세요")
                                    );
                                }

                                @Override
                                public void onError(String utteranceId) {
                                    isSpeaking = false;
                                }
                            });

                    // ⭐ 최초 진입 시 질문 음성 안내
                    speak(currentGuideScript, true);
                }
            }
        });
    }

    private void speak(String text, boolean flush) {
        if (!isTtsReady || !TtsStateManager.isTtsEnabled(this)) return;

        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                "tts_" + System.currentTimeMillis());

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
    }

    private void stopSpeaking() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            isSpeaking = false;
        }
    }

    private void toggleTts() {
        isTtsEnabled = !isTtsEnabled;
        TtsStateManager.setTtsEnabled(this, isTtsEnabled);

        if (!isTtsEnabled) stopSpeaking();
        updateVolumeButtonUi();
    }

    private void updateVolumeButtonUi() {
        if (isTtsEnabled) {
            volumeOnBtn.setText("🔇 음성 중단하기");
            volumeOnBtn.setGravity(Gravity.START);
        } else {
            volumeOnBtn.setText("🔈 음성 재생하기");
            volumeOnBtn.setGravity(Gravity.END);
        }
    }

    // ================= STT =================
    private void checkPermissionAndInitialize() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            initializeSpeechRecognizer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            initializeSpeechRecognizer();
        }
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override public void onReadyForSpeech(Bundle params) {
                stopSpeaking();
                binding.statusText.setText("말씀해주세요...");
            }

            @Override public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    recognizedText += " " + matches.get(0);
                }

                binding.statusText.setText("인식 완료!");
            }

            @Override public void onError(int error) {
                binding.statusText.setText("다시 시도해주세요");
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    // ================= 버튼 =================
    private void setupListeners() {

        binding.btnVoiceInput.setOnClickListener(v -> {
            stopSpeaking();
            speechRecognizer.startListening(recognizerIntent);
        });

        binding.btnSave.setOnClickListener(v -> {
            stopSpeaking();
            saveAnswer();
        });

        binding.end.setOnClickListener(v -> {
            stopSpeaking();
            finish();
        });
    }

    // ================= Firebase =================
    private void saveAnswer() {

        if (recognizedText.trim().isEmpty()) {
            Toast.makeText(this, "답변을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> answerData = new HashMap<>();
        answerData.put("answerText", recognizedText.trim());
        answerData.put("answered", true);
        answerData.put("timestamp", System.currentTimeMillis());

        dbRef.child("users")
                .child(uid)
                .child("answers")
                .child(currentDate)
                .setValue(answerData)
                .addOnSuccessListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) textToSpeech.shutdown();
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}