package com.example.myapplication.guardian;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityGqnaMainBinding;
import android.content.Intent;

public class G_qna_main extends AppCompatActivity {

    private ActivityGqnaMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityGqnaMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.btnAnswer.setOnClickListener(v -> {
            Intent intent = new Intent(G_qna_main.this, G_qna_answer.class);
            startActivity(intent);
        });

        binding.btnListen.setOnClickListener(v -> {
            Intent intent = new Intent(G_qna_main.this, G_qna_listen.class);
            startActivity(intent);
        });
    }
}