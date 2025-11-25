package com.example.myapplication.guardian;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.TodoItem; // TodoItem 클래스 import 필수

import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    private List<TodoItem> todoList;

    public TodoAdapter(List<TodoItem> todoList) {
        this.todoList = todoList;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // list_item_todo.xml 레이아웃을 가져와서 뷰로 만듦
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_todo, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        // 현재 순서의 데이터를 가져옴
        TodoItem item = todoList.get(position);

        // 뷰에 데이터 표시
        holder.tvTime.setText(item.time);
        holder.tvContent.setText(item.content);
    }

    @Override
    public int getItemCount() {
        return (todoList != null) ? todoList.size() : 0;
    }

    // 뷰 홀더: XML의 아이디들을 찾아서 보관하는 역할
    public static class TodoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvContent;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_todo_time);
            tvContent = itemView.findViewById(R.id.tv_todo_content);
        }
    }
}
