package com.example.myapplication.senior;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ScheduleMainActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private TextView tvScheduleDate;
    private Button schedulebtn;
    private LinearLayout scheduleContainer;

    private String familyId;
    private int currentScheduleIndex = 0;
    private List<ScheduleItem> allSchedules = new ArrayList<>();

    // 🔥 현재 보고 있는 년/월
    private int currentYear;
    private int currentMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_schedule_main);

        databaseReference = FirebaseDatabase.getInstance().getReference("schedules");

        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        familyId = prefs.getString("familyId", null);

        tvScheduleDate = findViewById(R.id.textView);
        schedulebtn = findViewById(R.id.BtnSchedule);
        scheduleContainer = findViewById(R.id.scheduleContainer);

        schedulebtn.setOnClickListener(v -> {
            Intent intent = new Intent(ScheduleMainActivity.this, AddScheduleActivity.class);
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

        // 🔥 초기값: 오늘 날짜 기준
        Calendar calendar = Calendar.getInstance();
        currentYear = calendar.get(Calendar.YEAR);
        currentMonth = calendar.get(Calendar.MONTH) + 1;

        loadSchedulesForMonth(currentYear, currentMonth);
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
                    return;
                }

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String title = snapshot.child("title").getValue(String.class);
                    String date = snapshot.child("date").getValue(String.class);
                    String time = snapshot.child("time").getValue(String.class);
                    String scheduleFamilyId = snapshot.child("familyId").getValue(String.class);

                    // 🔥 해당 년/월 일정만 표시
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
                    .inflate(R.layout.item_schedule, scheduleContainer, false);

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
        noScheduleText.setTextSize(25); // 🔥 글자 크기 25sp
        noScheduleText.setTypeface(null, android.graphics.Typeface.BOLD); // 🔥 굵게
        noScheduleText.setPadding(20, 40, 20, 40);
        noScheduleText.setGravity(android.view.Gravity.CENTER); // 🔥 중앙 정렬
        scheduleContainer.addView(noScheduleText);

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
