package com.example.myapplication.senior;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityQnaMainBinding;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.security.cert.PolicyNode;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import com.example.myapplication.senior.TtsStateManager;

  public class Qna_main extends AppCompatActivity {

        private static final String TAG = "Qna_main";
        private ActivityQnaMainBinding binding;

        private DatabaseReference dbRef;
        private Calendar currentDisplayDate;

        private TextToSpeech textToSpeech;
        private boolean isTtsReady = false;
        private boolean isSpeaking = false;

        // ⭐ re 버튼에서 다시 읽을 안내 멘트
        private String currentGuideScript = "";
        // 음성 중단 관련
        private Button volumeOnBtn;
        private boolean isTtsEnabled = true; // 음성 ON/OFF 상태

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);

            binding = ActivityQnaMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            dbRef = FirebaseDatabase.getInstance().getReference();
            currentDisplayDate = Calendar.getInstance();

            initializeTextToSpeech();
            setupNavigationListeners();

            // ⭐ 최초 진입 → 무조건 출력
            loadDailyQuestion(currentDisplayDate.getTime());

            binding.re.setOnClickListener(v -> {
                if (!currentGuideScript.isEmpty()) {
                    speak(currentGuideScript, true);
                }
            });

            binding.tell.setOnClickListener(v -> {
                stopTts();
                Intent intent = new Intent(this, Qna_tell.class);

                String dateStr = new SimpleDateFormat(
                        "yyyy-MM-dd", Locale.KOREA
                ).format(currentDisplayDate.getTime());

                intent.putExtra("date", dateStr);
                intent.putExtra(
                        "questionText",
                        binding.question.getText().toString()
                );

                startActivity(intent);
            });

            binding.listen.setOnClickListener(v -> {
                stopTts();
                startActivity(
                        new Intent(this, Qna_listen.class)
                );
            });

            volumeOnBtn = findViewById(R.id.volumeOnBtn);

            // 3. 초기 상태 (ON)
            isTtsEnabled = true;
            volumeOnBtn.setText("🔇 음성 중단하기");
            volumeOnBtn.setGravity(Gravity.START);

            // 4. 버튼 클릭 리스너
            volumeOnBtn.setOnClickListener(v -> toggleTts());

            isTtsEnabled = true;
            TtsStateManager.setTtsEnabled(this, true);
            updateVolumeButtonUi();

            binding.end.setOnClickListener(v->{
                Intent intent = new Intent(Qna_main.this, SeniorMain.class);
                startActivity(intent);
            });
        }

        // ================= 날짜 이동 =================
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

                Calendar display = (Calendar) currentDisplayDate.clone();
                display.set(Calendar.HOUR_OF_DAY, 0);
                display.set(Calendar.MINUTE, 0);
                display.set(Calendar.SECOND, 0);
                display.set(Calendar.MILLISECOND, 0);

                if (display.before(today)) {
                    currentDisplayDate.add(Calendar.DAY_OF_YEAR, 1);
                    loadDailyQuestion(currentDisplayDate.getTime());
                }
            });
        }

        // ================= 질문 로드 =================
        private void loadDailyQuestion(Date dateToLoad) {

            String queryDate = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.KOREA
            ).format(dateToLoad);

            String displayDate = new SimpleDateFormat(
                    "MM월 dd일", Locale.KOREA
            ).format(dateToLoad);

            binding.mainText.setText("[ " + displayDate + " 문답 ]");

            binding.tell.setEnabled(false);
            binding.listen.setEnabled(false);

            String uid = FirebaseAuth.getInstance()
                    .getCurrentUser().getUid();

            dbRef.child("question")
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(
                                        @NonNull DataSnapshot snapshot) {

                                    String foundQuestion = null;

                                    for (DataSnapshot q :
                                            snapshot.getChildren()) {

                                        String date =
                                                q.child("date")
                                                        .getValue(String.class);
                                        String text =
                                                q.child("text")
                                                        .getValue(String.class);

                                        if (queryDate.equals(date)) {
                                            foundQuestion = text;
                                            break;
                                        }
                                    }

                                    if (foundQuestion != null) {
                                        binding.question.setText(foundQuestion);

                                        // ⭐ 안내 멘트 생성 + 저장
                                        currentGuideScript =
                                                makeGuideScript(
                                                        dateToLoad,
                                                        foundQuestion
                                                );

                                        // ⭐ 페이지 진입 / 날짜 변경 시 무조건 출력
                                        speak(currentGuideScript, true);

                                        loadAnswerStatus(uid, queryDate);
                                    } else {
                                        binding.question.setText(
                                                "해당 날짜의 질문이 없습니다."
                                        );
                                    }
                                }

                                @Override
                                public void onCancelled(
                                        @NonNull DatabaseError error) {
                                    Log.e(TAG,
                                            "질문 로드 실패",
                                            error.toException());
                                }
                            });
        }

        // ================= 답변 여부 =================
        private void loadAnswerStatus(String uid, String dateKey) {
            dbRef.child("users")
                    .child(uid)
                    .child("answers")
                    .child(dateKey)
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(
                                        @NonNull DataSnapshot snapshot) {

                                    boolean hasAnswer =
                                            snapshot.exists()
                                                    && Boolean.TRUE.equals(
                                                    snapshot.child("answered")
                                                            .getValue(Boolean.class)
                                            );

                                    binding.tell.setEnabled(!hasAnswer);
                                    binding.listen.setEnabled(hasAnswer);
                                }

                                @Override
                                public void onCancelled(
                                        @NonNull DatabaseError error) {
                                    binding.tell.setEnabled(true);
                                    binding.listen.setEnabled(false);
                                }
                            });
        }

        // ================= TTS 멘트 =================
        private String makeGuideScript(
                Date date, String questionText) {

            String dateStr = new SimpleDateFormat(
                    "M월 d일", Locale.KOREA
            ).format(date);

            return "문답 화면입니다. " + dateStr +
                    " 질문입니다. " + questionText +
                    " 답변을 남기시려면 답변하기 버튼을, " +
                    " 답변을 들으려면 답변 보기 버튼을 눌러주세요. " +
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
    }