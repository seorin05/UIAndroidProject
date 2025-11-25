package com.example.myapplication.guardian;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.TodoItem;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GuardianTodoDelete extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TodoDeleteAdapter adapter;
    private List<TodoItem> todoList;
    private DatabaseReference mDatabase;
    private MaterialButton btnDelete; // 삭제 버튼

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guardian_todo_delete);

        // 버튼 설정
        btnDelete = findViewById(R.id.btn_delete_todo);
        recyclerView = findViewById(R.id.rv_todo_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ImageView alarm=findViewById(R.id.received_alarm); // 얘 안 씀
        LinearLayout gotoCalendar = findViewById(R.id.nav_calendar);
        LinearLayout gotoQna = findViewById(R.id.nav_notification);
        LinearLayout gotoTodo = findViewById(R.id.nav_todo);

        todoList = new ArrayList<>();

        //        alarm.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//                Intent intent=new Intent(getApplicationContext(),G_sche_main.class);
//                startActivity(intent);
//            }
//        });
        gotoCalendar.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(),G_sche_main.class);
                startActivity(intent);
            }
        });
        gotoQna.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(),G_qna_main.class);
                startActivity(intent);
            }
        });
        gotoTodo.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(),GuardianTodoMain.class);
                startActivity(intent);
            }
        });

        // 어댑터 초기화 및 리스너 설정 (선택 개수 변경 시 호출됨)
        adapter = new TodoDeleteAdapter(todoList, count -> {
            if (count > 0) {
                // 1개 이상 선택됨 -> 버튼 활성화 (주황색)
                btnDelete.setEnabled(true);
                btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FC8A00"))); // BtnP 색상
                btnDelete.setText("할 일 삭제하기");
            } else {
                // 선택 안됨 -> 버튼 비활성화 (회색)
                btnDelete.setEnabled(false);
                btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D7D7D7"))); // BtnD 색상
                btnDelete.setText("할 일 삭제하기");
            }
        });
        recyclerView.setAdapter(adapter);

        // 1. 파이어베이스 데이터 가져오기 (메인과 동일)
        String groupCode = "1234";
        mDatabase = FirebaseDatabase.getInstance().getReference("Todos").child(groupCode);

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                todoList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    TodoItem todoItem = dataSnapshot.getValue(TodoItem.class);
                    if (todoItem != null) {
                        todoItem.key = dataSnapshot.getKey(); // 키 저장 필수!
                        todoList.add(todoItem);
                    }
                }
                java.util.Collections.sort(todoList, new java.util.Comparator<TodoItem>() {
                    @Override
                    public int compare(TodoItem o1, TodoItem o2) {
                        int time1 = convertToMinutes(o1.time);
                        int time2 = convertToMinutes(o2.time);
                        return Integer.compare(time1, time2);
                    }
                });
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 2. 삭제 버튼 클릭 시 다이얼로그 띄우기
        btnDelete.setOnClickListener(v -> showDeleteDialog());
    }

    // 다이얼로그 표시 메서드
    private void showDeleteDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_confirm);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvMessage = dialog.findViewById(R.id.tv_confirm_message);
        Button btnYes = dialog.findViewById(R.id.btn_yes);
        Button btnNo = dialog.findViewById(R.id.btn_no);

        // 선택된 항목들 텍스트 만들기
        List<String> selectedContents = adapter.getSelectedContents();
        StringBuilder message = new StringBuilder();
        for (String content : selectedContents) {
            message.append("- ").append(content).append("\n");
        }
        message.append("할 일을 삭제할까요?");
        tvMessage.setText(message.toString());

        // "아니요" -> 닫기
        btnNo.setOnClickListener(v -> dialog.dismiss());

        // "네" -> 실제 삭제 수행
        btnYes.setOnClickListener(v -> {
            deleteSelectedItems();
            dialog.dismiss();
        });

        dialog.show();
    }

    // 실제 파이어베이스 삭제 메서드
    private void deleteSelectedItems() {
        List<String> keysToDelete = adapter.getSelectedKeys();
        String groupCode = "1234";
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Todos").child(groupCode);

        for (String key : keysToDelete) {
            ref.child(key).removeValue(); // 해당 키의 데이터 삭제
        }

        Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();

        // 메인 화면으로 이동
        Intent intent = new Intent(getApplicationContext(), GuardianTodoMain.class);
        // 뒤로가기 방지
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    // 시간 변환 함수
    private int convertToMinutes(String timeStr) {
        try {
            String[] parts = timeStr.split(" ");
            String amPm = parts[0];
            String[] timeParts = parts[1].split(":");

            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            if (amPm.equals("오후") && hour != 12) hour += 12;
            if (amPm.equals("오전") && hour == 12) hour = 0;

            return (hour * 60) + minute;
        } catch (Exception e) {
            return 99999;
        }
    }
}