package com.example.myapplication.guardian;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
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
    private MaterialButton btnDelete;
    private String groupCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guardian_todo_delete);

        btnDelete = findViewById(R.id.btn_delete_todo);
        recyclerView = findViewById(R.id.rv_todo_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        LinearLayout gotoCalendar = findViewById(R.id.nav_calendar);
        LinearLayout gotoQna = findViewById(R.id.nav_notification);
        LinearLayout gotoTodo = findViewById(R.id.nav_todo);

        todoList = new ArrayList<>();

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

        adapter = new TodoDeleteAdapter(todoList, count -> {
            if (count > 0) {
                btnDelete.setEnabled(true);
                btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FC8A00")));
                btnDelete.setText("할 일 삭제하기");
            } else {
                btnDelete.setEnabled(false);
                btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D7D7D7")));
                btnDelete.setText("할 일 삭제하기");
            }
        });
        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        groupCode = prefs.getString("familyId", "");

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
                    TodoItem todoItem = dataSnapshot.getValue(TodoItem.class);
                    if (todoItem != null) {
                        String key = dataSnapshot.getKey();
                        todoItem.setTodoId(key);
                        todoItem.key = key;

                        todoList.add(todoItem);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        btnDelete.setOnClickListener(v -> showDeleteDialog());
    }

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

        List<String> selectedContents = adapter.getSelectedContents();
        StringBuilder message = new StringBuilder();
        for (String content : selectedContents) {
            message.append("- ").append(content).append("\n");
        }
        message.append("할 일을 삭제할까요?");
        tvMessage.setText(message.toString());

        btnNo.setOnClickListener(v -> dialog.dismiss());

        btnYes.setOnClickListener(v -> {
            deleteSelectedItems();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteSelectedItems() {
        List<String> keysToDelete = adapter.getSelectedKeys();

        if (mDatabase != null) {
            for (String key : keysToDelete) {
                mDatabase.child(key).removeValue();
            }
            Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getApplicationContext(), GuardianTodoMain.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "데이터베이스 연결 오류", Toast.LENGTH_SHORT).show();
        }
    }
}