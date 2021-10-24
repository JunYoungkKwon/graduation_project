package com.cookandroid.graduation_project;

import com.google.firebase.database.IgnoreExtraProperties;


@IgnoreExtraProperties
public class UserData {
    public String name;
    public String email;


    public UserData() { }
    public UserData(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }




    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }



    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", email='" + email + '}';
    }
}