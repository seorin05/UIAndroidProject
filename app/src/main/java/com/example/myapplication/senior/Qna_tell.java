package com.example.myapplication.senior;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

    private static final String TAG = "Qna_tell";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private ActivityQnaTellBinding binding;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private DatabaseReference dbRef;

    private String recognizedText = "";
    private String currentDate;

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

        // Firebase 초기화
        dbRef = FirebaseDatabase.getInstance().getReference();

        // 현재 날짜 (Intent로 받아오거나 오늘 날짜 사용)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        currentDate = dateFormat.format(new Date());

        // Intent에서 날짜 받기 (Qna_main에서 전달한 경우)
        String receivedDate = getIntent().getStringExtra("date");
        if (receivedDate != null) {
            currentDate = receivedDate;
        }

        // 권한 체크 및 STT 초기화
        checkPermissionAndInitialize();

        // 버튼 리스너 설정
        setupListeners();
    }

    private void checkPermissionAndInitialize() {
        // 마이크 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // 권한 요청
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            // 권한 있으면 STT 초기화
            initializeSpeechRecognizer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 허용됨
                initializeSpeechRecognizer();
                Toast.makeText(this, "마이크 권한이 허용되었습니다", Toast.LENGTH_SHORT).show();
            } else {
                // 권한 거부됨
                Toast.makeText(this, "마이크 권한이 필요합니다", Toast.LENGTH_LONG).show();
                binding.btnVoiceInput.setEnabled(false);
            }
        }
    }

    private void initializeSpeechRecognizer() {
        // SpeechRecognizer 생성
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // Intent 설정
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR"); // 한국어
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        // RecognitionListener 설정
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                binding.statusText.setText("말씀해주세요...");
                binding.btnVoiceInput.setEnabled(false);
            }

            @Override
            public void onBeginningOfSpeech() {
                binding.statusText.setText("듣고 있습니다...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // 음성 크기 변화 (필요시 사용)
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // 버퍼 수신 (필요시 사용)
            }

            @Override
            public void onEndOfSpeech() {
                binding.statusText.setText("처리 중...");
            }

            @Override
            public void onError(int error) {
                String errorMessage = getErrorMessage(error);
                binding.statusText.setText("다시 시도해주세요");
                Toast.makeText(Qna_tell.this, errorMessage, Toast.LENGTH_SHORT).show();
                binding.btnVoiceInput.setEnabled(true);
                Log.e(TAG, "Speech recognition error: " + errorMessage);
            }

            @Override
            public void onResults(Bundle results) {
                // 음성 인식 결과
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    String newText = matches.get(0);

                    // 기존 텍스트에 추가
                    if (!recognizedText.isEmpty()) {
                        recognizedText += " " + newText;
                    } else {
                        recognizedText = newText;
                    }

                    binding.statusText.setText("인식 완료! 계속 말하려면 다시 누르세요");
                }

                binding.btnVoiceInput.setEnabled(true);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // 부분 결과 (실시간 표시)
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // 이벤트 처리
            }
        });
    }

    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "오디오 녹음 오류";
            case SpeechRecognizer.ERROR_CLIENT:
                return "클라이언트 오류";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "권한이 부족합니다";
            case SpeechRecognizer.ERROR_NETWORK:
                return "네트워크 오류";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "네트워크 타임아웃";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "음성을 인식하지 못했습니다";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "음성 인식기가 사용 중입니다";
            case SpeechRecognizer.ERROR_SERVER:
                return "서버 오류";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "음성 입력 시간 초과";
            default:
                return "알 수 없는 오류";
        }
    }

    private void setupListeners() {
        // 음성 입력 버튼
        binding.btnVoiceInput.setOnClickListener(v -> {
            if (speechRecognizer != null) {
                speechRecognizer.startListening(recognizerIntent);
            } else {
                Toast.makeText(this, "음성 인식을 초기화할 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        });

        // 저장 버튼
        binding.btnSave.setOnClickListener(v -> {
            saveAnswer();
        });

        // 취소 버튼
        binding.end.setOnClickListener(v -> {
            finish();
        });
    }

    private void saveAnswer() {
        if (recognizedText.isEmpty()) {
            Toast.makeText(this, "답변을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 답변 데이터
        Map<String, Object> answerData = new HashMap<>();
        answerData.put("text", recognizedText);
        answerData.put("answered", true);
        answerData.put("timestamp", System.currentTimeMillis());

        // Firebase에 저장
        dbRef.child("users").child(uid).child("answers").child(currentDate)
                .setValue(answerData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "답변이 저장되었습니다", Toast.LENGTH_SHORT).show();
                    finish(); // 저장 후 이전 화면으로
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "저장 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "답변 저장 실패", e);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}