package com.cookandroid.graduation_project;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

public class HomeActivity extends AppCompatActivity {

    private String name, email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Intent intent = getIntent();

        name = intent.getStringExtra("name");
        email = intent.getStringExtra("email");

        Log.d("minyong", name+","+email);

        Button recogBtn = findViewById(R.id.home_mask_recog_btn);
        Button covidBtn = findViewById(R.id.home_covid_info_btn);
        Button reportBtn = findViewById(R.id.home_report_list_btn);



        recogBtn.setOnClickListener(view -> {
            startActivity(new Intent(this, ClassifierActivity.class));
        });

        covidBtn.setOnClickListener(view -> {
            startActivity(new Intent(this, CovidInfoActivity.class));
        });

        reportBtn.setOnClickListener(view -> {
            startActivity(new Intent(this, ReportListActivity.class));
        });

    }
}