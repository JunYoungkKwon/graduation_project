package com.cookandroid.graduation_project;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.renderscript.Sampler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kakao.sdk.user.model.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class ReportListActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private ArrayList<ReportData> arrayList;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_list);

        RecyclerView reportListView = findViewById(R.id.report_list);
        ReportAdapter adapter = new ReportAdapter();
        reportListView.setAdapter(adapter);
        arrayList = new ArrayList<>();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        DatabaseReference databaseReference = mDatabase.child("reports");

        Intent intent1 = getIntent();
        email = intent1.getStringExtra("email");

        //데이터 읽기
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                arrayList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    ReportData reportData = postSnapshot.getValue(ReportData.class);
                    if (reportData.email.equals(email)){
                        arrayList.add(reportData); //데이터 배열에 넣고 리사이클러뷰로 보낼 준비
                        Log.d("PRINTARRAY", String.valueOf(arrayList));
                    }

                }
                adapter.notifyDataSetChanged(); //리스트 저장 및 새로고침

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { //참조에 액세스 할 수 없을 때 호출
                Toast.makeText(getApplicationContext(), "데이터를 가져오는데 실패했습니다", Toast.LENGTH_LONG).show();
            }
        });

    }




    class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_report, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            //
        }

        @Override
        public int getItemCount() {
            return 30;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView timeText;
            public TextView reportNumText;
            public TextView locationText;
            public TextView progressText;


            public ViewHolder(View view) {
                super(view);
//                timeText = (TextView) view.findViewById(R.id.);
//                reportNumText = (TextView) view.findViewById(R.id.);
//                locationText = (TextView) view.findViewById(R.id.);
//                progressText = (TextView) view.findViewById(R.id.);
            }
        }
    }

}