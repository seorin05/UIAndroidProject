package com.example.myapplication.senior;

import android.os.Bundle;
import android.content.Intent;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.databinding.ActivityQnaListen2Binding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

public class Qna_listen_sen extends AppCompatActivity {

    private ActivityQnaListen2Binding binding;
    private DatabaseReference dbRef;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityQnaListen2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbRef = FirebaseDatabase.getInstance().getReference();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 🔹 Listen 화면에서 넘겨준 날짜 받기
        String dateKey = getIntent().getStringExtra("selectDate");
        Log.d("QNA_SEN", "받은 날짜 = " + dateKey);

        if (dateKey == null) {
            binding.answerText.setText("날짜 정보가 전달되지 않았습니다.");
            return;
        }

        // ⭐ 날짜 표시 (요청한 mainText 설정)
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            Date date = sdf.parse(dateKey);

            SimpleDateFormat displayFormat = new SimpleDateFormat("MM월 dd일", Locale.KOREA);
            String displayDate = displayFormat.format(date);

            binding.mainText.setText("[ " + displayDate + " 답변 ]");

        } catch (Exception e) {
            e.printStackTrace();
            binding.mainText.setText("[ 날짜 오류 ]");
        }

        loadDailyAnswer(dateKey);

        binding.end.setOnClickListener(v -> {
            Intent intent = new Intent(Qna_listen_sen.this, Qna_main.class);
            startActivity(intent);
        });
    }


    // 🔥 선택한 날짜의 답변 불러오기
    private void loadDailyAnswer(String dateKey) {
        dbRef.child("users").child(uid).child("answers").child(dateKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            binding.answerText.setText("아직 답변이 없습니다.");
                            return;
                        }

                        String answerText = snapshot.child("answerText").getValue(String.class);

                        if (answerText != null && !answerText.isEmpty()) {
                            binding.answerText.setText(answerText);
                        } else {
                            binding.answerText.setText("답변이 없습니다!");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("Firebase", "답변 불러오기 실패", error.toException());
                        binding.answerText.setText("오류가 발생했습니다.");
                    }
                });
    }
}