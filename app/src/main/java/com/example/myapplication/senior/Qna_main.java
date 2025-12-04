package com.example.myapplication.senior;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.databinding.ActivityQnaMainBinding;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Qna_main extends AppCompatActivity {

    private static final String TAG = "Qna_main";
    private ActivityQnaMainBinding binding;

    private DatabaseReference dbRef;
    private Calendar currentDisplayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityQnaMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.tell.setOnClickListener(v -> {
            Intent intent = new Intent(Qna_main.this, Qna_tell.class);
            startActivity(intent);
        });

        binding.listen.setOnClickListener(v -> {
            Intent intent = new Intent(Qna_main.this, Qna_listen.class);
            startActivity(intent);
        });


        // 1. Firebase 및 날짜 초기화
        dbRef = FirebaseDatabase.getInstance().getReference();
        currentDisplayDate = Calendar.getInstance();

        // 2. 초기 질문 로드 (오늘 날짜)
        loadDailyQuestion(currentDisplayDate.getTime());

        // 3. 버튼 리스너 설정
        setupNavigationListeners();
    }

    private void setupNavigationListeners() {
        // 'arrowLeft'와 'arrowRight' 사용
        binding.arrowLeft.setOnClickListener(v -> {
            // 하루 전으로 이동
            currentDisplayDate.add(Calendar.DAY_OF_YEAR, -1);
            loadDailyQuestion(currentDisplayDate.getTime());
        });

        binding.arrowRight.setOnClickListener(v -> {
            // 미래 날짜 제한 로직 (오늘을 넘어서지 않도록)
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar nextDay = (Calendar) currentDisplayDate.clone();
            nextDay.add(Calendar.DAY_OF_YEAR, 1);

            if (!nextDay.after(today)) {
                currentDisplayDate.add(Calendar.DAY_OF_YEAR, 1);
                loadDailyQuestion(currentDisplayDate.getTime());
            } else {
                Log.d(TAG, "Cannot move to future date.");
            }
        });
    }

    private void loadDailyQuestion(Date dateToLoad) {

        // 날짜 포맷
        SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        String queryDate = queryDateFormat.format(dateToLoad);

        SimpleDateFormat displayDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        String displayDate = displayDateFormat.format(dateToLoad);

        // 날짜 UI 업데이트
        binding.mainText.setText("[ " + displayDate + " 문답 ]");

        // 버튼은 초기 상태는 모두 비활성화
        binding.tell.setEnabled(false);
        binding.listen.setEnabled(false);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 질문 불러오기
        dbRef.child("question").child(queryDate).child("question")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()) {
                            String questionText = snapshot.getValue(String.class);

                            if (questionText != null) {
                                binding.question.setText(questionText);
                            } else {
                                binding.question.setText("질문 데이터 형식을 확인해주세요.");
                            }

                            // 질문을 불러온 뒤 → 그 날짜의 사용자 답변 여부 조회
                            loadAnswerStatus(uid, queryDate);

                        } else {
                            binding.question.setText("해당 날짜의 질문이 존재하지 않습니다.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadAnswerStatus(String uid, String dateKey) {

        dbRef.child("users").child(uid).child("answers").child(dateKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        Boolean answered = snapshot.child("answered").getValue(Boolean.class);
                        boolean isAnswered = answered != null && answered;

                        if (isAnswered) {
                            // 답변 완료 → 듣기 버튼 활성화
                            binding.tell.setEnabled(false);
                            binding.listen.setEnabled(true);

                        } else {
                            // 답변 미완료 → 답변하기 버튼 활성화
                            binding.tell.setEnabled(true);
                            binding.listen.setEnabled(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}