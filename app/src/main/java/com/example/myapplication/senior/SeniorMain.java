package com.example.myapplication.senior;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.net.Uri; // 전화

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class SeniorMain extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_senior_main);

        // 버튼
        Button todoButton = findViewById(R.id.btn_todo);
        Button scheButton = findViewById(R.id.btn_schedule);
        Button qnaButton = findViewById(R.id.btn_qa);
        Button callButton = findViewById(R.id.btn_call);
        // 음성 중단하기 버튼 추가
        // 안내 다시듣기 버튼 추가

        // 클릭 리스너 설정
        todoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SeniorTodo.class);
                startActivity(intent);
            }
        });
        scheButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ScheduleMainActivity.class);
                startActivity(intent);
            }
        });
        qnaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Qna_main.class);
                startActivity(intent);
            }
        });
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Intent.ACTION_DIAL);
                startActivity(intent);
            }
        });
    }
}