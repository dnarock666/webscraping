package com.example.webscraping;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ImageButton img_ps4 = findViewById(R.id.img_ps4);
        img_ps4.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PS4ScrapingActivity.class);
            startActivity(intent);
        });

        ImageButton img_ps5 = findViewById(R.id.img_ps5);
        img_ps5.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PS5ScrapingActivity.class);
            startActivity(intent);
        });

        ImageButton img_psvr = findViewById(R.id.img_psvr);
//        img_psvr.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, PSVRScrapingActivity.class);
//            startActivity(intent);
//        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}