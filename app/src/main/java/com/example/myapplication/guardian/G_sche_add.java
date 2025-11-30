package com.example.myapplication.guardian;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;

public class G_sche_add extends AppCompatActivity {

    private EditText timeDialog;
    private EditText dateDialog;
    private EditText titleInput;
    private Button btnSave;

    private DatabaseReference databaseReference;
    private String familyId; // 하드코딩 제거

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gsche_add);

        databaseReference = FirebaseDatabase.getInstance().getReference("schedules");

        // 실제 로그인 정보에서 familyId 가져오기
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        familyId = prefs.getString("familyId", null);

        timeDialog = findViewById(R.id.et_todo_empty);
        dateDialog = findViewById(R.id.date_dialog);
        titleInput = findViewById(R.id.edit_title);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setEnabled(false);

        setupInputWatcher(titleInput);
        setupInputWatcher(dateDialog);
        setupInputWatcher(timeDialog);

        dateDialog.setOnClickListener(v -> showDatePickerDialog());
        timeDialog.setOnClickListener(v -> showTimePickerDialog(this,
                () -> Toast.makeText(this, "시간이 선택되었습니다!", Toast.LENGTH_SHORT).show(),
                () -> Toast.makeText(this, "선택이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        ));

        btnSave.setOnClickListener(v -> saveScheduleToFirebase());
    }

    private void setupInputWatcher(EditText editText) {
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { checkInputs(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void checkInputs() {
        String title = titleInput.getText().toString().trim();
        String date = dateDialog.getText().toString().trim();
        String time = timeDialog.getText().toString().trim();

        boolean ready = !title.isEmpty() && !date.isEmpty() && !time.isEmpty();
        btnSave.setEnabled(ready);
        if (ready) btnSave.setBackgroundColor(getResources().getColor(R.color.orange));
    }

    private void showDatePickerDialog() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // G_sche_main의 selectedDate 형식과 정확히 동일하게 저장
                    String selectedDate = selectedYear + "년 " + (selectedMonth + 1) + "월 " + selectedDay + "일";
                    dateDialog.setText(selectedDate);
                    Toast.makeText(this, "날짜가 선택되었습니다.", Toast.LENGTH_SHORT).show();
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    public void showTimePickerDialog(Context context, Runnable onConfirm, Runnable onDismiss) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_schedule_time, null);

        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnDismiss = dialogView.findViewById(R.id.btnDismiss);

        Calendar currentTime = Calendar.getInstance();
        timePicker.setHour(currentTime.get(Calendar.HOUR_OF_DAY));
        timePicker.setMinute(currentTime.get(Calendar.MINUTE));
        timePicker.setIs24HourView(false);

        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();

        btnDismiss.setOnClickListener(v -> { onDismiss.run(); dialog.dismiss(); });

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

    private void saveScheduleToFirebase() {
        String title = titleInput.getText().toString().trim();
        String date = dateDialog.getText().toString().trim();
        String time = timeDialog.getText().toString().trim();

        if (familyId == null || familyId.isEmpty()) {
            Toast.makeText(this, "가족 코드가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String scheduleId = databaseReference.push().getKey();

        HashMap<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("title", title);
        scheduleData.put("date", date);
        scheduleData.put("time", time);
        scheduleData.put("timestamp", System.currentTimeMillis());
        scheduleData.put("familyId", familyId);

        if (scheduleId != null) {
            databaseReference.child(scheduleId).setValue(scheduleData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(G_sche_add.this, "일정이 저장되었습니다!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK); // 저장 성공 알림
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(G_sche_add.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
