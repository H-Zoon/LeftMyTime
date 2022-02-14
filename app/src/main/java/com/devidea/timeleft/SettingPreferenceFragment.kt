package com.devidea.timeleft

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.util.Log
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat.getSystemService
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.devidea.timeleft.activity.MainActivity

class SettingPreferenceFragment : PreferenceFragmentCompat() {
    // 저장되는 데이터에 접근하기 위한 SharedPreferences
    lateinit var prefs: SharedPreferences

    // Preference 객체
    var themePreference: Preference? = null

    //var languagePreference: Preference? = null
    var pausePreference: Preference? = null

    var powerPreference: Preference? = null

    // onCreate() 중에 호출되어 Fragment에 preference를 제공하는 메서드
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // preference xml을 inflate하는 메서드
        setPreferencesFromResource(R.xml.settings_preference, rootKey)

        // rootKey가 null 이라면
        if (rootKey == null) {
            // Preference 객체 초기화
            themePreference = findPreference("theme")
            //languagePreference = findPreference("language")
            pausePreference = findPreference("pause")
            powerPreference = findPreference("power_service")

            // SharedPreferences 객체 초기화
            prefs = PreferenceManager.getDefaultSharedPreferences(App.context())

            // sound_list라는 key로 저장된 값이 비어있지 않다면
            if (prefs.getString("theme", "") != "") {
                // ListPreference의 summary를 sound_list key에 해당하는 값으로 설정
                themePreference?.summary = prefs.getString("theme", "")
            }

            when(prefs.getBoolean("power_service", false)) {
                false -> {
                    powerPreference?.title = "더 정확한 알림과 위젯활용"
                    powerPreference?.summary = "TimeLeft의 알람 기능과 위젯 업데이트를 위해 배터리 최적화를 해제하여야 합니다."
                    powerPreference?.isEnabled = true
                    powerPreference?.isSelectable = true
                }

                true -> {
                    powerPreference?.title = "최적화됨"
                    powerPreference?.summary = "TimeLeft가 최적화 되었습니다."
                    powerPreference?.isEnabled = false
                    powerPreference?.isSelectable = false
                }
            }
        }
    }

    // 설정 변경 이벤트 처리
    val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences?, key: String? ->
            Log.d("s",key.toString())
            Log.d("s",sharedPreferences.toString())
            when (key) {
                "power_service" -> {
                    val intent = Intent()
                    intent.action = ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    intent.data = Uri.parse("package:com.devidea.timeleft")
                    startActivity(intent)

                }

                "theme" -> {
                    when (prefs.getString("theme", "")) {
                        "밝게" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

                        "어둡게" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

                        else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

                    }

                }
                "pause" ->{

                }
            }
        }

    // 리스너 등록
    override fun onResume() {
        super.onResume()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    // 리스너 해제
    override fun onPause() {
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}