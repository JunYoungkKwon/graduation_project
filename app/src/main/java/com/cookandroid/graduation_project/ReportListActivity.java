package com.cookandroid.graduation_project;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ReportListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_list);

        RecyclerView reportListView = findViewById(R.id.report_list);
        ReportAdapter adapter = new ReportAdapter();
        reportListView.setAdapter(adapter);

    }


    class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

        @NonNull
        @Override
        public ReportAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_report, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportAdapter.ViewHolder holder, int position) {
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