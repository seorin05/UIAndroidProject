package com.example.myapplication.senior;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.example.myapplication.databinding.ActivityQnaListenBinding;
import com.example.myapplication.senior.TtsStateManager;

public class Qna_listen extends AppCompatActivity {
    private static final String TAG = "Qna_listen";
    private ActivityQnaListenBinding binding;
    private DatabaseReference dbRef;
    private Calendar currentDisplayDate;
    // 음성 중단 관련
    private boolean isTtsReady = false;
    private boolean isSpeaking = false;
    private String currentGuideScript = "";
    private TextToSpeech textToSpeech;
    private Button volumeOnBtn;
    private boolean isTtsEnabled = true; // 음성 ON/OFF 상태

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityQnaListenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase 및 날짜 초기화
        dbRef = FirebaseDatabase.getInstance().getReference();
        currentDisplayDate = Calendar.getInstance();

        // 초기 질문 로드 (오늘 날짜)
        loadDailyQuestion(currentDisplayDate.getTime());

        // 클릭 리스너 — 중요한 변경: Intent에 넘기는 날짜를 "현재 화면에서 보여주는 날짜"로 생성
        binding.my.setOnClickListener(v -> {
            String selectDate = formatDateForQuery(currentDisplayDate.getTime());
            Log.d(TAG, "보내는 queryDate (my): " + selectDate);
            Intent intent = new Intent(Qna_listen.this, Qna_listen_sen.class);
            intent.putExtra("selectDate", selectDate);
            startActivity(intent);
        });

        binding.your.setOnClickListener(v -> {
            String selectDate = formatDateForQuery(currentDisplayDate.getTime());
            Log.d(TAG, "보내는 queryDate (your): " + selectDate);
            Intent intent = new Intent(Qna_listen.this, Qna_listen_gua.class);
            intent.putExtra("selectDate", selectDate);
            startActivity(intent);
        });

        binding.end.setOnClickListener(v -> {
            stopTts();
            Intent intent = new Intent(Qna_listen.this, Qna_main.class);
            startActivity(intent);
        });

        // 버튼 리스너 (날짜 네비게이션)
        setupNavigationListeners();

        // 음성 출력 관련
        initializeTextToSpeech();
        currentDisplayDate = Calendar.getInstance();

        // ⭐ 최초 진입 → 무조건 출력
        binding.re.setOnClickListener(v -> {
            if (!currentGuideScript.isEmpty()) {
                speak(currentGuideScript, true);
            }
        });

        volumeOnBtn = findViewById(R.id.volumeOnBtn);

        // 3. 초기 상태 (ON)
        isTtsEnabled = true;
        volumeOnBtn.setText("🔇 음성 중단하기");
        volumeOnBtn.setGravity(Gravity.START);

        // 4. 버튼 클릭 리스너
        volumeOnBtn.setOnClickListener(v -> toggleTts());

