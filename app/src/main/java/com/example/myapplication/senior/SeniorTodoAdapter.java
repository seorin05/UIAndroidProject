package com.example.myapplication.senior;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.TodoItem;

import java.util.List;

public class SeniorTodoAdapter extends RecyclerView.Adapter<SeniorTodoAdapter.ViewHolder> {

    private List<TodoItem> todoList;

    public SeniorTodoAdapter(List<TodoItem> todoList) {
        this.todoList = todoList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_senior_todo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TodoItem item = todoList.get(position);
        holder.tvContent.setText(item.content);
        holder.tvTime.setText(item.time);

        // ★ 상태에 따라 카드 배경색 변경 ★
        if (item.isCompleted) {
            // 완료됨 -> 회색 배경
            holder.cardLayout.setBackgroundResource(R.drawable.todo_btn_complete_bg);
        } else {
            // 미완료 -> 주황색 배경
            holder.cardLayout.setBackgroundResource(R.drawable.todo_btn_inprogress_bg);
            // (주황색 꽉 찬 배경 파일 이름 확인 필요)
        }
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime;
        LinearLayout cardLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvTime = itemView.findViewById(R.id.tv_time);
            cardLayout = itemView.findViewById(R.id.card_layout);
        }
    }
}
