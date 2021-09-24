package com.devidea.timeleft;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro2;
import com.github.appintro.AppIntroCustomLayoutFragment;
import com.github.appintro.AppIntroFragment;

public class AppIntroActivity extends AppIntro2 {

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //최초접속시에만 intro화면 출력하도록
        sharedPreferences = getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        if (sharedPreferences != null) {
            boolean checkShared = sharedPreferences.getBoolean("checkStated", false);
            if (checkShared) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            }
        }

        setImmersiveMode();

        addSlide(AppIntroFragment.newInstance(
                "TimeLaft에 오신것을 환영합니다",
                "지금 이순간에도 지나가는 시간을 확인해보세요",
                R.drawable.description01,
                R.color.white,
                R.color.black,
                R.color.black,
                R.font.nanumsquareround,
                R.font.nanumsquareround,
                R.drawable.ic_launcher_background
        ));

        addSlide(AppIntroFragment.newInstance(
                "내가 원하는것 추가해보기",
                "내가 원하는 시간, 날까지의 흐름을 추가해보세요",
                R.drawable.description02,
                R.color.white,
                R.color.black,
                R.color.black,
                R.font.nanumsquareround,
                R.font.nanumsquareround,
                R.drawable.ic_launcher_background
        ));

        addSlide(AppIntroFragment.newInstance(
                "언제 어디서나 확인가능",
                "모든 항목에 대한 위젯을 만들수 있어요",
                R.drawable.description03,
                R.color.white,
                R.color.black,
                R.color.black,
                R.font.nanumsquareround,
                R.font.nanumsquareround,
                R.drawable.ic_launcher_background
        ));

    }

    @Override
    protected void onSkipPressed(@Nullable Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        editor.putBoolean("checkStated", true).commit();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }

    @Override
    protected void onDonePressed(@Nullable Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        editor.putBoolean("checkStated", true).commit();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }
}