        isTtsEnabled = TtsStateManager.isTtsEnabled(this);
        updateVolumeButtonUi();
    }

    // 날짜 포맷 유틸 (일관성 위해 Locale.KOREA 사용)
    private String formatDateForQuery(Date date) {
        SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        return queryDateFormat.format(date);
    }

    // 날짜 및 문답 출력
    private void setupNavigationListeners() {
        binding.arrowLeft.setOnClickListener(v -> {
            currentDisplayDate.add(Calendar.DAY_OF_YEAR, -1);
            loadDailyQuestion(currentDisplayDate.getTime());
        });

        binding.arrowRight.setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar currentDisplay = (Calendar) currentDisplayDate.clone();
            currentDisplay.set(Calendar.HOUR_OF_DAY, 0);
            currentDisplay.set(Calendar.MINUTE, 0);
            currentDisplay.set(Calendar.SECOND, 0);
            currentDisplay.set(Calendar.MILLISECOND, 0);

            if (currentDisplay.before(today)) {
                currentDisplayDate.add(Calendar.DAY_OF_YEAR, 1);
                loadDailyQuestion(currentDisplayDate.getTime());
            } else {
                Log.d(TAG, "이미 오늘입니다. 미래로 갈 수 없습니다.");
            }
        });
    }

    private void loadDailyQuestion(Date dateToLoad) {
        String foundQuestion = null;

        // 날짜 포맷
        SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        String queryDate = queryDateFormat.format(dateToLoad);

        SimpleDateFormat displayDateFormat = new SimpleDateFormat("MM월 dd일", Locale.KOREA);
        String displayDate = displayDateFormat.format(dateToLoad);

        // 날짜 UI 업데이트
        binding.mainText.setText("[ " + displayDate + " 문답 ]");

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 디버그 로그: 현재 비교하려는 날짜
        Log.d(TAG, "loadDailyQuestion() 호출 — queryDate: " + queryDate);

        // 질문 불러오기
        dbRef.child("question")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        String foundQuestion = null;

                        // question 아래에 있는 모든(q01, q02...) 반복 탐색
                        for (DataSnapshot questionSnapshot : snapshot.getChildren()) {

                            // DB 값 가져오기 (널 안전하게)
                            String date = null;
                            String text = null;
                            if (questionSnapshot.child("date").getValue() != null) {
                                date = questionSnapshot.child("date").getValue(String.class);
                            }
                            if (questionSnapshot.child("text").getValue() != null) {
                                text = questionSnapshot.child("text").getValue(String.class);
                            }

                            // 디버그: 각 항목의 date, text 로그 찍기
                            Log.d(TAG, "DB question node key=" + questionSnapshot.getKey()
                                    + " date=" + date + " text=" + text);

                            // date 값이 내가 찾는 날짜와 동일하면 저장 (널 체크 및 trim)
                            if (date != null) {
                                String dateTrim = date.trim();
                                if (dateTrim.equals(queryDate)) {
                                    foundQuestion = (text != null) ? text : "";
                                    break;
                                }
                            }
                        }

                        if (foundQuestion != null) {
                            // 질문 화면에 표시
                            binding.question.setText(foundQuestion);
                        } else {
                            binding.question.setText("해당 날짜의 질문이 존재하지 않습니다.");
                            Log.w(TAG, "해당 날짜의 질문을 찾지 못했습니다. queryDate=" + queryDate);
                        }

                        if (foundQuestion != null) {
                            binding.question.setText(foundQuestion);

                            // ⭐ 여기서 스크립트 생성
                            currentGuideScript = makeGuideScript(dateToLoad, foundQuestion);

                            // ⭐ TTS 켜져 있으면 읽기
                            speak(currentGuideScript, true);

                        } else {
                            binding.question.setText("해당 날짜의 질문이 존재하지 않습니다.");

                            currentGuideScript =
                                    "해당 날짜의 질문이 존재하지 않습니다.";
                            speak(currentGuideScript, true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "질문 로드 실패", error.toException());
                        binding.question.setText("질문 로드 중 오류가 발생했습니다.");
                    }
                });
        currentGuideScript = makeGuideScript(dateToLoad, foundQuestion);
        speak(currentGuideScript, true);
    }

    // ================= TTS 멘트 =================
    private String makeGuideScript(
            Date date, String questionText) {

        String dateStr = new SimpleDateFormat(
                "M월 d일", Locale.KOREA
        ).format(date);

        return "답변 화면입니다. " + dateStr +
                "질문입니다. " + questionText +
                "내 답변을 들으시려면 내 답변 버튼을, " +
                "상대방 답변을 들으려면 상대 답변 버튼을 눌러주세요. " +
                "안내를 다시 들으려면 안내 다시 듣기 버튼을 눌러주세요.";
    }

    // ================= TTS =================
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {

                int result = textToSpeech.setLanguage(Locale.KOREAN);
                isTtsReady = result != TextToSpeech.LANG_NOT_SUPPORTED
                        && result != TextToSpeech.LANG_MISSING_DATA;

                if (isTtsReady) {
                    textToSpeech.setSpeechRate(0.8f);

                    textToSpeech.setOnUtteranceProgressListener(
                            new UtteranceProgressListener() {
                                @Override
                                public void onStart(String utteranceId) {
                                    isSpeaking = true;
                                }

                                @Override
                                public void onDone(String utteranceId) {
                                    isSpeaking = false;
                                }

                                @Override
                                public void onError(String utteranceId) {
                                    isSpeaking = false;
                                }
                            }
                    );

                    // ⭐ TTS 준비 완료 후, 이미 멘트가 있으면 바로 읽기
                    if (!currentGuideScript.isEmpty()) {
                        speak(currentGuideScript, true);
                    }
                }
            }
        });
    }

    private void speak(String text, boolean force) {

        if (!isTtsReady || !TtsStateManager.isTtsEnabled(this)) return;

        if (force && isSpeaking) {
            textToSpeech.stop();
            isSpeaking = false;
        }

        textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "tts"
        );
        isSpeaking = true;
    }

    private void stopTts() {
        if (textToSpeech != null && isSpeaking) {
            textToSpeech.stop();
            isSpeaking = false;
        }
    }

    private void toggleTts() {

        isTtsEnabled = !isTtsEnabled;

        // ⭐ 앱 전체 상태 저장
        TtsStateManager.setTtsEnabled(this, isTtsEnabled);

        if (!isTtsEnabled && textToSpeech != null) {
            textToSpeech.stop();
            isSpeaking = false;
        }

        updateVolumeButtonUi();
    }

    private void updateVolumeButtonUi() {

        if (volumeOnBtn == null) return;

        if (isTtsEnabled) {
            // 🔇 음성 ON 상태 → 중단 가능
            volumeOnBtn.setText("🔇 음성 중단하기");
            volumeOnBtn.setGravity(Gravity.START);
        } else {
            // 🔈 음성 OFF 상태 → 다시 재생 가능
            volumeOnBtn.setText("🔈 음성 재생하기");
            volumeOnBtn.setGravity(Gravity.END);
        }
    }
}