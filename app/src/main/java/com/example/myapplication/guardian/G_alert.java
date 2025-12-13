package com.example.myapplication.guardian;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.firebase.database.*;

public class G_alert extends AppCompatActivity {

    private DatabaseReference dbRef;

    private LinearLayout todayAlertLayout;
    private LinearLayout yesterdayAlertLayout;

    private String connectionCode = "8893"; // ❗ 실제로는 로그인 정보에서 가져오기

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_galert);

        todayAlertLayout = findViewById(R.id.todayAlert);
        yesterdayAlertLayout = findViewById(R.id.yesterdayAlert);

        dbRef = FirebaseDatabase.getInstance().getReference("Todos");

        loadCompletedTodos();
    }

    // ================= 완료된 할 일 로드 =================
    private void loadCompletedTodos() {

        dbRef.child(connectionCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        todayAlertLayout.removeAllViews();
                        yesterdayAlertLayout.removeAllViews();

                        for (DataSnapshot todoSnapshot : snapshot.getChildren()) {

                            Boolean isCompleted =
                                    todoSnapshot.child("isCompleted").getValue(Boolean.class);
                            Boolean pushAlert =
                                    todoSnapshot.child("pushAlert").getValue(Boolean.class);

                            if (isCompleted == null || !isCompleted) continue;
                            if (pushAlert != null && pushAlert) continue;

                            String content =
                                    todoSnapshot.child("content").getValue(String.class);
                            String time =
                                    todoSnapshot.child("time").getValue(String.class);

                            addAlertButton(content, time);

                            // 🔥 알림 처리 완료 표시
                            todoSnapshot.getRef().child("pushAlert").setValue(true);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("G_alert", error.getMessage());
                    }
                });
    }

    // ================= 알림 버튼 생성 =================
    private void addAlertButton(String content, String time) {

        Button alertBtn = new Button(this);
        alertBtn.setText(content + "을(를) 완료하였습니다. " + time);
        alertBtn.setAllCaps(false);
        alertBtn.setTextSize(14f);

        // 🔸 지금은 전부 오늘 알림 처리
        todayAlertLayout.addView(alertBtn);
    }
}