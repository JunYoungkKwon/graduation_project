package com.cookandroid.graduation_project;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.user.UserApiClient;
import com.kakao.sdk.user.model.User;

import java.util.HashMap;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    TextView data;
    int i = 1; //pk

    private View loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = findViewById(R.id.login);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(UserApiClient.getInstance().isKakaoTalkLoginAvailable(MainActivity.this)){
                    UserApiClient.getInstance().loginWithKakaoTalk(MainActivity.this, new Function2<OAuthToken, Throwable, Unit>() {
                        @Override
                        public Unit invoke(OAuthToken oAuthToken, Throwable throwable) {
                            if(oAuthToken != null){
                                //TODO:예외처리 할 것
                            }
                            if(throwable != null){
                                //TODO:예외처리 할 것
                            }
                            updateKakaoLoginUi();
                            return null;
                        }
                    });
                }
                else{
                    UserApiClient.getInstance().loginWithKakaoAccount(MainActivity.this, new Function2<OAuthToken, Throwable, Unit>() {
                        @Override
                        public Unit invoke(OAuthToken oAuthToken, Throwable throwable) {
                            if(oAuthToken != null){
                                //TODO:예외처리 할 것
                            }
                            if(throwable != null){
                                //TODO:예외처리 할 것
                            }
                            updateKakaoLoginUi();
                            return null;
                        }
                    });
                }
            }
        });

        //firebase test 부분
        mDatabase = FirebaseDatabase.getInstance().getReference(); //DatabaseReference의 인스턴스
    }

    private void updateKakaoLoginUi(){
        UserApiClient.getInstance().me(new Function2<User, Throwable, Unit>() {
            @Override
            public Unit invoke(User user, Throwable throwable) {

                HashMap result = new HashMap<>();
                result.put("name", user.getKakaoAccount().getProfile().getNickname()); //키, 값
                result.put("email", user.getKakaoAccount().getEmail());

                writeUser(Integer.toString(i++), user.getKakaoAccount().getProfile().getNickname(), user.getKakaoAccount().getEmail());

                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                intent.putExtra("name", user.getKakaoAccount().getProfile().getNickname());
                intent.putExtra("email", user.getKakaoAccount().getEmail());
                startActivity(intent);

                return null;

            }
        });
    }

    //firebase 데이터 읽고 쓰기
    private void writeUser(String userId, String name, String email) {
        UserData user =  new UserData(name,email);

        //데이터 저장
        mDatabase.child("users").child(userId).setValue(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() { //데이터베이스에 넘어간 이후 처리
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(),"저장을 완료했습니다", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(),"저장에 실패했습니다" , Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void readUser(String userId) {
        //데이터 읽기
        mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserData user = snapshot.getValue(UserData.class);
                data.setText("이름: " + user.name + " 이메일: " + user.email);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { //참조에 액세스 할 수 없을 때 호출
                Toast.makeText(getApplicationContext(),"데이터를 가져오는데 실패했습니다" , Toast.LENGTH_LONG).show();
            }
        });
    }
}