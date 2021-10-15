package com.cookandroid.graduation_project;

import android.app.Application;
import com.kakao.sdk.common.KakaoSdk;


public class GlobalApplication extends Application {
    private static GlobalApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 네이티브 앱 키로 초기화
        KakaoSdk.init(this, "d7cce7b9b862966d17b63bae9ad6b109");
    }
}