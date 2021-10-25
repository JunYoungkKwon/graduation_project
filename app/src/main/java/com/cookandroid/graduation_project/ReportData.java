package com.cookandroid.graduation_project;

import com.google.firebase.database.IgnoreExtraProperties;


@IgnoreExtraProperties
public class ReportData {
    public String time;
    public String email;
    public int reportNum;
    public boolean state;
    public String address;





    public ReportData() { }
    public ReportData(String time, String email, int reportNum, boolean state, String address) {
        this.email = email;
        this.time = time;
        this.reportNum=reportNum;
        this.state=state;
        this.address=address;
    }

    public void setTime(String time) { this.time = time; }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setReportNum(int reportNum) { this.reportNum = reportNum; }
    public void setState(boolean state) { this.state = state; }
    public void setAddress(String address) { this.address = address; }


    public String getTime() { return time; }
    public String getEmail() {
        return email;
    }
    public int getReportNum() { return reportNum; }
    public boolean isState() { return state; }
    public String getAddress() { return address; }



    @Override
    public String toString() {
        return "User{" +
                "time='" + time + '\'' +
                ", email='" + email + '\'' +
                ", reportNum='" + reportNum + '\'' +
                ", state='" + state + '\'' +
                ", address='" + address + '}';
    }
}