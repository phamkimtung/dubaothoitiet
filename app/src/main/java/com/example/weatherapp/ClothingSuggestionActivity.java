package com.example.weatherapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.weatherapp.service.ClothingSuggestionService;
import java.util.Locale;

public class ClothingSuggestionActivity extends AppCompatActivity {

    private TextView suggestionTextView;
    private ProgressBar progressBar;
    private Button retryButton;
    private ImageButton closeButton;
    private double currentTemp;
    private String weatherCondition;
    private String location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clothing_suggestion);

        // Lấy dữ liệu thời tiết từ Intent
        currentTemp = getIntent().getDoubleExtra("temperature", 25.0);
        weatherCondition = getIntent().getStringExtra("weather_condition");
        location = getIntent().getStringExtra("location");

        initViews();
        getClothingSuggestion();
    }

    private void initViews() {
        suggestionTextView = findViewById(R.id.suggestionTextView);
        progressBar = findViewById(R.id.progressBar);
        retryButton = findViewById(R.id.retryButton);
        closeButton = findViewById(R.id.closeButton);

        // Hiển thị thông tin thời tiết cơ bản
        String weatherInfo = String.format(Locale.getDefault(),
                "📍 %s\n🌡️ %.1f°C - %s", location, currentTemp, weatherCondition);
        TextView weatherInfoTextView = findViewById(R.id.weatherInfoTextView);
        weatherInfoTextView.setText(weatherInfo);

        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getClothingSuggestion();
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void getClothingSuggestion() {
        showLoading();

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    return ClothingSuggestionService.getClothingSuggestion(
                            currentTemp, weatherCondition, location, ClothingSuggestionActivity.this);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Lỗi khi gọi AI: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String suggestion) {
                hideLoading();
                if (suggestion != null && !suggestion.isEmpty()) {
                    suggestionTextView.setText(suggestion);
                } else {
                    suggestionTextView.setText("Không thể lấy gợi ý. Vui lòng thử lại.");
                    retryButton.setVisibility(View.VISIBLE);
                }
            }
        }.execute();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        suggestionTextView.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        suggestionTextView.setText("🤖 Đang kết nối với Gemini AI...");
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        suggestionTextView.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
    }
}