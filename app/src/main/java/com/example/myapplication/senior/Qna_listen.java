package com.example.myapplication.senior;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityQnaListenBinding;

public class Qna_listen extends AppCompatActivity {

    private ActivityQnaListenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityQnaListenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.my.setOnClickListener(v -> {
            Intent intent = new Intent(Qna_listen.this, Qna_listen2.class);
            startActivity(intent);
        });

        binding.your.setOnClickListener(v -> {
            Intent intent = new Intent(Qna_listen.this, Qna_listen2.class);
            startActivity(intent);
        });

        binding.end.setOnClickListener(v -> {
            Intent intent = new Intent(Qna_listen.this, Qna_main.class);
            startActivity(intent);
        });
    }
}