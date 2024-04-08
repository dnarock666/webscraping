package com.example.webscraping;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    ImageButton ib_ps4 = findViewById(R.id.ib_ps4);
    ImageButton ib_ps5 = findViewById(R.id.ib_ps5);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ib_ps4.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PS4ScrapingActivity.class);
            startActivity(intent);
        });

        ib_ps5.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PS5ScrapingActivity.class);
            startActivity(intent);
        });
    }
}