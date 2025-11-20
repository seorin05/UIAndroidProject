package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 버튼 찾기
        Button gotoSeniorButton = findViewById(R.id.btn_senior);
        Button gotoGuardianButton = findViewById(R.id.btn_guardian);

        // 클릭 리스너 설정
        gotoSeniorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // SubActivity.class 대신 SeniorMain.class로 변경
                Intent intent = new Intent(getApplicationContext(), SeniorMain.class);
                startActivity(intent);
            }
        });
        gotoGuardianButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // SubActivity.class 대신 SeniorMain.class로 변경
                Intent intent = new Intent(getApplicationContext(), GuardianTodoMain.class);
                startActivity(intent);
            }
        });
    }
}