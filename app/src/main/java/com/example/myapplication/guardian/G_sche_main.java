package com.example.myapplication.guardian;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

import java.util.Calendar;

public class G_sche_main extends AppCompatActivity {

    private LinearLayout scheduleContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gsche_main);

        // 버튼 초기화
        Button btnAddSchedule = findViewById(R.id.btnAddSchedule);
        btnAddSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(G_sche_main.this, G_sche_add.class);
            startActivity(intent);
        });
        scheduleContainer = findViewById(R.id.scheduleContainer);

        TextView tvScheduleDate = findViewById(R.id.tvScheduleDate);

        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        tvScheduleDate.setText(month + "/" + day + " 일정");


    }
}
