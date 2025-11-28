package com.example.myapplication.guardian;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class G_sche_add extends AppCompatActivity {

    private EditText timeDialog;
    private EditText dateDialog;
    private EditText titleInput;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gsche_add);

        timeDialog = findViewById(R.id.et_todo_empty);
        dateDialog = findViewById(R.id.date_dialog);
        titleInput = findViewById(R.id.edit_title);
        btnSave = findViewById(R.id.btnSave);

        // 초기 버튼 색 (비활성 상태)
        btnSave.setEnabled(false);

        // 입력값 체크
        setupInputWatcher(titleInput);
        setupInputWatcher(dateDialog);
        setupInputWatcher(timeDialog);

        // 날짜 선택 다이얼로그
        dateDialog.setOnClickListener(v -> showDatePickerDialog());

        // 시간 선택 다이얼로그
        timeDialog.setOnClickListener(v -> showTimePickerDialog(this,
                () -> Toast.makeText(this, "시간이 선택되었습니다!", Toast.LENGTH_SHORT).show(),
                () -> Toast.makeText(this, "선택이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        ));


    }

    // 입력값 변화를 감지하는 메서드
    private void setupInputWatcher(EditText editText) {
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkInputs();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    // 세 입력란이 모두 채워졌는지 확인
    private void checkInputs() {
        String title = titleInput.getText().toString().trim();
        String date = dateDialog.getText().toString().trim();
        String time = timeDialog.getText().toString().trim();

        if (!title.isEmpty() && !date.isEmpty() && !time.isEmpty()) {
            btnSave.setBackgroundColor(getResources().getColor(R.color.orange));
            btnSave.setEnabled(true);
        } else {
            btnSave.setEnabled(false);
        }
    }

    // 날짜 선택 다이얼로그
    private void showDatePickerDialog() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = selectedYear + "년 " + (selectedMonth + 1) + "월 " + selectedDay + "일";
                    dateDialog.setText(selectedDate);
                    Toast.makeText(this, "날짜가 선택되었습니다.", Toast.LENGTH_SHORT).show();
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    // 시간 선택 다이얼로그
    public void showTimePickerDialog(Context context, Runnable onConfirm, Runnable onDismiss) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_schedule_time, null);

        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnDismiss = dialogView.findViewById(R.id.btnDismiss);

        Calendar currentTime = Calendar.getInstance();
        timePicker.setHour(currentTime.get(Calendar.HOUR_OF_DAY));
        timePicker.setMinute(currentTime.get(Calendar.MINUTE));
        timePicker.setIs24HourView(false); // 오전/오후 표시

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        btnDismiss.setOnClickListener(v -> {
            onDismiss.run();
            dialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();

            String amPm = hour < 12 ? "오전" : "오후";
            int displayHour = hour % 12 == 0 ? 12 : hour % 12;
            String formattedTime = String.format("%s %02d:%02d", amPm, displayHour, minute);

            timeDialog.setText(formattedTime);
            onConfirm.run();
            dialog.dismiss();
        });

        dialog.show();
    }
}
