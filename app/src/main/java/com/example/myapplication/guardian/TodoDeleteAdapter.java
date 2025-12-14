package com.example.myapplication.guardian;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.TodoItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TodoDeleteAdapter extends RecyclerView.Adapter<TodoDeleteAdapter.DeleteViewHolder> {

    private List<TodoItem> todoList;
    private Set<Integer> selectedPositions = new HashSet<>();
    private OnItemSelectedListener listener;

    public interface OnItemSelectedListener {
        void onItemSelected(int count);
    }

    public TodoDeleteAdapter(List<TodoItem> todoList, OnItemSelectedListener listener) {
        this.todoList = todoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeleteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_todo_delete, parent, false);
        return new DeleteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeleteViewHolder holder, int position) {
        TodoItem item = todoList.get(position);
        holder.tvTime.setText(item.time);
        holder.tvContent.setText(item.content);

        // 색상 정의
        int orangeColor = Color.parseColor("#FF9800");
        int whiteColor = Color.WHITE;

        // ★★★ 디자인 변경 로직 (핵심) ★★★
        if (selectedPositions.contains(position)) {
            // [선택됨] -> 주황 배경 / 흰색 글씨 / 빨간 X 아이콘
            holder.container.setBackgroundResource(R.drawable.todo_btn_inprogress_bg); // 주황색 꽉 찬 배경
            holder.tvTime.setTextColor(whiteColor);
            holder.tvContent.setTextColor(whiteColor);
            holder.ivIcon.setImageResource(R.drawable.ic_close); // 빨간 X 아이콘
        } else {
            // [선택 안됨] -> 흰색(테두리) 배경 / 주황 글씨 / 기본 아이콘
            holder.container.setBackgroundResource(R.drawable.todo_btn_incomplete_bg); // 주황색 테두리 배경
            holder.tvTime.setTextColor(orangeColor);
            holder.tvContent.setTextColor(orangeColor);
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position);
            } else {
                selectedPositions.add(position);
            }
            notifyItemChanged(position);
            listener.onItemSelected(selectedPositions.size());
        });
    }

    @Override
    public int getItemCount() {
        return (todoList != null) ? todoList.size() : 0;
    }

    public List<String> getSelectedKeys() {
        List<String> keys = new ArrayList<>();
        for (int pos : selectedPositions) {
            keys.add(todoList.get(pos).key);
        }
        return keys;
    }

    public List<String> getSelectedContents() {
        List<String> contents = new ArrayList<>();
        for (int pos : selectedPositions) {
            contents.add(todoList.get(pos).time + " " + todoList.get(pos).content);
        }
        return contents;
    }

    public static class DeleteViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        TextView tvTime, tvContent;
        ImageView ivIcon;

        public DeleteViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.btn_todo_container);
            tvTime = itemView.findViewById(R.id.tv_todo_time);
            tvContent = itemView.findViewById(R.id.tv_todo_content);
            ivIcon = itemView.findViewById(R.id.iv_todo_icon); // 아이콘 ID 연결 확인
        }
    }
}