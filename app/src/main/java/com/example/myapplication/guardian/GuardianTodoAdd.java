package com.example.myapplication.guardian;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.TodoItem; // 2단계에서 만든 클래스 import
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class GuardianTodoAdd extends AppCompatActivity {
    private EditText etTodoContent;
    private TextView tvTimeSelect;
    private MaterialButton btnAddSubmit;

    // 파이어베이스 데이터베이스 참조 변수
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guardian_todo_add);

        // 버튼
//        ImageView alarm=findViewById(R.id.received_alarm); // 알람 화면 이동 버튼 추가
        LinearLayout gotoCalendar = findViewById(R.id.nav_calendar);
        LinearLayout gotoQna = findViewById(R.id.nav_notification);
        LinearLayout gotoTodo = findViewById(R.id.nav_todo);
        btnAddSubmit = findViewById(R.id.btn_add_todo_submit); // 할 일 추가하기
        etTodoContent = findViewById(R.id.et_todo_content); // 내용 입력
        tvTimeSelect = findViewById(R.id.tv_time_select); // 시간 선택

        // 클릭 리스너 설정
//        alarm.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//                Intent intent=new Intent(getApplicationContext(),G_sche_main.class);
//                startActivity(intent);
//            }
//        });
        gotoCalendar.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(),G_sche_main.class);
                startActivity(intent);
            }
        });
        gotoQna.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(),G_qna_main.class);
                startActivity(intent);
            }
        });
        gotoTodo.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(),GuardianTodoMain.class);
                startActivity(intent);
            }
        });

        // 파이어베이스 초기화 ("Todos"라는 경로에 저장할 예정)
        mDatabase = FirebaseDatabase.getInstance().getReference("Todos");

        // 초기 버튼 상태 비활성화
        updateButtonState();

        // 할 일 입력 감지 (키보드 입력 시마다 실행)
        etTodoContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateButtonState(); // 글자가 바뀔 때마다 버튼 상태 체크
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 시간 선택 텍스트 클릭 시 다이얼로그 띄우기
        tvTimeSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        // 추가하기 버튼 클릭 시 파이어베이스 저장
        btnAddSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveToFirebase();
            }
        });
    }

    // 시간 선택 다이얼로그 보여주기
    private void showTimePickerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // 타이틀 제거
        dialog.setContentView(R.layout.dialog_time_picker);   // XML 연결

        TimePicker timePicker = dialog.findViewById(R.id.timePicker);
        Button btnDismiss = dialog.findViewById(R.id.btnDismiss);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);

        // 버튼설정
        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        // 선택(확인) 버튼
        btnConfirm.setOnClickListener(v -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();

            // 시간 포맷 설정
            String amPm = (hour < 12) ? "오전" : "오후";
            int displayHour = (hour > 12) ? hour - 12 : hour;
            if (displayHour == 0) displayHour = 12; // 0시는 12시로 표시

            // %02d는 한 자리 숫자일 때 앞에 0을 붙임 (예: 5분 -> 05분)
            String timeText = String.format("%s %02d:%02d", amPm, displayHour, minute);

            // 텍스트뷰에 넣고, 다이얼로그 닫고, 버튼 상태 체크
            tvTimeSelect.setText(timeText);
            dialog.dismiss();
            updateButtonState();
        });

        dialog.show();
    }

    // 입력값 확인해서 버튼 활성화/비활성화 및 색상 변경
    private void updateButtonState() {
        String content = etTodoContent.getText().toString().trim();
        String time = tvTimeSelect.getText().toString().trim();

        boolean isTimeValid = !time.equals("시간을 선택해주세요.") && !time.isEmpty();
        boolean isContentValid = !content.isEmpty();

        if (isContentValid && isTimeValid) {
            // 둘 다 유효함 -> 활성화 (주황색)
            btnAddSubmit.setEnabled(true);
            btnAddSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FC8A00")));
            btnAddSubmit.setTextColor(Color.WHITE);
        } else {
            // 하나라도 부족함 -> 비활성화 (회색)
            btnAddSubmit.setEnabled(false);
            btnAddSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D7D7D7")));
            btnAddSubmit.setTextColor(Color.WHITE);
        }
    }


    // 파이어베이스 저장 로직
    private void saveToFirebase() {
        String content = etTodoContent.getText().toString().trim();
        String time = tvTimeSelect.getText().toString();
        String groupCode = "1234"; // 나중엔 로그인 정보로 교체

        // 1. 시간을 비교 가능한 숫자(분)로 변환 (예: 오후 12:00 -> 720)
        int minutes = convertToMinutes(time);

        // 2. 정렬을 위한 '키(ID)' 생성
        String randomKey = mDatabase.child(groupCode).push().getKey();

        // [시간(5자리 숫자)]_[랜덤키] 형식으로 만듭니다.
        // %05d: 숫자를 5자리로 맞춤 (예: 720 -> "00720"). 그래야 문자열 정렬이 잘 됩니다.
        String sortableKey = String.format("%05d_%s", minutes, randomKey);

        // 3. 데이터 생성 및 저장
        // 주의: TodoItem 객체 안에는 key를 넣을 필요가 없습니다(읽을 때 넣으면 됨).
        // 저장할 때는 '값'만 중요하니까요.
        TodoItem todoItem = new TodoItem(content, time, groupCode);

        if (randomKey != null) {
            // 만든 '정렬 키'를 경로로 사용하여 저장
            mDatabase.child(groupCode).child(sortableKey).setValue(todoItem)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(GuardianTodoAdd.this, "할 일이 추가되었습니다!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(getApplicationContext(), GuardianTodoMain.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(GuardianTodoAdd.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
    private int convertToMinutes(String timeStr) {
        try {
            String[] parts = timeStr.split(" ");
            String amPm = parts[0];
            String[] timeParts = parts[1].split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            if (amPm.equals("오후") && hour != 12) hour += 12;
            if (amPm.equals("오전") && hour == 12) hour = 0;
            return (hour * 60) + minute;
        } catch (Exception e) {
            return 99999; // 에러 시 맨 뒤로
        }
    }
}