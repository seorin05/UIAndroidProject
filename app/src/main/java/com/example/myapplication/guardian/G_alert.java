package com.example.myapplication.guardian;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class G_alert extends AppCompatActivity {

    private static final String TAG = "G_alert";

    // ✅ 서버 API 엔드포인트 (FCM 키 없음)
    private static final String PUSH_API_URL =
            "https://your-server.com/api/send-push"; // 🔴 서버 주소로 교체

    private DatabaseReference dbRef;
    private LinearLayout todayAlertLayout;

    private String guardianUid;
    private String connectionCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_galert);

        todayAlertLayout = findViewById(R.id.todayAlert);
        dbRef = FirebaseDatabase.getInstance().getReference();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        guardianUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadConnectionCode();
    }

    // ======================================================
    // 보호자 connectionCode 로드
    // ======================================================
    private void loadConnectionCode() {
        dbRef.child("users")
                .child(guardianUid)
                .child("connectionCode")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        connectionCode = snapshot.getValue(String.class);
                        if (connectionCode != null) {
                            loadCompletedTodos();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, error.getMessage());
                    }
                });
    }

    // ======================================================
    // 완료된 Todo 로드
    // ======================================================
    private void loadCompletedTodos() {

        dbRef.child("Todos")
                .child(connectionCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        todayAlertLayout.removeAllViews();

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

                            // ✅ 서버에 푸시 요청
                            requestPushToServer(content, time);

                            // ✅ 중복 방지
                            todoSnapshot.getRef()
                                    .child("pushAlert")
                                    .setValue(true);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, error.getMessage());
                    }
                });
    }

    // ======================================================
    // UI 알림 버튼
    // ======================================================
    private void addAlertButton(String content, String time) {
        Button alertBtn = new Button(this);
        alertBtn.setText(content + "을(를) 완료했습니다. (" + time + ")");
        alertBtn.setAllCaps(false);
        todayAlertLayout.addView(alertBtn);
    }

    // ======================================================
    // 서버에 푸시 요청
    // ======================================================
    private void requestPushToServer(String content, String time) {
        new Thread(() -> {
            try {
                URL url = new URL(PUSH_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("guardianUid", guardianUid);
                body.put("content", content);
                body.put("time", time);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes());
                os.flush();
                os.close();

                Log.d(TAG, "서버에 푸시 요청 완료");

            } catch (Exception e) {
                Log.e(TAG, "푸시 요청 실패", e);
            }
        }).start();
    }
}