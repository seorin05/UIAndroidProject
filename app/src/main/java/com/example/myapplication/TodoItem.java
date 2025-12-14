package com.example.myapplication;

public class TodoItem {
    public String content; // 할 일 내용
    public String time;    // 시간
    public boolean isCompleted; // 완료 여부
    public String groupCode; // 연결된 4자리 숫자 (그룹 ID)
    public String key;
    private String todoId;      // 수정을 위해 키값
    private boolean pushAlert;  // 알림 트리거 (false -> true)

    // 파이어베이스는 빈 생성자가 필수
    public TodoItem() { }

    public TodoItem(String content, String time, String groupCode) {
        this.content = content;
        this.time = time;
        this.isCompleted = false; // 기본값은 미완료
        this.groupCode = groupCode;
    }
    public String getTodoId() { return todoId; }
    public void setTodoId(String todoId) { this.todoId = todoId; }

    public boolean isPushAlert() { return pushAlert; }
    public void setPushAlert(boolean pushAlert) { this.pushAlert = pushAlert; }
}
