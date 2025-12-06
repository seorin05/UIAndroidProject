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
import java.util.Date;
import java.util.Locale;

public class Qna_listen_gua extends AppCompatActivity {

    private ActivityQnaListen2Binding binding;
    private DatabaseReference dbRef;
    private String seniorUid;

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
        seniorUid = FirebaseAuth.getInstance().getCurrentUser().getUid(); // 현재 로그인한 어르신 UID

        // 날짜 받기 (yyyy-MM-dd)
        String dateKey = getIntent().getStringExtra("selectDate");

        if (dateKey == null) {
            binding.mainText.setText("[ 날짜 없음 ]");
            binding.answerText.setText("날짜 정보가 전달되지 않았습니다.");
            return;
        }

        // 날짜 표시 (MM월 dd일)
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

        // 보호자 UID 찾고 답변 불러오기 (users 루트 기준)
        loadGuardianUidAndAnswer(dateKey);

        binding.end.setOnClickListener(v -> {
            startActivity(new Intent(Qna_listen_gua.this, Qna_main.class));
        });
    }

    /**
     * users 루트 아래에서 connectedElderlyId == seniorUid 인 '보호자' role 사용자를 찾음.
     * (Firebase 구조가 users/{uid}/connectedElderlyId 인 경우에 맞춤)
     */
    private void loadGuardianUidAndAnswer(String dateKey) {

        // users 루트에서 connectedElderlyId가 seniorUid인 사용자들 검색
        dbRef.child("users")
                .orderByChild("connectedElderlyId")
                .equalTo(seniorUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            binding.answerText.setText("연결된 보호자가 없습니다.");
                            Log.w("QNA_GUA", "users에서 connectedElderlyId == " + seniorUid + " 인 항목 없음");
                            return;
                        }

                        // 보호자 역할(role == "보호자")인 항목을 찾아 첫번째 것을 사용
                        for (DataSnapshot userSnap : snapshot.getChildren()) {

                            String role = userSnap.child("role").getValue(String.class);

                            if ("보호자".equals(role)) {
                                String guardianUid = userSnap.getKey();
                                Log.d("QNA_GUA", "찾은 보호자 UID = " + guardianUid);

                                // 찾은 보호자 UID로 답변 불러오기
                                loadDailyAnswer(guardianUid, dateKey);
                                return;
                            }
                        }

                        // 일치하는 보호자 role을 못찾았을 때
                        binding.answerText.setText("연결된 보호자를 찾을 수 없습니다.");
                        Log.w("QNA_GUA", "connectedElderlyId 일치 항목은 있으나 role==보호자 없음");
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        binding.answerText.setText("오류가 발생했습니다.");
                        Log.e("QNA_GUA", "보호자 조회 중 onCancelled", error.toException());
                    }
                });
    }

    /**
     * users/{guardianUid}/answers/{dateKey}/answerText 를 읽어와서 화면에 표시
     */
    private void loadDailyAnswer(String guardianUid, String dateKey) {

        dbRef.child("users")
                .child(guardianUid)
                .child("answers")
                .child(dateKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            binding.answerText.setText("아직 답변이 없습니다.");
                            Log.i("QNA_GUA", "guardianUid=" + guardianUid + " 에 dateKey=" + dateKey + " 의 answers 노드 없음");
                            return;
                        }

                        String answerText = snapshot.child("answerText").getValue(String.class);

                        if (answerText != null && !answerText.isEmpty()) {
                            binding.answerText.setText(answerText);
                            Log.d("QNA_GUA", "답변 로드 성공: " + answerText);
                        } else {
                            binding.answerText.setText("답변이 없습니다!");
                            Log.i("QNA_GUA", "answers 노드는 존재하지만 answerText 가 비어있음");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        binding.answerText.setText("오류가 발생했습니다.");
                        Log.e("QNA_GUA", "답변 불러오기 실패", error.toException());
                    }
                });
    }
}