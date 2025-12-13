package com.example.myapplication.guardian;

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

    private ActivityGqnaListenBinding binding;
    private DatabaseReference dbRef;
    private String guardianUid;
    private String dateKey;
    private String questionFromMain;
    private Date selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityGqnaListenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbRef = FirebaseDatabase.getInstance().getReference();

        // 현재 사용자 확인
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        guardianUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Intent로부터 데이터 받기
        dateKey = getIntent().getStringExtra("date");
        questionFromMain = getIntent().getStringExtra("questionText");

        // 날짜 정보 처리
        if (dateKey == null) {
            binding.mainText.setText("날짜 정보가 없습니다.");
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            selectedDate = sdf.parse(dateKey);

            String displayDate = new SimpleDateFormat(
                    "MM월 dd일", Locale.KOREA
            ).format(selectedDate);

            // 질문 텍스트 설정
            if (!TextUtils.isEmpty(questionFromMain)) {
                binding.mainText.setText("[ " + displayDate + " 질문 ]\n" + questionFromMain);
            } else {
                binding.mainText.setText("[ " + displayDate + " 질문 ]\n질문 정보가 없습니다.");
            }

        } catch (Exception e) {
            Log.e("G_qna_listen", "날짜 파싱 오류: " + e.getMessage());
            binding.mainText.setText("날짜 형식 오류");

            // 날짜 파싱 실패해도 질문은 표시
            if (!TextUtils.isEmpty(questionFromMain)) {
                binding.mainText.setText("[ 질문 ]\n" + questionFromMain);
            }
        }

        // 연결된 어르신 정보 로드
        loadConnectedElderly();
    }

    // ================= 보호자 → 어르신 연결 정보 로드 =================
    private void loadConnectedElderly() {
        dbRef.child("users").child(guardianUid)
                .child("connectedElderlyID")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            binding.nametext.setText("- 어르신 정보 없음 -");
                            binding.question.setText("연결된 어르신이 없습니다.");
                            Log.w("G_qna_listen", "연결된 어르신 정보 없음");
                            return;
                        }

                        String elderlyUid = snapshot.getValue(String.class);

                        if (TextUtils.isEmpty(elderlyUid)) {
                            binding.nametext.setText("- 어르신 정보 없음 -");
                            binding.question.setText("연결된 어르신 정보가 유효하지 않습니다.");
                            return;
                        }

                        // 어르신 이름과 답변 로드
                        loadElderlyName(elderlyUid);
                        loadAnswer(elderlyUid);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        binding.nametext.setText("- 오류 발생 -");
                        binding.question.setText("데이터를 불러오는 중 오류가 발생했습니다.");
                        Log.e("G_qna_listen", "DB 오류: " + error.getMessage());
                    }
                });
    }

    // ================= 어르신 이름 로드 =================
    private void loadElderlyName(String elderlyUid) {
        dbRef.child("users").child(elderlyUid)
                .child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String name = snapshot.getValue(String.class);

                            if (!TextUtils.isEmpty(name)) {
                                binding.nametext.setText("- " + name + "님 -");
                            } else {
                                binding.nametext.setText("- 이름 없음 -");
                            }
                        } else {
                            binding.nametext.setText("- 이름 정보 없음 -");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        binding.nametext.setText("- 오류 -");
                        Log.e("G_qna_listen", "이름 로드 오류: " + error.getMessage());
                    }
                });
    }

    // ================= 답변 로드 =================
    private void loadAnswer(String elderlyUid) {
        if (TextUtils.isEmpty(dateKey)) {
            binding.question.setText("날짜 정보가 없어 답변을 불러올 수 없습니다.");
            return;
        }

        dbRef.child("users").child(elderlyUid)
                .child("answers")
                .child(dateKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            binding.question.setText("아직 답변이 등록되지 않았습니다.");
                            return;
                        }

                        String answerText = snapshot.child("answerText").getValue(String.class);

                        if (!TextUtils.isEmpty(answerText)) {
                            binding.question.setText(answerText);
                        } else {
                            binding.question.setText("답변 내용이 비어있습니다.");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        binding.question.setText("답변을 불러오는 중 오류가 발생했습니다.");
                        Log.e("G_qna_listen", "답변 로드 오류: " + error.getMessage());
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}