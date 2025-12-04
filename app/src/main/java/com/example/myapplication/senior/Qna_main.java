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

            // 현재 표시 중인 날짜 전달
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            String dateStr = dateFormat.format(currentDisplayDate.getTime());
            intent.putExtra("date", dateStr);

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

    // 날짜 및 문답 출력
    private void setupNavigationListeners() {
        binding.arrowLeft.setOnClickListener(v -> {
            // 하루 전으로 이동 (제한 없음)
            currentDisplayDate.add(Calendar.DAY_OF_YEAR, -1);
            loadDailyQuestion(currentDisplayDate.getTime());
        });

        binding.arrowRight.setOnClickListener(v -> {
            // 오늘을 기준으로 비교
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            // 현재 표시 날짜도 시간 초기화
            Calendar currentDisplay = (Calendar) currentDisplayDate.clone();
            currentDisplay.set(Calendar.HOUR_OF_DAY, 0);
            currentDisplay.set(Calendar.MINUTE, 0);
            currentDisplay.set(Calendar.SECOND, 0);
            currentDisplay.set(Calendar.MILLISECOND, 0);

            // 현재 날짜가 오늘보다 이전이면 → 오른쪽 이동 가능
            if (currentDisplay.before(today)) {
                currentDisplayDate.add(Calendar.DAY_OF_YEAR, 1);
                loadDailyQuestion(currentDisplayDate.getTime());
            } else {
                // 이미 오늘이면 → 더 이상 진행 불가
                Log.d(TAG, "이미 오늘입니다. 미래로 갈 수 없습니다.");
            }
        });
    }

    private void loadDailyQuestion(Date dateToLoad) {

        // 날짜 포맷
        SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        String queryDate = queryDateFormat.format(dateToLoad);

        SimpleDateFormat displayDateFormat = new SimpleDateFormat("MM월 dd일", Locale.KOREA);
        String displayDate = displayDateFormat.format(dateToLoad);

        // 날짜 UI 업데이트
        binding.mainText.setText("[ " + displayDate + " 문답 ]");

        // 버튼은 초기 상태는 모두 비활성화
        binding.tell.setEnabled(false);
        binding.listen.setEnabled(false);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 질문 불러오기
        dbRef.child("question")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        String foundQuestion = null;

                        // question 아래에 있는 모든(q01, q02...) 반복 탐색
                        for (DataSnapshot questionSnapshot : snapshot.getChildren()) {

                            String date = questionSnapshot.child("date").getValue(String.class);
                            String text = questionSnapshot.child("text").getValue(String.class);

                            // date 값이 내가 찾는 날짜와 동일하면 저장
                            if (date != null && date.equals(queryDate)) {
                                foundQuestion = text;
                                break;
                            }
                        }

                        if (foundQuestion != null) {
                            // 질문 화면에 표시
                            binding.question.setText(foundQuestion);

                            // 질문 찾은 뒤 → 답변 여부 조회
                            loadAnswerStatus(uid, queryDate);

                        } else {
                            binding.question.setText("해당 날짜의 질문이 존재하지 않습니다.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "질문 로드 실패", error.toException());
                    }
                });
    }

    // 문답 여부에 따른 버튼 활성화
    private void loadAnswerStatus(String uid, String dateKey) {
        dbRef.child("users").child(uid).child("answers").child(dateKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        boolean hasAnswer = snapshot.exists() &&
                                snapshot.child("answered").getValue(Boolean.class) == Boolean.TRUE;

                        // 답변 있으면 듣기 활성화, 없으면 답변하기 활성화
                        binding.tell.setEnabled(!hasAnswer);
                        binding.listen.setEnabled(hasAnswer);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "답변 상태 조회 실패", error.toException());
                        // 에러 시 답변하기 활성화
                        binding.tell.setEnabled(true);
                        binding.listen.setEnabled(false);
                    }
                });
    }
}