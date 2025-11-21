package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class GuardianTodoDelete extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guardian_todo_delete);

        // 버튼 찾기
        Button deleteButton = findViewById(R.id.btn_delete_todo);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // SubActivity.class 대신 SeniorMain.class로 변경
                Intent intent = new Intent(getApplicationContext(), GuardianTodoMain.class);
                startActivity(intent);
            }
        });
    }
}