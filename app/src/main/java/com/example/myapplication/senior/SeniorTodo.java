package com.example.myapplication.senior;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
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
import java.util.Comparator;
import java.util.List;

public class SeniorTodo extends AppCompatActivity {

    private ViewPager2 viewPager;
    private SeniorTodoAdapter adapter;
    private List<TodoItem> todoList;
    private DatabaseReference mDatabase;

    private MaterialButton btnComplete, btnIncomplete; // 완료(1번), 미완료(2번) 버튼
    private ImageView leftArrow, rightArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_senior_todo);

        // 1. 뷰 연결
        viewPager = findViewById(R.id.todo_list);
        leftArrow = findViewById(R.id.left_arrow);
        rightArrow = findViewById(R.id.right_arrow);
        btnComplete = findViewById(R.id.btn_schedule); // ID 주의: xml에서 완료버튼 ID가 btn_schedule 인가요?
        btnIncomplete = findViewById(R.id.btn_todo);   // ID 주의: xml에서 미완료버튼 ID가 btn_todo 인가요?
        Button btnExit = findViewById(R.id.btn_off);

        // 2. 초기 설정
        todoList = new ArrayList<>();
        adapter = new SeniorTodoAdapter(todoList);
        viewPager.setAdapter(adapter);

        String groupCode = "1234";
        mDatabase = FirebaseDatabase.getInstance().getReference("Todos").child(groupCode);

        // 3. 데이터 불러오기
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
                // 시간순 정렬 (필요하다면)
                Collections.sort(todoList, (o1, o2) -> o1.time.compareTo(o2.time)); // 간단 비교 (문자열)

                adapter.notifyDataSetChanged();

                // 데이터 로드 후 현재 페이지의 버튼 상태 업데이트
                updateUI(viewPager.getCurrentItem());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 4. 페이지 넘길 때 버튼 상태 업데이트 (중요!)
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateUI(position);
            }
        });

        // 5. 화살표 클릭 이벤트
        leftArrow.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current > 0) viewPager.setCurrentItem(current - 1);
        });

        rightArrow.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < todoList.size() - 1) viewPager.setCurrentItem(current + 1);
        });

        // 6. 완료/미완료 버튼 클릭 이벤트 (파이어베이스 업데이트)
        btnComplete.setOnClickListener(v -> updateTodoStatus(true));
        btnIncomplete.setOnClickListener(v -> updateTodoStatus(false));

        btnExit.setOnClickListener(v -> finish());
    }

    // 현재 페이지의 아이템 상태에 따라 버튼 색상/활성화 변경
    private void updateUI(int position) {
        if (todoList.isEmpty() || position >= todoList.size()) return;

        TodoItem currentItem = todoList.get(position);

        if (currentItem.isCompleted) {
            // 이미 완료된 상태 -> 완료 버튼 비활성화(회색), 미완료 버튼 활성화(파랑)
            setButtonState(btnComplete, false);
            setButtonState(btnIncomplete, true);
        } else {
            // 미완료 상태 -> 완료 버튼 활성화(파랑), 미완료 버튼 비활성화(회색)
            setButtonState(btnComplete, true);
            setButtonState(btnIncomplete, false);
        }
    }

    // 버튼 스타일 변경 헬퍼 함수
    private void setButtonState(MaterialButton btn, boolean isEnabled) {
        btn.setEnabled(isEnabled);
        if (isEnabled) {
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#98CDFF"))); // 파란색 (EnabledBtn)
        } else {
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BEBEBE"))); // 회색 (DisabledBtn)
        }
    }

    // 파이어베이스 상태 업데이트
    private void updateTodoStatus(boolean isCompleted) {
        int position = viewPager.getCurrentItem();
        if (todoList.isEmpty() || position >= todoList.size()) return;

        TodoItem currentItem = todoList.get(position);
        if (currentItem.key == null) return;

        // 파이어베이스 값 변경 -> addValueEventListener가 감지해서 화면 자동 갱신됨
        mDatabase.child(currentItem.key).child("isCompleted").setValue(isCompleted);

        // 안내 메시지 (선택 사항)
        String msg = isCompleted ? "완료 처리되었습니다." : "미완료로 변경되었습니다.";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}