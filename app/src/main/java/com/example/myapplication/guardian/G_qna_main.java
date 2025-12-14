package com.example.myapplication.guardian;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityGqnaMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class G_qna_main extends AppCompatActivity {

    private static final String TAG = "G_Qna_main";

    private DatabaseReference dbRef;
    private Calendar currentDisplayDate;
    private ActivityGqnaMainBinding binding;

    // 🔥 G_qna_listen으로 넘길 데이터
    private String currentQuestionText = "";
    private String currentQuestionDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityGqnaMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ================= 버튼 클릭 =================

        binding.btnAnswer.setOnClickListener(v -> {
            Intent intent = new Intent(this, G_qna_answer.class);
            intent.putExtra("date", currentQuestionDate);
            intent.putExtra("questionText", currentQuestionText);
            startActivity(intent);
        });

        binding.btnListen.setOnClickListener(v -> {
            Intent intent = new Intent(this, G_qna_listen.class);

            // ✅ 날짜 + 질문 같이 전달
            intent.putExtra("questionDate", currentQuestionDate);
            intent.putExtra("questionText", currentQuestionText);

            startActivity(intent);
        });

        // ================= 초기화 =================

        dbRef = FirebaseDatabase.getInstance().getReference();
        currentDisplayDate = Calendar.getInstance();

        loadDailyQuestion(currentDisplayDate.getTime());
        setupNavigationListeners();

        binding.navCalendar.setOnClickListener(v-> {
            Intent intent = new Intent(G_qna_main.this, G_sche_main.class);
            startActivity(intent);
        });

        binding.navTodo.setOnClickListener(v-> {
            Intent intent = new Intent(G_qna_main.this, GuardianTodoMain.class);
            startActivity(intent);
        });

        binding.message.setOnClickListener(v -> {
            startActivity(new Intent(this, G_alert.class));
        });

    }

    // ================= 날짜 이동 =================

    private void setupNavigationListeners() {

        binding.arrowLeft.setOnClickListener(v -> {
            currentDisplayDate.add(Calendar.DAY_OF_YEAR, -1);
            loadDailyQuestion(currentDisplayDate.getTime());
        });

        binding.arrowRight.setOnClickListener(v -> {

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar current = (Calendar) currentDisplayDate.clone();
            current.set(Calendar.HOUR_OF_DAY, 0);
            current.set(Calendar.MINUTE, 0);
            current.set(Calendar.SECOND, 0);
            current.set(Calendar.MILLISECOND, 0);

            if (current.before(today)) {
                currentDisplayDate.add(Calendar.DAY_OF_YEAR, 1);
                loadDailyQuestion(currentDisplayDate.getTime());
            }
        });
    }

    // ================= 질문 로드 =================

    private void loadDailyQuestion(Date dateToLoad) {

        SimpleDateFormat queryFormat =
                new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        currentQuestionDate = queryFormat.format(dateToLoad);

        SimpleDateFormat displayFormat =
                new SimpleDateFormat("MM월 dd일", Locale.KOREA);
        binding.mainText.setText("[ " + displayFormat.format(dateToLoad) + " 문답 ]");

        binding.btnListen.setEnabled(false);
        binding.btnAnswer.setEnabled(false);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        dbRef.child("question")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        String foundQuestion = null;

                        for (DataSnapshot q : snapshot.getChildren()) {
                            String date = q.child("date").getValue(String.class);
                            String text = q.child("text").getValue(String.class);

                            if (currentQuestionDate.equals(date)) {
                                foundQuestion = text;
                                break;
                            }
                        }

                        if (foundQuestion != null) {
                            currentQuestionText = foundQuestion;
                            binding.question.setText(foundQuestion);
                            loadAnswerStatus(uid, currentQuestionDate);
                        } else {
                            currentQuestionText = "";
                            binding.question.setText("해당 날짜의 질문이 존재하지 않습니다.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "질문 로드 실패", error.toException());
                    }
                });
    }

    // ================= 답변 여부 =================

    private void loadAnswerStatus(String uid, String dateKey) {

        dbRef.child("users")
                .child(uid)
                .child("answers")
                .child(dateKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        boolean hasAnswer =
                                snapshot.exists() &&
                                        snapshot.child("answered")
                                                .getValue(Boolean.class) == Boolean.TRUE;

                        binding.btnAnswer.setEnabled(!hasAnswer);
                        binding.btnListen.setEnabled(hasAnswer);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.btnAnswer.setEnabled(true);
                        binding.btnListen.setEnabled(false);
                    }
                });
    }
}