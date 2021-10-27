package com.cookandroid.graduation_project;

import com.google.firebase.database.IgnoreExtraProperties;


@IgnoreExtraProperties
public class ReportData {
    public String time;
    public String email;
    public boolean state;
    public String address;





    public ReportData() { }
    public ReportData(String time, String email, boolean state, String address) {
        this.email = email;
        this.time = time;
        this.state=state;
        this.address=address;
    }

    public void setTime(String time) { this.time = time; }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setState(boolean state) { this.state = state; }
    public void setAddress(String address) { this.address = address; }


    public String getTime() { return time; }
    public String getEmail() {
        return email;
    }
    public boolean isState() { return state; }
    public String getAddress() { return address; }



    @Override
    public String toString() {
        return "Report{" +
                "time='" + time + '\'' +
                ", email='" + email + '\'' +
                ", state='" + state + '\'' +
                ", address='" + address + '}';
    }
}