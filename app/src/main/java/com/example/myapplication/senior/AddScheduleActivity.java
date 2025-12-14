package com.example.myapplication.senior;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.start.StartPageActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddScheduleActivity extends AppCompatActivity {

    private static final String TAG = "VoiceScheduleActivity";
    private static final int SPEECH_REQUEST_CODE = 100;

    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    private boolean isSpeaking = false;

    private DatabaseReference databaseReference;
    private String familyId;

    // UI 요소
    private MaterialButton btnSchedule;
    private Button btnStop;
    private TextView textView;

    // 음성 인식 단계
    private enum Step {
        WAITING,      // 대기 중
        LISTENING     // 음성 듣는 중
    }

    private Step currentStep = Step.WAITING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_schedule);

        databaseReference = FirebaseDatabase.getInstance().getReference("schedules");

        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        familyId = prefs.getString("familyId", null);

        // 뷰 초기화
        btnSchedule = findViewById(R.id.BtnSchedule);
        textView = findViewById(R.id.textView);
        btnStop = findViewById(R.id.btnStop);

        // TTS 초기화
        initializeTextToSpeech();

        // 음성 등록 시작 버튼
        btnSchedule.setOnClickListener(v -> {
            textView.setText("일정 추가 중");
            startVoiceRegistration();
        });

        MaterialButton btnStop2 = findViewById(R.id.VoulumOnBtn);
        btnStop2.setOnClickListener(v -> {
            stopSpeaking();
        });

        // TTS 중지 버튼
        btnStop.setOnClickListener(v -> {
            stopSpeaking();
        });

        // 취소 버튼
        Button btnCancel = findViewById(R.id.BtnExit);
        btnCancel.setOnClickListener(v -> {
            Intent intent = new Intent(AddScheduleActivity.this, ScheduleMainActivity.class);
            startActivity(intent);
        });
    }


    /**
     * TTS 초기화
     */
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.KOREAN);
                isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED);

                if (isTtsReady) {
                    // TTS 상태 리스너 추가
                    textToSpeech.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            isSpeaking = true;
                            runOnUiThread(() -> {
                                btnStop.setEnabled(true);
                                btnStop.setVisibility(Button.VISIBLE);
                            });
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            isSpeaking = false;
                            runOnUiThread(() -> {
                                btnStop.setEnabled(false);
                                btnStop.setVisibility(Button.VISIBLE);
                            });
                        }

                        @Override
                        public void onError(String utteranceId) {
                            isSpeaking = false;
                            runOnUiThread(() -> {
                                btnStop.setEnabled(false);
                                btnStop.setVisibility(Button.VISIBLE);
                            });
                        }
                    });

                    // 초기 안내 메시지
                    speak("음성으로 일정을 등록합니다. 년도, 월, 일, 일정 제목, 시간 순서로 말씀해주세요. " +
                            "예를 들어, 2025년, 12월 2일, 손녀 결혼식, 오후 6시 라고 말씀 하시면 됩니다.");
                }
            }
        });
    }

    /**
     * TTS 음성 중지
     */
    private void stopSpeaking() {
        if (textToSpeech != null && isSpeaking) {
            textToSpeech.stop();
            isSpeaking = false;
            btnStop.setEnabled(false);
            btnStop.setVisibility(Button.VISIBLE);
        }
    }

    /**
     * 음성 등록 시작
     */
    private void startVoiceRegistration() {
        speak("일정을 말씀해주세요.");

        // 0.5초 후 음성 인식 시작 (TTS가 끝날 시간 확보)
        new android.os.Handler().postDelayed(this::startSpeechRecognition, 500);
    }

    /**
     * 음성 인식 시작
     */
    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "일정을 말씀해주세요");
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);

        try {
            currentStep = Step.LISTENING;
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "음성 인식을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Speech recognition error: " + e.getMessage());
            textView.setText("일정 추가하기\n 버튼을 눌러\n일정을 알려주세요");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                Log.d(TAG, "인식된 음성: " + spokenText);



                processVoiceInput(spokenText);
                textView.setText("일정 추가하기\n 버튼을 눌러\n일정을 알려주세요");
            }
        } else {
            speak("음성 인식에 실패했습니다. 다시 시도해주세요.");
            currentStep = Step.WAITING;
            textView.setText("일정 추가하기\n 버튼을 눌러\n일정을 알려주세요");
        }
    }

    /**
     * 음성 입력 처리 및 일정 데이터 추출
     */
    private void processVoiceInput(String spokenText) {
        try {
            ScheduleData scheduleData = parseScheduleFromVoice(spokenText);

            if (scheduleData != null) {
                // 확인 메시지
                String confirmMessage = scheduleData.year + "년 " + scheduleData.month + "월 " +
                        scheduleData.day + "일 " + scheduleData.title + " " +
                        scheduleData.time + " 일정으로 등록하시겠습니까?";

                speak(confirmMessage);

                // Firebase에 저장
                saveScheduleToFirebase(scheduleData);
                textView.setText("일정 추가하기\n 버튼을 눌러\n일정을 알려주세요");

            } else {
                speak("일정 형식을 인식하지 못했습니다. 년도, 월, 일, 일정 제목, 시간 순서로 다시 말씀해주세요.");
                textView.setText("일정 추가하기\n 버튼을 눌러\n일정을 알려주세요");
            }

        } catch (Exception e) {
            Log.e(TAG, "음성 처리 오류: " + e.getMessage());
            speak("일정 처리 중 오류가 발생했습니다.");
            textView.setText("일정 추가하기\n 버튼을 눌러\n일정을 알려주세요");
        }

        currentStep = Step.WAITING;
    }

    /**
     * 음성에서 일정 데이터 파싱
     */
    private ScheduleData parseScheduleFromVoice(String text) {
        try {
            ScheduleData data = new ScheduleData();

            // 숫자로 된 년도 추출 (예: 2025년)
            Pattern yearPattern = Pattern.compile("(\\d{4})년");
            Matcher yearMatcher = yearPattern.matcher(text);
            if (yearMatcher.find()) {
                data.year = yearMatcher.group(1);
            }

            // 월 추출 (예: 12월, 십이월)
            Pattern monthPattern = Pattern.compile("(\\d{1,2})월|([일이삼사오육칠팔구십]{1,3})월");
            Matcher monthMatcher = monthPattern.matcher(text);
            if (monthMatcher.find()) {
                String month = monthMatcher.group(1);
                if (month != null) {
                    data.month = String.format("%02d", Integer.parseInt(month));
                } else {
                    // 한글 숫자를 아라비아 숫자로 변환
                    String koreanMonth = monthMatcher.group(2);
                    data.month = String.format("%02d", convertKoreanToNumber(koreanMonth));
                }
            }

            // 일 추출 (예: 25일, 이십오일)
            Pattern dayPattern = Pattern.compile("(\\d{1,2})일|([일이삼사오육칠팔구십]{1,4})일");
            Matcher dayMatcher = dayPattern.matcher(text);
            if (dayMatcher.find()) {
                String day = dayMatcher.group(1);
                if (day != null) {
                    data.day = String.format("%02d", Integer.parseInt(day));
                } else {
                    String koreanDay = dayMatcher.group(2);
                    data.day = String.format("%02d", convertKoreanToNumber(koreanDay));
                }
            }

            // 시간 추출 (예: 오후 6시, 18시, 6시 30분)
            data.time = extractTime(text);

            // 일정 제목 추출 (년월일과 시간 사이의 텍스트)
            data.title = extractTitle(text);

            // 필수 정보 확인
            if (data.year != null && data.month != null && data.day != null &&
                    data.title != null && data.time != null) {
                data.date = data.year + "년 " + data.month + "월 " + data.day + "일";
                return data;
            }

        } catch (Exception e) {
            Log.e(TAG, "파싱 오류: " + e.getMessage());
        }

        return null;
    }

    /**
     * 시간 추출
     */
    private String extractTime(String text) {
        // 오전/오후 + 시간 (예: 오후 6시, 오전 10시 30분)
        Pattern timePattern = Pattern.compile("(오전|오후)\\s*(\\d{1,2})시\\s*(\\d{1,2}분)?");
        Matcher timeMatcher = timePattern.matcher(text);

        if (timeMatcher.find()) {
            String period = timeMatcher.group(1);
            int hour = Integer.parseInt(timeMatcher.group(2));
            String minute = timeMatcher.group(3);

            // 오후면 12 더하기 (단, 12시는 그대로)
            if (period.equals("오후") && hour != 12) {
                hour += 12;
            } else if (period.equals("오전") && hour == 12) {
                hour = 0;
            }

            if (minute != null) {
                minute = minute.replace("분", "");
                return String.format("%02d:%s", hour, minute);
            } else {
                return String.format("%02d:00", hour);
            }
        }

        // 24시간 형식 (예: 18시, 18시 30분)
        Pattern time24Pattern = Pattern.compile("(\\d{1,2})시\\s*(\\d{1,2}분)?");
        Matcher time24Matcher = time24Pattern.matcher(text);

        if (time24Matcher.find()) {
            int hour = Integer.parseInt(time24Matcher.group(1));
            String minute = time24Matcher.group(2);

            if (minute != null) {
                minute = minute.replace("분", "");
                return String.format("%02d:%s", hour, minute);
            } else {
                return String.format("%02d:00", hour);
            }
        }

        return "00:00";
    }

    /**
     * 일정 제목 추출
     */
    private String extractTitle(String text) {
        // 날짜와 시간 패턴 제거하고 남은 텍스트를 제목으로
        String title = text;

        // 년월일 패턴 제거
        title = title.replaceAll("\\d{4}년", "");
        title = title.replaceAll("(\\d{1,2}|[일이삼사오육칠팔구십]{1,3})월", "");
        title = title.replaceAll("(\\d{1,2}|[일이삼사오육칠팔구십]{1,4})일", "");

        // 시간 패턴 제거
        title = title.replaceAll("(오전|오후)\\s*\\d{1,2}시\\s*(\\d{1,2}분)?", "");
        title = title.replaceAll("\\d{1,2}시\\s*(\\d{1,2}분)?", "");

        // 공백 정리
        title = title.trim().replaceAll("\\s+", " ");

        return title.isEmpty() ? "일정" : title;
    }

    /**
     * 한글 숫자를 아라비아 숫자로 변환
     */
    private int convertKoreanToNumber(String korean) {
        Map<String, Integer> koreanNumbers = new HashMap<>();
        koreanNumbers.put("일", 1);
        koreanNumbers.put("이", 2);
        koreanNumbers.put("삼", 3);
        koreanNumbers.put("사", 4);
        koreanNumbers.put("오", 5);
        koreanNumbers.put("육", 6);
        koreanNumbers.put("칠", 7);
        koreanNumbers.put("팔", 8);
        koreanNumbers.put("구", 9);
        koreanNumbers.put("십", 10);

        int result = 0;
        int temp = 0;

        for (int i = 0; i < korean.length(); i++) {
            String ch = String.valueOf(korean.charAt(i));
            Integer num = koreanNumbers.get(ch);

            if (num != null) {
                if (num == 10) {
                    temp = (temp == 0) ? 10 : temp * 10;
                } else {
                    temp += num;
                }
            }
        }

        result += temp;
        return result;
    }

    /**
     * Firebase에 일정 저장
     */
    private void saveScheduleToFirebase(ScheduleData scheduleData) {
        String scheduleId = databaseReference.push().getKey();

        if (scheduleId != null) {
            Map<String, Object> schedule = new HashMap<>();
            schedule.put("title", scheduleData.title);
            schedule.put("date", scheduleData.date);
            schedule.put("time", scheduleData.time);
            schedule.put("familyId", familyId);

            databaseReference.child(scheduleId).setValue(schedule)
                    .addOnSuccessListener(aVoid -> {
                        speak("일정이 등록되었습니다.");

                        // 2초 후 화면 종료
                        new android.os.Handler().postDelayed(() -> {
                            setResult(RESULT_OK);
                            finish();
                        }, 2000);
                    })
                    .addOnFailureListener(e -> {
                        speak("일정 등록에 실패했습니다.");
                        Log.e(TAG, "Firebase 저장 실패: " + e.getMessage());
                    });
        }
    }

    /**
     * TTS 음성 출력
     */
    private void speak(String text) {
        if (isTtsReady && textToSpeech != null) {
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "tts_" + System.currentTimeMillis());
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 화면이 백그라운드로 가면 음성 중지
        stopSpeaking();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    /**
     * 일정 데이터 클래스
     */
    private static class ScheduleData {
        String year;
        String month;
        String day;
        String date;
        String title;
        String time;
    }
}