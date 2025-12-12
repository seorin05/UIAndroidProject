package com.example.myapplication.guardian;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
import java.util.List;

public class GuardianTodoMain extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TodoAdapter adapter;
    private List<TodoItem> todoList;
    private DatabaseReference mDatabase;
    private String groupCode; // 그룹 코드를 저장할 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guardian_todo_main);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        groupCode = prefs.getString("familyId", "");

        if (groupCode.isEmpty()) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Button addButton = findViewById(R.id.btn_add_todo);
        Button deleteButton = findViewById(R.id.btn_delete_todo);

        LinearLayout gotoCalendar = findViewById(R.id.nav_calendar);
        LinearLayout gotoQna = findViewById(R.id.nav_notification);
        LinearLayout gotoTodo = findViewById(R.id.nav_todo);

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

        gotoCalendar.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(), G_sche_main.class);
                startActivity(intent);
            }
        });

        gotoQna.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(), G_qna_main.class);
                startActivity(intent);
            }
        });

        gotoTodo.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(), GuardianTodoMain.class);
                startActivity(intent);
                finish();
            }
        });

        recyclerView = findViewById(R.id.rv_todo_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // 세로로 나열

        todoList = new ArrayList<>();

        adapter = new TodoAdapter(todoList, groupCode);

        recyclerView.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference("Todos").child(groupCode);

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                todoList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    TodoItem todoItem = dataSnapshot.getValue(TodoItem.class);

                    if (todoItem != null) {
                        todoItem.setTodoId(dataSnapshot.getKey());
                        todoList.add(todoItem);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GuardianTodoMain.this, "데이터 로드 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}