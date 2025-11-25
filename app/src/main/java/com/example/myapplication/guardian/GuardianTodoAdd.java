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

        // 버튼 설정
        ImageView alarm=findViewById(R.id.received_alarm); // 얘 안 씀
        LinearLayout gotoCalendar = findViewById(R.id.nav_calendar);
        LinearLayout gotoQna = findViewById(R.id.nav_notification);
        LinearLayout gotoTodo = findViewById(R.id.nav_todo);

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

        // 1. 뷰 찾기
        etTodoContent = findViewById(R.id.et_todo_content);
        tvTimeSelect = findViewById(R.id.tv_time_select);
        btnAddSubmit = findViewById(R.id.btn_add_todo_submit);

        // 파이어베이스 초기화 ("Todos"라는 경로에 저장할 예정)
        mDatabase = FirebaseDatabase.getInstance().getReference("Todos");

        // 2. 초기 버튼 상태 비활성화 (이미 XML에 enabled=false지만 확실하게)
        updateButtonState();

        // 3. 할 일 입력 감지 (키보드 입력 시마다 실행)
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

        // 4. 시간 선택 텍스트 클릭 시 다이얼로그 띄우기
        tvTimeSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        // 5. 추가하기 버튼 클릭 시 파이어베이스 저장
        btnAddSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveToFirebase();
            }
        });
    }

    // 커스텀 타임피커 다이얼로그 보여주기
    private void showTimePickerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // 타이틀 제거
        dialog.setContentView(R.layout.dialog_time_picker);   // 아까 만든 XML 연결

        // 다이얼로그 배경 투명하게 (둥근 모서리 보이게 하려면 필수)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TimePicker timePicker = dialog.findViewById(R.id.timePicker);
        Button btnDismiss = dialog.findViewById(R.id.btnDismiss);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);

        // 버튼설정
        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        // 선택(확인) 버튼
        btnConfirm.setOnClickListener(v -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();

            // 시간 포맷 예쁘게 만들기 (오전/오후 00:00)
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

        //        alarm.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//                Intent intent=new Intent(getApplicationContext(),G_sche_main.class);
//                startActivity(intent);
//            }
//        });

    }

    // 입력값 확인해서 버튼 활성화/비활성화 및 색상 변경
    private void updateButtonState() {
        String content = etTodoContent.getText().toString().trim();
        String time = tvTimeSelect.getText().toString().trim();

        // ★수정된 부분★: 단순히 비어있는지만 보는 게 아니라, 기본 문구인지도 확인해야 함!
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

        // ★중요★: 현재 사용자의 연결 코드 (나중에는 로그인 정보에서 가져와야 함)
        // 지금은 테스트를 위해 "1234"라고 가정
        String currentGroupCode = "1234";

        // 1. 저장할 데이터 객체 생성 (그룹 코드 포함)
        TodoItem todoItem = new TodoItem(content, time, currentGroupCode);

        // 2. 파이어베이스 저장 경로 수정
        // 기존: Todos -> (무작위키) -> 데이터
        // 수정: Todos -> 1234(그룹코드) -> (무작위키) -> 데이터
        // 이렇게 하면 '1234' 그룹의 데이터만 따로 모여서 관리가 훨씬 쉽습니다!

        String key = mDatabase.child(currentGroupCode).push().getKey(); // 그룹 코드 아래에 키 생성

        if (key != null) {
            // child(currentGroupCode)를 추가하여 그룹별로 폴더를 나눕니다.
            mDatabase.child(currentGroupCode).child(key).setValue(todoItem)
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
}