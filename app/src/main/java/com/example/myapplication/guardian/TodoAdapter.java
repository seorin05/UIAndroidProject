package com.example.myapplication.guardian;

import android.content.Intent;
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

import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    private List<TodoItem> todoList;

    public TodoAdapter(List<TodoItem> todoList) {
        this.todoList = todoList;
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

        // 1. 텍스트 설정
        holder.tvTime.setText(item.time);
        holder.tvContent.setText(item.content);

        // 색상 정의
        int orangeColor = Color.parseColor("#FF9800");
        int whiteColor = Color.WHITE;

        // 2. 완료 여부(isCompleted)에 따른 디자인 분기
        if (item.isCompleted) {
            // === [완료 상태] ===
            // 배경: 회색
            holder.container.setBackgroundResource(R.drawable.todo_btn_complete_bg);

            // 글자색: 흰색
            holder.tvTime.setTextColor(whiteColor);
            holder.tvContent.setTextColor(whiteColor);

            // 아이콘: 초록색 체크
            holder.ivIcon.setImageResource(R.drawable.ic_check);

            // ★ 클릭 방지: 리스너를 제거하고 클릭 불가능하게 설정
            holder.ivIcon.setOnClickListener(null);
            holder.ivIcon.setClickable(false);
            holder.itemView.setClickable(false); // 아이템 전체 클릭 방지

        } else {
            // === [미완료 상태] ===
            // 배경: 주황 테두리 (기존)
            holder.container.setBackgroundResource(R.drawable.todo_btn_incomplete_bg);

            // 글자색: 주황색
            holder.tvTime.setTextColor(orangeColor);
            holder.tvContent.setTextColor(orangeColor);

            // 아이콘: 주황색 종(알림)
            holder.ivIcon.setImageResource(R.drawable.ic_calendar);

            // ★ 종 아이콘 클릭 이벤트 설정
            holder.ivIcon.setClickable(true);
            holder.ivIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 여기에 "알림 보내기" 기능 구현
                    Toast.makeText(v.getContext(), item.content + " 알림을 전송합니다!", Toast.LENGTH_SHORT).show();

                    // (나중에 여기에 파이어베이스 FCM 알림 전송 코드를 넣어야됨)
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return (todoList != null) ? todoList.size() : 0;
    }

    // 뷰 홀더: XML의 ID들을 찾아서 보관
    public static class TodoViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        TextView tvTime;
        TextView tvContent;
        ImageView ivIcon; // 아이콘 변수 추가

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            // activity의 findViewById가 아니라 itemView.findViewById를 씁니다!
            container = itemView.findViewById(R.id.btn_todo_container);
            tvTime = itemView.findViewById(R.id.tv_todo_time);
            tvContent = itemView.findViewById(R.id.tv_todo_content);
            ivIcon = itemView.findViewById(R.id.iv_todo_icon); // XML ID 연결
        }
    }
}