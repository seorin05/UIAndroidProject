package com.example.myapplication.senior;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.R;
import com.example.myapplication.TodoItem;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SeniorTodo extends AppCompatActivity {

    private ViewPager2 viewPager;
    private SeniorTodoAdapter adapter;
    private List<TodoItem> todoList;
    private DatabaseReference mDatabase;

    private MaterialButton btnComplete, btnIncomplete;
    private ImageView leftArrow, rightArrow;

    private TextToSpeech tts;
    private boolean isTtsReady = false;

    private boolean isInitialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_senior_todo);

        viewPager = findViewById(R.id.todo_list);
        leftArrow = findViewById(R.id.left_arrow);
        rightArrow = findViewById(R.id.right_arrow);

        btnComplete = findViewById(R.id.btn_schedule);
        btnIncomplete = findViewById(R.id.btn_todo);
        Button btnListenAgain = findViewById(R.id.btn_qa);
        Button btnExit = findViewById(R.id.btn_off);
        MaterialButton btnStopVoice = findViewById(R.id.VoulumOnBtn);

        todoList = new ArrayList<>();
        adapter = new SeniorTodoAdapter(todoList);
        viewPager.setAdapter(adapter);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "한국어 지원 불가");
                } else {
                    isTtsReady = true;
                    if (!todoList.isEmpty()) {
                        speakCurrentTask(true);
                    }
                }
            }
        });

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String groupCode = prefs.getString("familyId", "");

        if (groupCode.isEmpty()) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mDatabase = FirebaseDatabase.getInstance().getReference("Todos").child(groupCode);

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                todoList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    TodoItem item = dataSnapshot.getValue(TodoItem.class);
                    if (item != null) {
                        item.key = dataSnapshot.getKey();
                        todoList.add(item);
                    }
                }
                Collections.sort(todoList, (o1, o2) -> o1.time.compareTo(o2.time));

                adapter.notifyDataSetChanged();
                updateUI(viewPager.getCurrentItem());

                if (isInitialLoad && !todoList.isEmpty()) {
                    speakCurrentTask(true);
                    isInitialLoad = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateUI(position);
                speakCurrentTask(true);
            }
        });

        leftArrow.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current > 0) viewPager.setCurrentItem(current - 1);
        });

        rightArrow.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < todoList.size() - 1) viewPager.setCurrentItem(current + 1);
        });

        btnComplete.setOnClickListener(v -> updateTodoStatus(true));
        btnIncomplete.setOnClickListener(v -> updateTodoStatus(false));

        btnListenAgain.setOnClickListener(v -> speakCurrentTask(false));

        btnStopVoice.setOnClickListener(v -> stopTTS());

        btnExit.setOnClickListener(v -> {
            stopTTS();
            Intent intent = new Intent(getApplicationContext(), SeniorMain.class);
            startActivity(intent);
            finish();
        });
    }

    private void speakCurrentTask(boolean isAuto) {
        if (!isTtsReady || tts == null || todoList.isEmpty()) return;

        int position = viewPager.getCurrentItem();
        if (position >= todoList.size()) return;

        TodoItem currentItem = todoList.get(position);

        if (isAuto && currentItem.isCompleted) {
            stopTTS();
            return;
        }

        String taskContent = "오늘의 할 일은 " + currentItem.content + "입니다. " +
                "시간은 " + currentItem.time + "입니다. ";

        String guideContent = "완료하셨으면 1번 완료 버튼을, " +
                "아직 못하셨으면 2번 미완료 버튼을 눌러주세요. " +
                "설명을 다시 들으시려면 3번, " +
                "나가시려면 4번 종료하기를 눌러주세요.";

        tts.speak(taskContent + guideContent, TextToSpeech.QUEUE_FLUSH, null, "TodoGuide");
    }

    private void stopTTS() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
    }

    private void updateUI(int position) {
        if (todoList.isEmpty() || position >= todoList.size()) return;
        TodoItem currentItem = todoList.get(position);

        if (currentItem.isCompleted) {
            setButtonState(btnComplete, false);
            setButtonState(btnIncomplete, true);
        } else {
            setButtonState(btnComplete, true);
            setButtonState(btnIncomplete, false);
        }
    }

    private void setButtonState(MaterialButton btn, boolean isEnabled) {
        btn.setEnabled(isEnabled);
        if (isEnabled) {
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#98CDFF")));
        } else {
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BEBEBE")));
        }
    }

    private void updateTodoStatus(boolean isCompleted) {
        int position = viewPager.getCurrentItem();
        if (todoList.isEmpty() || position >= todoList.size()) return;
        TodoItem currentItem = todoList.get(position);
        if (currentItem.key == null) return;

        mDatabase.child(currentItem.key).child("isCompleted").setValue(isCompleted);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTTS();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}