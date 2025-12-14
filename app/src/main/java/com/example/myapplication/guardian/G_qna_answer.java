package com.example.myapplication.guardian;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityGqnaAnswerBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class G_qna_answer extends AppCompatActivity {

    private ActivityGqnaAnswerBinding binding;
    private DatabaseReference dbRef;

    private String currentDate;
    private String answerText = "";
    private String questionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityGqnaAnswerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase
        dbRef = FirebaseDatabase.getInstance().getReference();

        // 날짜 받기
        currentDate = getIntent().getStringExtra("date");
        if (currentDate == null) {
            Toast.makeText(this, "날짜 정보가 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        // ⭐ 질문 받기
        questionText = getIntent().getStringExtra("questionText");

        // ⭐ 질문 표시
        showQuestion();

        // 👉 TextView를 입력창처럼 사용
        enableTextInput();

        // 저장 버튼
        binding.btnAnswer.setOnClickListener(v -> saveAnswer());

        binding.navCalendar.setOnClickListener(v-> {
            Intent intent = new Intent(G_qna_answer.this, G_sche_main.class);
            startActivity(intent);
        });

        binding.navTodo.setOnClickListener(v-> {
            Intent intent = new Intent(G_qna_answer.this, GuardianTodoMain.class);
            startActivity(intent);
        });

        binding.message.setOnClickListener(v -> {
            startActivity(new Intent(this, G_alert.class));
        });
    }

    /**
     * TextView를 입력창처럼 사용
     */
    private void enableTextInput() {

        // 클릭하면 입력 가능
        binding.question.setFocusableInTouchMode(true);
        binding.question.setFocusable(true);
        binding.question.setCursorVisible(true);

        // 힌트 느낌
        binding.question.setText("");
        binding.question.setHint("보호자 답변을 입력해주세요.");

        // 입력 감지
        binding.question.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                answerText = s.toString().trim();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Firebase 저장
     */
    private void saveAnswer() {

        if (answerText.isEmpty()) {
            Toast.makeText(this, "답변을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> answerData = new HashMap<>();
        answerData.put("answerText", answerText);
        answerData.put("answered", true);   // ⚠ Firebase 구조 그대로
        answerData.put("timestamp", System.currentTimeMillis());

        dbRef.child("users")
                .child(uid)
                .child("answers")
                .child(currentDate)
                .setValue(answerData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "답변이 저장되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void showQuestion() {
        if (questionText != null && !questionText.isEmpty()) {
            binding.mainText.setText(questionText);
        } else {
            binding.mainText.setText("질문을 불러올 수 없습니다.");
        }
    }
}