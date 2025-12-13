package com.example.myapplication.senior;

import android.content.Context;

public class TtsStateManager {

    private static final String PREF_NAME = "tts_pref";
    private static final String KEY_TTS_ENABLED = "tts_enabled";

    // 현재 TTS 상태 가져오기
    public static boolean isTtsEnabled(Context context) {
        return context
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_TTS_ENABLED, true); // 기본값 ON
    }

    // TTS 상태 저장
    public static void setTtsEnabled(Context context, boolean enabled) {
        context
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_TTS_ENABLED, enabled)
                .apply();
    }
}