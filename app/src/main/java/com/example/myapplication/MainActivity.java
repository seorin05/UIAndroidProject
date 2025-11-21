package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private EditText inputText;
    private Button saveButton;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase 초기화
        database = FirebaseDatabase.getInstance().getReference();

        // UI 연결
        inputText = findViewById(R.id.inputText);
        saveButton = findViewById(R.id.saveButton);

        // 저장 버튼 클릭
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = inputText.getText().toString();

                if (!text.isEmpty()) {
                    // 데이터베이스에 저장
                    database.child("messages").push().setValue(text);

                    Toast.makeText(MainActivity.this, "저장 완료!", Toast.LENGTH_SHORT).show();
                    inputText.setText(""); // 입력창 비우기
                }
            }
        });
    }
}