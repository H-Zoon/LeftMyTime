package com.devidea.timeleft;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro2;
import com.github.appintro.AppIntroCustomLayoutFragment;


public class AppIntroActivity extends AppIntro2 {

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showStatusBar(true);
        setIndicatorEnabled(false);

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
        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.intro_layout_01));
        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.intro_layout_02));
        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.intro_layout_03));

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
