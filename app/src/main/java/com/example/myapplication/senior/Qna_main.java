package com.example.myapplication.senior;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityQnaMainBinding;

import android.content.Intent;

public class Qna_main extends AppCompatActivity {

    private ActivityQnaMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityQnaMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.tell.setOnClickListener(v -> {
            Intent intent = new Intent(Qna_main.this, Qna_tell.class);
            startActivity(intent);
        });

        binding.listen.setOnClickListener(v -> {
            Intent intent = new Intent(Qna_main.this, Qna_listen.class);
            startActivity(intent);
        });
    }
}