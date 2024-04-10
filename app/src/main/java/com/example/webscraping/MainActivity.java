package com.example.webscraping;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
//    private final ImageButton ib_ps4 = findViewById(R.id.img_ps4);
//    private final ImageButton ib_ps5 = findViewById(R.id.img_ps5);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        findViewById(R.id.img_ps4).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PS4ScrapingActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.img_ps5).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PS5ScrapingActivity.class);
            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}