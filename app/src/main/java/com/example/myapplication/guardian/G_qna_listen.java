package com.example.myapplication.guardian;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityGqnaListenBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class G_qna_listen extends AppCompatActivity {

    private static final String TAG = "G_qna_listen";

    private ActivityGqnaListenBinding binding;
    private DatabaseReference dbRef;

    private String guardianUid;
    private String dateKey;
    private String questionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityGqnaListenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbRef = FirebaseDatabase.getInstance().getReference();

        // ================= 로그인 확인 =================
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        guardianUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // ================= Intent 데이터 =================
        dateKey = getIntent().getStringExtra("questionDate");
        questionText = getIntent().getStringExtra("questionText");

        // ================= 질문 먼저 표시 =================
        showQuestionFromIntent();

        // ================= 어르신 연결 → 데이터 로드 =================
        loadConnectedElderly();

        binding.navCalendar.setOnClickListener(v-> {
            Intent intent = new Intent(G_qna_listen.this, G_sche_main.class);
            startActivity(intent);
        });

        binding.navTodo.setOnClickListener(v-> {
            Intent intent = new Intent(G_qna_listen.this, GuardianTodoMain.class);
            startActivity(intent);
        });

        binding.message.setOnClickListener(v -> {
            startActivity(new Intent(this, G_alert.class));
        });
    }

    // ======================================================
    // 질문 표시 (Intent)
    // ======================================================
    private void showQuestionFromIntent() {

        if (TextUtils.isEmpty(questionText)) {
            binding.mainText.setText("질문이 없습니다.");
            return;
        }

        binding.mainText.setText(questionText);
    }

    // ======================================================
    // 보호자 → 연결된 어르신 UID 가져오기
    // ======================================================
    private void loadConnectedElderly() {

        binding.nametext.setText("- 어르신 정보 불러오는 중 -");
        binding.question.setText("답변을 불러오는 중입니다...");

        dbRef.child("users")
                .child(guardianUid)
                .child("connectedElderlyId")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            binding.nametext.setText("- 연결된 어르신 없음 -");
                            binding.question.setText("연결된 어르신이 없습니다.");
                            return;
                        }

                        String elderlyUid = snapshot.getValue(String.class);

                        if (TextUtils.isEmpty(elderlyUid)) {
                            binding.nametext.setText("- 어르신 정보 오류 -");
                            binding.question.setText("어르신 UID가 비어있습니다.");
                            return;
                        }

                        Log.d(TAG, "연결된 어르신 UID: " + elderlyUid);

                        loadElderlyName(elderlyUid);
                        loadAnswer(elderlyUid);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "연결 정보 로드 실패", error.toException());
                        binding.nametext.setText("- 오류 발생 -");
                        binding.question.setText("데이터 로드 실패");
                    }
                });
    }

    // ======================================================
    // 어르신 이름 로드
    // ======================================================
    private void loadElderlyName(String elderlyUid) {

        dbRef.child("users")
                .child(elderlyUid)
                .child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        String name = snapshot.getValue(String.class);

                        if (!TextUtils.isEmpty(name)) {
                            binding.nametext.setText("- " + name + "님 -");
                        } else {
                            binding.nametext.setText("- 이름 없음 -");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "이름 로드 실패", error.toException());
                        binding.nametext.setText("- 이름 로드 오류 -");
                    }
                });
    }

    // ======================================================
    // 어르신 답변 로드
    // ======================================================
    private void loadAnswer(String elderlyUid) {

        if (TextUtils.isEmpty(dateKey)) {
            binding.question.setText("날짜 정보가 없습니다.");
            return;
        }

        dbRef.child("users")
                .child(elderlyUid)
                .child("answers")
                .child(dateKey)
                .child("answerText")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        String answerText = snapshot.getValue(String.class);

                        if (!TextUtils.isEmpty(answerText)) {
                            binding.question.setText(answerText);
                        } else {
                            binding.question.setText("아직 답변이 없습니다.");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "답변 로드 실패", error.toException());
                        binding.question.setText("답변 로드 오류");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}