package com.cookandroid.graduation_project;

import com.google.firebase.database.IgnoreExtraProperties;


@IgnoreExtraProperties
public class ReportData {
    public String time;
    public String email;


    public ReportData() { }
    public ReportData(String time, String email) {
        this.email = email;
        this.time = time;
    }



    public void setTime(String time) { this.time = time; }


    public void setEmail(String email) {
        this.email = email;
    }
    public String getTime() { return time; }
    public String getEmail() {
        return email;
    }



    @Override
    public String toString() {
        return "User{" +
                "time='" + time + '\'' +
                ", email='" + email + '}';
    }
}