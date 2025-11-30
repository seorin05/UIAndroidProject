package com.example.myapplication.guardian;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

public class G_sche_main extends AppCompatActivity {

    private static final String TAG = "G_sche_main";
    private LinearLayout scheduleContainer;
    private DatabaseReference databaseReference;
    private CalendarView calendarView;
    private TextView tvScheduleDate;
    private String selectedDate;   // 선택된 날짜 (없으면 null)
    private String familyId;
    private int currentYear;
    private int currentMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gsche_main);

        // Firebase 초기화
        databaseReference = FirebaseDatabase.getInstance().getReference("schedules");

        // SharedPreferences에서 familyId 가져오기
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        familyId = prefs.getString("familyId", null);

        Log.d(TAG, "familyId: " + familyId);

        // 뷰 초기화
        scheduleContainer = findViewById(R.id.scheduleContainer);
        tvScheduleDate = findViewById(R.id.tvScheduleDate);
        calendarView = findViewById(R.id.calendarView);

        // 일정 추가 버튼
        Button btnAddSchedule = findViewById(R.id.btnAddSchedule);
        btnAddSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(G_sche_main.this, G_sche_add.class);
            startActivityForResult(intent, 100);
        });

        // 현재 월 저장
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(calendarView.getDate());
        currentYear = calendar.get(Calendar.YEAR);
        currentMonth = calendar.get(Calendar.MONTH) + 1;

        // 초기에는 선택된 날짜 없음 → 월 전체 일정 표시
        selectedDate = null;
        tvScheduleDate.setText(currentMonth + "월 일정");

        // 캘린더 날짜 선택 이벤트
        calendarView.setOnDateChangeListener((view, selectedYear, selectedMonth, selectedDay) -> {
            selectedDate = selectedYear + "년 " + (selectedMonth + 1) + "월 " + selectedDay + "일";
            tvScheduleDate.setText((selectedMonth + 1) + "/" + selectedDay + " 일정");
            loadSchedulesFromFirebase();
        });

        // Firebase에서 일정 불러오기
        loadSchedulesFromFirebase();

        // 하단 네비게이션 설정
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSchedulesFromFirebase();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            loadSchedulesFromFirebase();
        }
    }

    private void loadSchedulesFromFirebase() {
        Log.d(TAG, "Loading schedules for date: " + selectedDate);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                scheduleContainer.removeAllViews();

                if (!dataSnapshot.exists()) {
                    showNoScheduleMessage();
                    return;
                }

                boolean hasSchedule = false;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String scheduleId = snapshot.getKey();
                    String title = snapshot.child("title").getValue(String.class);
                    String date = snapshot.child("date").getValue(String.class);
                    String time = snapshot.child("time").getValue(String.class);
                    String scheduleFamilyId = snapshot.child("familyId").getValue(String.class);

                    boolean isSameFamily = (familyId == null || scheduleFamilyId == null || familyId.equals(scheduleFamilyId));

                    if (date != null && isSameFamily) {
                        if (selectedDate == null) {
                            // 🔥 날짜 선택 안 한 경우 → 현재 월 일정만 표시
                            if (date.startsWith(currentYear + "년 " + currentMonth + "월")) {
                                addScheduleItem(scheduleId, title, date, time);
                                hasSchedule = true;
                            }
                        } else {
                            // 🔥 날짜 선택한 경우 → 해당 날짜 일정만 표시
                            if (date.equals(selectedDate)) {
                                addScheduleItem(scheduleId, title, date, time);
                                hasSchedule = true;
                            }
                        }
                    }
                }

                if (!hasSchedule) {
                    showNoScheduleMessage();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                Toast.makeText(G_sche_main.this,
                        "데이터 로드 실패: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addScheduleItem(String scheduleId, String title, String date, String time) {
        View scheduleItem = LayoutInflater.from(this)
                .inflate(R.layout.item_schedule, scheduleContainer, false);

        TextView tvTitle = scheduleItem.findViewById(R.id.tvScheduleTitle);
        TextView tvDateTime = scheduleItem.findViewById(R.id.tvScheduleDateTime);

        tvTitle.setText(title);
        tvDateTime.setText(date + " " + time);

        scheduleContainer.addView(scheduleItem);
    }

    private void deleteSchedule(String scheduleId) {
        databaseReference.child(scheduleId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showNoScheduleMessage() {
        TextView noScheduleText = new TextView(this);
        noScheduleText.setText("등록된 일정이 없습니다.");
        noScheduleText.setTextSize(16);
        noScheduleText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        noScheduleText.setPadding(20, 60, 20, 60);
        noScheduleText.setGravity(android.view.Gravity.CENTER);
        scheduleContainer.addView(noScheduleText);
    }

    private void setupBottomNavigation() {
        LinearLayout navCalendar = findViewById(R.id.nav_calendar);
        LinearLayout navTodo = findViewById(R.id.nav_todo);
        LinearLayout navNotification = findViewById(R.id.nav_notification);

        navCalendar.setOnClickListener(v -> {
            // 현재 화면이므로 아무 동작 없음
        });

        navTodo.setOnClickListener(v -> {
            Toast.makeText(this, "할일 페이지로 이동", Toast.LENGTH_SHORT).show();
        });

        navNotification.setOnClickListener(v -> {
            Toast.makeText(this, "알림 페이지로 이동", Toast.LENGTH_SHORT).show();
        });
    }
}
