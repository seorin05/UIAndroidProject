package com.example.myapplication.senior;

import android.os.Bundle;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityQnaListen2Binding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Qna_listen_gua extends AppCompatActivity {

    private ActivityQnaListen2Binding binding;
    private DatabaseReference dbRef;
    private String seniorUid;

    // ===== TTS 관련 =====
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    private boolean isTtsEnabled = true;

    private String currentGuideScript = "";
    private Button volumeOnBtn;
    private Date selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityQnaListen2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbRef = FirebaseDatabase.getInstance().getReference();
        seniorUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // ===== 날짜 받기 =====
        String dateKey = getIntent().getStringExtra("selectDate");
        if (dateKey == null) {
            binding.mainText.setText("[ 날짜 없음 ]");
            binding.answerText.setText("날짜 정보가 전달되지 않았습니다.");
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            selectedDate = sdf.parse(dateKey);

            String displayDate = new SimpleDateFormat(
                    "MM월 dd일", Locale.KOREA
            ).format(selectedDate);

            binding.mainText.setText("[ " + displayDate + " 답변 ]");

        } catch (Exception e) {
            binding.mainText.setText("[ 날짜 오류 ]");
        }

        // ===== TTS 초기화 =====
        initializeTextToSpeech();

        volumeOnBtn = findViewById(R.id.volumeOnBtn);
        isTtsEnabled = TtsStateManager.isTtsEnabled(this);
        updateVolumeButtonUi();

        volumeOnBtn.setOnClickListener(v -> toggleTts());

        binding.re.setOnClickListener(v -> {
            if (!currentGuideScript.isEmpty()) {
                speak(currentGuideScript, true);
            }
        });

        // ===== 보호자 답변 로드 =====
        loadGuardianUidAndAnswer(dateKey);

        binding.end.setOnClickListener(v -> {
            startActivity(new Intent(this, Qna_main.class));
        });
    }

    // ================= 보호자 UID 찾기 =================
    private void loadGuardianUidAndAnswer(String dateKey) {

        dbRef.child("users")
                .orderByChild("connectedElderlyId")
                .equalTo(seniorUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            binding.answerText.setText("연결된 보호자가 없습니다.");
                            currentGuideScript = "연결된 보호자가 없습니다.";
                            speak(currentGuideScript, true);
                            return;
                        }

                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String role = userSnap.child("role").getValue(String.class);

                            if ("보호자".equals(role)) {
                                String guardianUid = userSnap.getKey();
                                loadDailyAnswer(guardianUid, dateKey);
                                return;
                            }
                        }

                        binding.answerText.setText("연결된 보호자를 찾을 수 없습니다.");
                        currentGuideScript = "연결된 보호자를 찾을 수 없습니다.";
                        speak(currentGuideScript, true);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        binding.answerText.setText("오류가 발생했습니다.");
                    }
                });
    }

    // ================= 보호자 답변 불러오기 =================
    private void loadDailyAnswer(String guardianUid, String dateKey) {

        dbRef.child("users")
                .child(guardianUid)
                .child("answers")
                .child(dateKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            binding.answerText.setText("아직 답변이 없습니다.");
                            currentGuideScript = "아직 작성된 답변이 없습니다.";
                            speak(currentGuideScript, true);
                            return;
                        }

                        String answerText =
                                snapshot.child("answerText").getValue(String.class);

                        if (answerText != null && !answerText.isEmpty()) {
                            binding.answerText.setText(answerText);

                            currentGuideScript =
                                    makeGuideScript(
                                            selectedDate,
                                            answerText
                                    );

                            speak(currentGuideScript, true);
                        } else {
                            binding.answerText.setText("답변이 없습니다.");
                            currentGuideScript = "답변이 없습니다.";
                            speak(currentGuideScript, true);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        binding.answerText.setText("오류가 발생했습니다.");
                    }
                });
    }

    // ================= 안내 멘트 =================
    private String makeGuideScript(Date date, String answerText) {

        String dateStr = new SimpleDateFormat(
                "M월 d일", Locale.KOREA
        ).format(date);

        return dateStr +
                "에 작성된 답변입니다. " +
                answerText + ". " +
                "안내를 다시 들으려면 안내 다시 듣기 버튼을 눌러주세요.";
    }

    // ================= TTS =================
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.KOREAN);
                isTtsReady = result != TextToSpeech.LANG_NOT_SUPPORTED
                        && result != TextToSpeech.LANG_MISSING_DATA;
            }
        });
    }

    private void speak(String text, boolean force) {
        if (!isTtsReady || !TtsStateManager.isTtsEnabled(this)) return;
        if (force) textToSpeech.stop();

        textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "tts"
        );
    }

    private void toggleTts() {
        isTtsEnabled = !isTtsEnabled;
        TtsStateManager.setTtsEnabled(this, isTtsEnabled);
        if (!isTtsEnabled) textToSpeech.stop();
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
}