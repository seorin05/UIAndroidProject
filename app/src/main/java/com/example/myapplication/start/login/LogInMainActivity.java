package com.example.myapplication.start.login;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LogInMainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference db;

    private EditText etName;
    private EditText etPassword;
    private AutoCompleteTextView roleSelector;
    private EditText etConnectionCode;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_in_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase 초기화
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();

        // View 초기화
        etName = findViewById(R.id.et_todo_empty);
        etPassword = findViewById(R.id.et_todo_empty2);
        roleSelector = findViewById(R.id.roleSelect);
        etConnectionCode = findViewById(R.id.et_todo_empty4);
        btnLogin = findViewById(R.id.sign_up);

        // 역할 선택 드롭다운
        String[] roles = {"어르신", "보호자"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, roles);
        roleSelector.setAdapter(adapter);

        roleSelector.setOnClickListener(v -> roleSelector.showDropDown());

        btnLogin.setOnClickListener(v -> login());
    }

    private void login() {
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String role = roleSelector.getText().toString().trim();
        String connectionCode = etConnectionCode.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!role.equals("어르신") && !role.equals("보호자")) {
            Toast.makeText(this, "역할을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 보호자는 연결번호 필수
        if (role.equals("보호자") && connectionCode.isEmpty()) {
            Toast.makeText(this, "연결번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = name + "@yourapp.com";

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            db.child("users").child(user.getUid())
                                    .get()
                                    .addOnSuccessListener(snapshot -> {
                                        if (snapshot.exists()) {
                                            String savedRole = snapshot.child("role").getValue(String.class);
                                            String savedCode = snapshot.child("connectionCode").getValue(String.class);

                                            if (role.equals("어르신")) {
                                                // 어르신은 연결번호 확인 없이 로그인
                                                Toast.makeText(this, "로그인 성공! 역할: 어르신", Toast.LENGTH_SHORT).show();
                                            } else if (role.equals("보호자")) {
                                                // 보호자는 연결번호 확인 필요
                                                if (savedCode != null && savedCode.equals(connectionCode)) {
                                                    Toast.makeText(this, "로그인 성공! 역할: 보호자", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(this, "연결번호가 올바르지 않습니다", Toast.LENGTH_SHORT).show();
                                                    auth.signOut();
                                                }
                                            }
                                        } else {
                                            Toast.makeText(this, "사용자 정보가 없습니다", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "사용자 정보 불러오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "로그인 실패";
                        Toast.makeText(this, "로그인 실패: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
