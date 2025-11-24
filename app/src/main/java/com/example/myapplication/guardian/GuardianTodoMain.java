package com.example.myapplication.guardian;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.TodoItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GuardianTodoMain extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TodoAdapter adapter;
    private List<TodoItem> todoList;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guardian_todo_main);

        // 1. 버튼 설정 (기존 코드 유지)
        Button addButton = findViewById(R.id.btn_add_todo);
        Button deleteButton = findViewById(R.id.btn_delete_todo);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), GuardianTodoAdd.class);
                startActivity(intent);
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), GuardianTodoDelete.class);
                startActivity(intent);
            }
        });

        // 2. 리사이클러뷰 초기화
        recyclerView = findViewById(R.id.rv_todo_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // 세로로 나열

        todoList = new ArrayList<>();
        adapter = new TodoAdapter(todoList);
        recyclerView.setAdapter(adapter);

        // 3. 파이어베이스 데이터 가져오기 (그룹 코드 "1234")
        String groupCode = "1234";
        mDatabase = FirebaseDatabase.getInstance().getReference("Todos").child(groupCode);

        // 데이터 변화를 감지하는 리스너
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                todoList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    TodoItem todoItem = dataSnapshot.getValue(TodoItem.class);
                    if (todoItem != null) {
                        todoList.add(todoItem);
                    }
                }

                // ★★★ 여기서 정렬 시작! ★★★
                // 리스트를 시간 순서대로 정렬합니다.
                Collections.sort(todoList, new Comparator<TodoItem>() {
                    @Override
                    public int compare(TodoItem o1, TodoItem o2) {
                        // 두 아이템의 시간을 '분(minute)' 단위 숫자로 바꿔서 비교
                        int time1 = convertToMinutes(o1.time);
                        int time2 = convertToMinutes(o2.time);
                        return Integer.compare(time1, time2); // 오름차순 정렬 (작은 시간 -> 큰 시간)
                    }
                });

                // 어댑터에게 데이터가 바뀌었다고 알림 -> 화면 갱신
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GuardianTodoMain.this, "데이터 로드 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ★★★ 시간 문자열을 비교 가능한 숫자(분)로 바꾸는 헬퍼 함수 ★★★
    // 예: "오전 01:30" -> 90분, "오후 01:30" -> 810분
    private int convertToMinutes(String timeStr) {
        try {
            // 공백을 기준으로 나눔 ["오전", "01:30"]
            String[] parts = timeStr.split(" ");
            String amPm = parts[0]; // 오전 or 오후
            String[] timeParts = parts[1].split(":"); // ["01", "30"]

            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            // 오후라면 12시간을 더함 (단, 12시는 제외)
            if (amPm.equals("오후") && hour != 12) {
                hour += 12;
            }
            // 오전 12시는 0시로 변경
            if (amPm.equals("오전") && hour == 12) {
                hour = 0;
            }

            // 총 분(minute)으로 환산해서 반환
            return (hour * 60) + minute;

        } catch (Exception e) {
            // 형식이 잘못되었거나 에러가 나면 순서를 맨 뒤로 보냄
            return 99999;
        }
    }
}