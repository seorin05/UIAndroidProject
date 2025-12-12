package com.example.myapplication.guardian;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.TodoItem;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    private List<TodoItem> todoList;
    private String groupCode;

    public TodoAdapter(List<TodoItem> todoList, String groupCode) {
        this.todoList = todoList;
        this.groupCode = groupCode;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_todo, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        TodoItem item = todoList.get(position);

        holder.tvTime.setText(item.time);
        holder.tvContent.setText(item.content);

        int orangeColor = Color.parseColor("#FF9800");
        int whiteColor = Color.WHITE;

        if (item.isCompleted) {
            holder.container.setBackgroundResource(R.drawable.todo_btn_complete_bg);
            holder.tvTime.setTextColor(whiteColor);
            holder.tvContent.setTextColor(whiteColor);
            holder.ivIcon.setImageResource(R.drawable.ic_check);

            holder.ivIcon.setOnClickListener(null);
            holder.ivIcon.setClickable(false);
            holder.itemView.setClickable(false);

        } else {
            holder.container.setBackgroundResource(R.drawable.todo_btn_incomplete_bg);
            holder.tvTime.setTextColor(orangeColor);
            holder.tvContent.setTextColor(orangeColor);
            holder.ivIcon.setImageResource(R.drawable.ic_calendar);

            holder.ivIcon.setClickable(true);
            holder.ivIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (item.getTodoId() == null || groupCode == null) {
                        Toast.makeText(v.getContext(), "오류: 항목 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DatabaseReference ref = FirebaseDatabase.getInstance()
                            .getReference("Todos")
                            .child(groupCode)
                            .child(item.getTodoId())
                            .child("pushAlert");

                    ref.setValue(true)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(v.getContext(), "📢 [" + item.content + "] 알림을 보냈습니다!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(v.getContext(), "알림 전송 실패", Toast.LENGTH_SHORT).show();
                            });
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return (todoList != null) ? todoList.size() : 0;
    }

    public static class TodoViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        TextView tvTime;
        TextView tvContent;
        ImageView ivIcon;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.btn_todo_container);
            tvTime = itemView.findViewById(R.id.tv_todo_time);
            tvContent = itemView.findViewById(R.id.tv_todo_content);
            ivIcon = itemView.findViewById(R.id.iv_todo_icon);
        }
    }
}