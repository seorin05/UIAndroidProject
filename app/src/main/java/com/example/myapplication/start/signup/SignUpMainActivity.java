package com.example.myapplication.start.signup;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SignUpMainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText etName;
    private EditText etPassword;
    private AutoCompleteTextView roleSelector;
    private EditText etConnectionCode;
    private Button btnSignUp;

    private String generatedCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Firebase 초기화
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false) // 오프라인 캐시 끄기
                .build();
        db.setFirestoreSettings(settings);

        // View 초기화
        etName = findViewById(R.id.et_todo_empty);
        etPassword = findViewById(R.id.et_todo_empty2);
        roleSelector = findViewById(R.id.roleSelect);
        etConnectionCode = findViewById(R.id.et_todo_empty4);
        btnSignUp = findViewById(R.id.sign_up);

        // 역할 선택 드롭다운
        String[] roles = {"어르신", "보호자"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, roles);
        roleSelector.setAdapter(adapter);

        // 클릭 시 드롭다운 표시
        roleSelector.setOnClickListener(v -> roleSelector.showDropDown());

        // 선택 시 처리
        roleSelector.setOnItemClickListener((parent, view, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            Toast.makeText(this, selected + " 선택됨", Toast.LENGTH_SHORT).show();
            handleRoleChange(selected);
        });

        btnSignUp.setOnClickListener(v -> signUp());
    }

    // 역할 선택에 따른 연결번호 처리
    private void handleRoleChange(String role) {
        role = role.trim();

        if (role.equals("어르신")) {
            // 어르신인 경우 연결번호 자동 생성
            generateUniqueConnectionCode(code -> {
                generatedCode = code;
                etConnectionCode.setText(code);
                etConnectionCode.setEnabled(false);
                Toast.makeText(this, "연결번호가 생성되었습니다: " + code, Toast.LENGTH_SHORT).show();
            });
        } else if (role.equals("보호자")) {
            // 보호자인 경우 입력 가능
            etConnectionCode.setText("");
            etConnectionCode.setEnabled(true);
            etConnectionCode.setHint("어르신의 연결번호 입력");
            generatedCode = "";
        } else {
            etConnectionCode.setText("");
            etConnectionCode.setEnabled(true);
            generatedCode = "";
        }
    }

    // 랜덤 연결번호 생성 (중복 확인 포함)
    private void generateUniqueConnectionCode(ConnectionCodeCallback callback) {
        Random random = new Random();
        String code = String.format("%04d", random.nextInt(10000));

        db.collection("connectionCodes")
                .document(code)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // 중복되면 다시 생성
                        generateUniqueConnectionCode(callback);
                    } else {
                        callback.onCodeGenerated(code);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "연결번호 생성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // 회원가입 처리
    private void signUp() {
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String role = roleSelector.getText().toString().trim();
        String connectionCode = etConnectionCode.getText().toString().trim();

        // 유효성 검사
        if (name.isEmpty()) {
            Toast.makeText(this, "이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty() || password.length() < 6) {
            Toast.makeText(this, "비밀번호는 6자리 이상 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!role.equals("어르신") && !role.equals("보호자")) {
            Toast.makeText(this, "역할은 '어르신' 또는 '보호자'로 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (role.equals("보호자") && connectionCode.isEmpty()) {
            Toast.makeText(this, "연결번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 보호자인 경우 연결번호 확인
        if (role.equals("보호자")) {
            verifyConnectionCodeAndSignUp(name, password, role, connectionCode);
        } else {
            // 어르신인 경우 바로 회원가입
            createUser(name, password, role, connectionCode);
        }
    }

    // 보호자 연결번호 확인 후 회원가입
    private void verifyConnectionCodeAndSignUp(String name, String password, String role, String connectionCode) {
        db.collection("connectionCodes")
                .document(connectionCode)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String elderlyId = document.getString("userId");
                        createUser(name, password, role, connectionCode, elderlyId);
                    } else {
                        Toast.makeText(this, "유효하지 않은 연결번호입니다", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "연결번호 확인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // 회원가입 (어르신)
    private void createUser(String name, String password, String role, String connectionCode) {
        createUser(name, password, role, connectionCode, null);
    }

    // 회원가입 (공통)
    private void createUser(String name, String password, String role, String connectionCode, String elderlyId) {
        String email = name + "@yourapp.com"; // Firebase Auth는 이메일 필요

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();
                        saveUserToFirestore(userId, name, role, connectionCode, elderlyId);
                    } else {
                        Toast.makeText(this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Firestore 저장
    private void saveUserToFirestore(String userId, String name, String role, String connectionCode, String elderlyId) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("role", role);
        user.put("userId", userId);

        if (role.equals("어르신")) {
            user.put("connectionCode", connectionCode);

            Map<String, Object> codeData = new HashMap<>();
            codeData.put("userId", userId);
            codeData.put("name", name);
            codeData.put("createdAt", System.currentTimeMillis());

            db.collection("connectionCodes")
                    .document(connectionCode)
                    .set(codeData)
                    .addOnSuccessListener(aVoid -> saveUserData(userId, user))
                    .addOnFailureListener(e -> Toast.makeText(this, "연결번호 저장 실패", Toast.LENGTH_SHORT).show());
        } else {
            user.put("connectedElderlyId", elderlyId);
            user.put("connectionCode", connectionCode);
            saveUserData(userId, user);
        }
    }

    private void saveUserData(String userId, Map<String, Object> user) {
        db.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "사용자 정보 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // 콜백 인터페이스
    interface ConnectionCodeCallback {
        void onCodeGenerated(String code);
    }
}
