package com.example.myapplication.senior;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.start.StartPageActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ScheduleMainActivity extends AppCompatActivity {

    private static final String TAG = "ScheduleMainActivity";

    private DatabaseReference databaseReference;
    private TextView tvScheduleDate;
    private MaterialButton schedulebtn;

    private LinearLayout scheduleContainer;

    private String familyId;
    private int currentScheduleIndex = 0;
    private List<ScheduleItem> allSchedules = new ArrayList<>();

    // 현재 보고 있는 년/월
    private int currentYear;
    private int currentMonth;

    // TTS 관련 변수
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_schedule_main);

        databaseReference = FirebaseDatabase.getInstance().getReference("schedules");

        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        familyId = prefs.getString("familyId", null);

        tvScheduleDate = findViewById(R.id.textView);
        schedulebtn = findViewById(R.id.btnSchedule);
        scheduleContainer = findViewById(R.id.scheduleContainer);

        // TTS 초기화
        initializeTextToSpeech();

        // 일정 추가 버튼
        schedulebtn.setOnClickListener(v -> {
            Intent intent = new Intent(ScheduleMainActivity.this, AddScheduleActivity.class);
            startActivity(intent);
        });

        // 확인 버튼
        MaterialButton btnConfirm = findViewById(R.id.btnConfirm);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                stopSpeaking();
            });
        }

        MaterialButton btnStop2 = findViewById(R.id.VoulumOnBtn);
            btnStop2.setOnClickListener(v -> {
                stopSpeaking();
            });


        MaterialButton btnStop = findViewById(R.id.VoulumOnBtn);
        if (btnStop != null) {
            btnStop.setOnClickListener(v -> {
                stopSpeaking();
            });
        }

        // 다시듣기 버튼
        MaterialButton btnRetry = findViewById(R.id.btnRetry);
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> {
                speakSchedulePrompt();
            });
        }

        // 종료하기 버튼
        MaterialButton btnExit = findViewById(R.id.btnExit);
        btnExit.setOnClickListener(v -> {
            Intent intent = new Intent(ScheduleMainActivity.this, SeniorMain.class);
            startActivity(intent);
        });

        ImageButton leftArrow = findViewById(R.id.left_arrow);
        ImageButton rightArrow = findViewById(R.id.right_arrow);

        leftArrow.setOnClickListener(v -> {
            // 이전 월로 이동
            if (currentMonth == 1) {
                currentMonth = 12;
                currentYear--;
            } else {
                currentMonth--;
            }
            loadSchedulesForMonth(currentYear, currentMonth);
        });

        rightArrow.setOnClickListener(v -> {
            // 다음 월로 이동
            if (currentMonth == 12) {
                currentMonth = 1;
                currentYear++;
            } else {
                currentMonth++;
            }
            loadSchedulesForMonth(currentYear, currentMonth);
        });

        // 초기값: 오늘 날짜 기준
        Calendar calendar = Calendar.getInstance();
        currentYear = calendar.get(Calendar.YEAR);
        currentMonth = calendar.get(Calendar.MONTH) + 1;

        loadSchedulesForMonth(currentYear, currentMonth);
    }

    /**
     * TTS 초기화
     */
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.KOREAN);
                isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED);

                if (!isTtsReady) {
                    Log.e(TAG, "TTS 한국어 지원 안됨");
                    Toast.makeText(this, "음성 안내를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "TTS 초기화 실패");
            }
        });
    }

    /**
     * 일정 음성 안내 프롬프트 생성 및 재생
     */
    private void speakSchedulePrompt() {
        if (!isTtsReady) {
            Log.w(TAG, "TTS가 준비되지 않았습니다.");
            return;
        }

        String prompt = buildVoicePrompt();
        textToSpeech.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, "schedulePrompt");
        Log.d(TAG, "음성 안내: " + prompt);
    }

    /**
     * 음성 안내 프롬프트 생성
     */
    private String buildVoicePrompt() {
        // 현재 년도와 월 (25년 11월 형식)
        String yearMonth = (currentYear % 100) + "년 " + currentMonth + "월";

        // 일정 부분 동적 생성
        String scheduleText;
        if (allSchedules.isEmpty()) {
            scheduleText = "등록된 일정이 없습니다.";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < allSchedules.size(); i++) {
                sb.append(allSchedules.get(i).title);
                if (i < allSchedules.size() - 1) {
                    sb.append(", ");
                }
            }

            if (allSchedules.size() == 1) {
                scheduleText = sb.toString() + " 일정이 있습니다.";
            } else {
                scheduleText = sb.toString() + " 일정이 있습니다.";
            }
        }

        // 전체 프롬프트 조합 (고정 부분 + 동적 부분)
        return yearMonth + " 일정입니다. " + scheduleText + " " +
                "일정을 추가하려면 일정추가 버튼을, " +
                "일정을 확인하셨으면 확인 버튼을, " +
                "일정 안내를 다시 들으려면 다시듣기 버튼을, " +
                "일정 페이지를 종료하려면 종료하기 버튼을 눌러주세요.";
    }

    /**
     * 음성 중지
     */
    private void stopSpeaking() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSchedulesForMonth(currentYear, currentMonth);
    }

    private void loadSchedulesForMonth(int year, int month) {
        tvScheduleDate.setText("[" + year + "년 " + month + "월 일정]");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                allSchedules.clear();

                if (!dataSnapshot.exists()) {
                    showNoScheduleMessage(year, month);
                    speakSchedulePrompt(); // 음성 안내
                    return;
                }

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String title = snapshot.child("title").getValue(String.class);
                    String date = snapshot.child("date").getValue(String.class);
                    String time = snapshot.child("time").getValue(String.class);
                    String scheduleFamilyId = snapshot.child("familyId").getValue(String.class);

                    // 해당 년/월 일정만 표시
                    String prefix = year + "년 " + month + "월";
                    if (date != null && date.startsWith(prefix)) {
                        if (scheduleFamilyId == null || scheduleFamilyId.equals(familyId)) {
                            allSchedules.add(new ScheduleItem(title, time, date));
                        }
                    }
                }

                Collections.sort(allSchedules, (s1, s2) -> s1.time.compareTo(s2.time));

                currentScheduleIndex = 0;
                displayCurrentSchedules(year, month);

                // 일정 로드 완료 후 음성 안내
                speakSchedulePrompt();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ScheduleMainActivity.this,
                        "데이터 로드 실패: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayCurrentSchedules(int year, int month) {
        scheduleContainer.removeAllViews();

        if (allSchedules.isEmpty()) {
            showNoScheduleMessage(year, month);
            return;
        }

        int startIndex = currentScheduleIndex;
        int endIndex = Math.min(startIndex + 3, allSchedules.size());

        for (int i = startIndex; i < endIndex; i++) {
            ScheduleItem item = allSchedules.get(i);

            View scheduleItemView = LayoutInflater.from(this)
                    .inflate(R.layout.item_schedule_senior, scheduleContainer, false);

            TextView tvTitle = scheduleItemView.findViewById(R.id.tvScheduleTitle);
            TextView tvDateTime = scheduleItemView.findViewById(R.id.tvScheduleDateTime);

            tvTitle.setText(item.title);
            tvDateTime.setText(item.date + " " + item.time);

            scheduleContainer.addView(scheduleItemView);
        }
    }

    private void showNoScheduleMessage(int year, int month) {
        scheduleContainer.removeAllViews();

        TextView noScheduleText = new TextView(this);
        noScheduleText.setText(year + "년 " + month + "월 일정이 없습니다.");
        noScheduleText.setTextSize(25);
        noScheduleText.setTypeface(null, android.graphics.Typeface.BOLD);
        noScheduleText.setPadding(20, 40, 20, 40);
        noScheduleText.setGravity(android.view.Gravity.CENTER);
        scheduleContainer.addView(noScheduleText);
    }

    @Override
    protected void onDestroy() {
        // TTS 리소스 해제
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 화면 나갈 때 음성 중지
        stopSpeaking();
    }

    private static class ScheduleItem {
        String title;
        String time;
        String date;

        ScheduleItem(String title, String time, String date) {
            this.title = title;
            this.time = time;
            this.date = date;
        }
    }
}