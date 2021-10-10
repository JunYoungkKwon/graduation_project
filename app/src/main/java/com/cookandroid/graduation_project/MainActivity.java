package com.cookandroid.graduation_project;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kakao.sdk.user.UserApiClient;
import com.kakao.sdk.user.model.Account;
import com.kakao.sdk.user.model.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private static final String TAG="사용자";
    private ImageButton btn_login, btn_login_out;
    private Button test_btn;

    private DatabaseReference mDatabase;
    Button save, read;
    EditText email, name, id;
    TextView data;
    int i = 1; //pk


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, ClassifierActivity.class);

        // Log.d("GET_KEYHASH",getKeyHash());

        btn_login = findViewById(R.id.btn_login);
        btn_login_out = findViewById(R.id.btn_login_out);

        btn_login.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                UserApiClient.getInstance().loginWithKakaoTalk(MainActivity.this,(oAuthToken, error) -> {
                    if (error != null) {
                        Log.e(TAG, "로그인 실패", error);
                    } else if (oAuthToken != null) {
                        Log.i(TAG, "로그인 성공(토큰) : " + oAuthToken.getAccessToken());

                        UserApiClient.getInstance().me((user, meError) -> {
                            if (meError != null) {
                                Log.e(TAG, "사용자 정보 요청 실패", meError);
                            } else {
                                System.out.println("로그인 완료");
                                Log.i(TAG, user.toString());
                                {
                                    Log.i(TAG, "사용자 정보 요청 성공" +
                                            "\n회원번호: "+user.getId() +
                                            "\n이메일: "+user.getKakaoAccount().getEmail());
                                }
                                Account user1 = user.getKakaoAccount();
                                System.out.println("사용자 계정" + user1);
                            }
                            return null;
                        });
                    }
                    return null;
                });

            }
        });

        /*
        btn_login_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                UserApiClient.getInstance().logout(error -> {
                    if (error != null) {
                        Log.e(TAG, "로그아웃 실패, SDK에서 토큰 삭제됨", error);
                    }else{
                        Log.e(TAG, "로그아웃 성공, SDK에서 토큰 삭제됨");
                    }
                    return null;
                });
            }
        }); */
        //테스트용으로 만듦!!!
        btn_login_out.setOnClickListener(view -> {
            startActivity(new Intent(this, HomeActivity.class));
        });


        //firebase test 부분
        mDatabase = FirebaseDatabase.getInstance().getReference(); //DatabaseReference의 인스턴스

        save = findViewById(R.id.submit);
        read = findViewById(R.id.read);
        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        id = findViewById(R.id.id);
        data = findViewById(R.id.data);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String getUserName = name.getText().toString();
                String getUserEmail = email.getText().toString();

                HashMap result = new HashMap<>();
                result.put("name", getUserName); //키, 값
                result.put("email", getUserEmail);

                writeUser(Integer.toString(i++), getUserName, getUserEmail);
            }
        });

        read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readUser(id.getText().toString());
            }
        });
    }

    // 키해시 얻기
    public String getKeyHash(){
        try{
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(),PackageManager.GET_SIGNATURES);
            if(packageInfo == null) return null;
            for(Signature signature: packageInfo.signatures){
                try{
                    MessageDigest md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    return android.util.Base64.encodeToString(md.digest(), Base64.NO_WRAP);
                }catch (NoSuchAlgorithmException e){
                    Log.w("getKeyHash", "Unable to get MessageDigest. signature="+signature, e);
                }
            }
        }catch(PackageManager.NameNotFoundException e){
            Log.w("getPackageInfo", "Unable to getPackageInfo");
        }
        return null;
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
