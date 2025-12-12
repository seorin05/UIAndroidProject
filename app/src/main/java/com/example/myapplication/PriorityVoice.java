package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.senior.SeniorMain;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PriorityVoice extends AppCompatActivity {

    private static final String TAG = "PriorityVoiceActivity";

    // UI 요소
    private TextView tvScheduleTime, tvScheduleTitle;
    private TextView tvTodoTime, tvTodoTitle, tvTodoSummary;
    private Button btnReplay, btnConfirm;
    private LinearLayout llContentContainer;

    // 데이터베이스 및 유저 정보
    private DatabaseReference mDatabase;
    private String familyId;
    private String todayDateStr; // 예: "2025년 12월 13일"

    // 데이터 리스트
    private List<ScheduleItem> todaySchedules = new ArrayList<>();
    private List<TodoItem> todayTodos = new ArrayList<>();

    // TTS
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    private String finalTtsMessage = ""; // 최종적으로 읽을 메시지

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_priority_voice);

        // 1. 초기화
        initViews();
        initData();
        initTTS();

        // 2. 버튼 리스너 설정
        btnConfirm.setOnClickListener(v -> {
            stopTTS();
            // 확인 버튼 클릭 시 디폴트 음성 페이지(메인)로 이동
            Intent intent = new Intent(PriorityVoice.this, SeniorMain.class); // 혹은 메인 액티비티
            startActivity(intent);
            finish();
        });

        btnReplay.setOnClickListener(v -> {
            // 다시 듣기: 버튼 비활성화 후 재재생
            if (isTtsReady) {
                setConfirmButtonEnabled(false);
                speak(finalTtsMessage);
            }
        });
    }

    private void initViews() {
        tvScheduleTime = findViewById(R.id.tv_schedule_time);
        tvScheduleTitle = findViewById(R.id.tv_schedule_title);
        tvTodoTime = findViewById(R.id.tv_todo_time);
        tvTodoTitle = findViewById(R.id.tv_todo_title);
        tvTodoSummary = findViewById(R.id.tv_todo_summary);
        btnReplay = findViewById(R.id.btn_replay);
        btnConfirm = findViewById(R.id.btn_confirm);
        llContentContainer = findViewById(R.id.ll_content_container);

        // 초기 상태: 확인 버튼 비활성화 (음성 다 들을 때까지)
        setConfirmButtonEnabled(false);
    }

    private void initData() {
        // SharedPreference에서 familyId 가져오기
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        familyId = prefs.getString("familyId", null);

        // 일정(schedules)은 Root에서 가져오고, 할 일(Todos)은 별도 경로
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 오늘 날짜 구하기 (포맷: yyyy년 MM월 dd일 - ScheduleMainActivity와 동일하게 맞춤)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        todayDateStr = sdf.format(new Date());

        Log.d(TAG, "Today: " + todayDateStr + ", FamilyId: " + familyId);

        if (familyId == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 데이터 로드 시작 (일정 -> 할일 순차 로드)
        loadSchedules();
    }

    // 1. 일정 데이터 로드 (기존 로직 유지)
    private void loadSchedules() {
        mDatabase.child("schedules").orderByChild("date").equalTo(todayDateStr)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        todaySchedules.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            String sFamilyId = data.child("familyId").getValue(String.class);
                            if (familyId.equals(sFamilyId)) {
                                String title = data.child("title").getValue(String.class);
                                String time = data.child("time").getValue(String.class);
                                todaySchedules.add(new ScheduleItem(title, time));
                            }
                        }
                        // 시간순 정렬
                        Collections.sort(todaySchedules, (o1, o2) -> o1.time.compareTo(o2.time));

                        // 다음 단계: 할 일 로드 호출
                        loadTodos();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Schedule Load Error: " + error.getMessage());
                        loadTodos(); // 에러 발생해도 할 일 로드는 진행
                    }
                });
    }

    // 2. 할 일 데이터 로드 (수정된 로직)
    private void loadTodos() {
        // 경로: Todos -> familyId -> (Key가 시간순으로 되어있음)
        DatabaseReference todoRef = FirebaseDatabase.getInstance().getReference("Todos").child(familyId);

        // orderByKey()를 사용하면 저장할 때 만든 "00720_랜덤키" 덕분에 자동으로 시간순 정렬됨
        todoRef.orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                todayTodos.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    // GuardianTodoAdd에서 저장한 필드명: content, time
                    String content = data.child("content").getValue(String.class);
                    String time = data.child("time").getValue(String.class);

                    // 완료 여부 확인 (GuardianTodoAdd엔 없지만 보통 리스트 화면에서 관리하므로 체크)
                    // 필드가 없으면 기본값 false(미완료)로 처리
                    Boolean isChecked = data.child("isChecked").getValue(Boolean.class);
                    if (isChecked == null) isChecked = false;

                    // 미완료된 할 일만 리스트에 추가
                    if (!isChecked && content != null) {
                        todayTodos.add(new TodoItem(content, time));
                    }
                }

                // 정렬은 이미 키값(분 단위 시간)으로 되어있으므로 별도 sort 불필요
                // 모든 데이터 로드 완료 -> 화면 갱신 및 TTS 실행
                updateUIAndPrepareTTS();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Todo Load Error: " + error.getMessage());
                updateUIAndPrepareTTS();
            }
        });
    }

    // 3. 화면 업데이트 및 TTS 메시지 생성
    private void updateUIAndPrepareTTS() {
        StringBuilder ttsBuilder = new StringBuilder();

        // --- A. 일정 처리 ---
        if (todaySchedules.isEmpty()) {
            tvScheduleTime.setText("");
            tvScheduleTitle.setText("오늘 예정된 \n일정이 없습니다.");
            ttsBuilder.append("오늘 예정된 일정이 없습니다. ");
        } else {
            // 화면: "11:00 ~" (첫 일정 시간), 내용 나열
            tvScheduleTime.setText(todaySchedules.get(0).time + " ~");

            StringBuilder scheduleDisplay = new StringBuilder();
            StringBuilder scheduleVoice = new StringBuilder();

            for (ScheduleItem item : todaySchedules) {
                scheduleDisplay.append(item.title).append(", ");
                // TTS: "오후 2시에 병원 방문 일정이 있습니다."
                scheduleVoice.append(item.time).append("에 ").append(item.title).append(" 일정이 있습니다. ");
            }

            // 텍스트뷰 마지막 콤마 제거
            if (scheduleDisplay.length() > 2) {
                scheduleDisplay.setLength(scheduleDisplay.length() - 2);
            }

            tvScheduleTitle.setText(scheduleDisplay.toString());
            ttsBuilder.append(scheduleVoice);
        }

        // --- B. 할 일 처리 ---
        if (todayTodos.isEmpty()) {
            tvTodoTime.setVisibility(View.GONE);
            tvTodoTitle.setText("예정된 할 일이 없습니다.");
            tvTodoSummary.setVisibility(View.GONE);
            ttsBuilder.append("예정된 할 일이 없습니다. ");
        } else {
            // 1. 가장 빠른 할 일 (0번째 인덱스) - 화면 및 음성 상세 안내
            TodoItem firstTodo = todayTodos.get(0);

            tvTodoTime.setVisibility(View.VISIBLE);
            tvTodoTime.setText(firstTodo.time); // 예: "오후 01:00"

            tvTodoTitle.setVisibility(View.VISIBLE);
            tvTodoTitle.setText(firstTodo.content); // 예: "약 복용"

            ttsBuilder.append(firstTodo.time).append("에 ").append(firstTodo.content).append("이 예정되어 있습니다. ");

            // 2. 나머지 할 일 - 요약
            int remainingCount = todayTodos.size() - 1;
            if (remainingCount > 0) {
                tvTodoSummary.setVisibility(View.VISIBLE);
                tvTodoSummary.setText("그 외 할 일 " + remainingCount + "건이 있습니다");

                ttsBuilder.append("이 외에 총 ").append(remainingCount).append("개의 할 일이 있습니다. ");
            } else {
                tvTodoSummary.setVisibility(View.GONE);
            }
        }

        // --- C. 마무리 멘트 ---
        ttsBuilder.append("넘어가시려면 확인 버튼을, 다시 한 번 들으시려면 다시 듣기 버튼을 눌러주세요.");

        finalTtsMessage = ttsBuilder.toString();

        // TTS 준비되었으면 바로 말하기 시작
        if (isTtsReady) {
            speak(finalTtsMessage);
        }
    }

    private void initTTS() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "음성 언어를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    isTtsReady = true;

                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            // 시작 시 버튼 비활성 (이미 되어있음)
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            // 음성 종료 시 버튼 활성화 (파란색)
                            runOnUiThread(() -> setConfirmButtonEnabled(true));
                        }

                        @Override
                        public void onError(String utteranceId) {
                            runOnUiThread(() -> setConfirmButtonEnabled(true));
                        }
                    });

                    // 데이터 로드가 먼저 끝났다면 여기서 재생 시작
                    if (!finalTtsMessage.isEmpty()) {
                        speak(finalTtsMessage);
                    }
                }
            }
        });
    }

    private void speak(String text) {
        if (textToSpeech != null && isTtsReady) {
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "PriorityVoiceID");
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "PriorityVoiceID");
        }
    }

    private void stopTTS() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    private void setConfirmButtonEnabled(boolean isEnabled) {
        btnConfirm.setEnabled(isEnabled);
        // 버튼 색상은 xml selector (bg_button_confirm_selector)가 enabled 상태에 따라 자동 변경
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    // 일정 모델
    private static class ScheduleItem {
        String title;
        String time;

        public ScheduleItem(String title, String time) {
            this.title = title;
            this.time = time;
        }
    }

    // 할 일 모델
    private static class TodoItem {
        String content;
        String time;

        public TodoItem(String content, String time) {
            this.content = content;
            this.time = time;
        }
    }
}