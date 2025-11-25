package com.example.myapplication.guardian;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private Set<Integer> selectedPositions = new HashSet<>(); // 선택된 아이템 위치 저장
    private OnItemSelectedListener listener; // 액티비티에 알리기 위한 리스너

    // 인터페이스 정의 (아이템 선택 시 액티비티에 알림)
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

        // 선택 상태에 따라 배경색/테두리 변경
        if (selectedPositions.contains(position)) {
            holder.container.setBackgroundResource(R.drawable.todo_btn_inprogress_bg); // 주황색 배경 (미리 만들어두신 것 사용)
            // 텍스트 색상 변경 등 추가 가능
        } else {
            // 선택 안됨: 기본 흰색 배경 + 주황 테두리
            holder.container.setBackgroundResource(R.drawable.todo_btn_incomplete_bg);
        }

        // 아이템 클릭 이벤트
        holder.itemView.setOnClickListener(v -> {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position); // 선택 해제
            } else {
                selectedPositions.add(position); // 선택
            }
            notifyItemChanged(position); // 화면 갱신
            listener.onItemSelected(selectedPositions.size()); // 액티비티에 선택 개수 전달
        });
    }

    @Override
    public int getItemCount() {
        return (todoList != null) ? todoList.size() : 0;
    }

    // 선택된 아이템들의 Firebase Key 목록 반환
    public List<String> getSelectedKeys() {
        List<String> keys = new ArrayList<>();
        for (int pos : selectedPositions) {
            keys.add(todoList.get(pos).key);
        }
        return keys;
    }

    // 선택된 아이템들의 내용 반환 (다이얼로그 표시용)
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

        public DeleteViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.btn_todo_container); // list_item_todo_delete.xml의 최상위 ID
            tvTime = itemView.findViewById(R.id.tv_todo_time);
            tvContent = itemView.findViewById(R.id.tv_todo_content);
        }
    }
}
