package com.example.weatherapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.weatherapp.api.WeatherApi;
import com.example.weatherapp.model.WeatherResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private ImageButton confirmButton, closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
    }

    private void initViews() {
        searchEditText = findViewById(R.id.searchEditText);
        confirmButton = findViewById(R.id.confirmButton);
        closeButton = findViewById(R.id.closeButton);

        // xử lý người dùng nhập thành phố
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cityName = searchEditText.getText().toString().trim();
                if (!cityName.isEmpty()) {
                    searchWeatherByCity(cityName);
                } else {
                    Toast.makeText(SearchActivity.this, "Vui lòng nhập tên thành phố", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Đóng Activity hiện tại
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    // tìm kiếm và trả về thời tiết teho tên thành phố
    private void searchWeatherByCity(String cityName) {
        WeatherApi weatherApi = com.example.weatherapp.api.ApiClient.getWeatherApi();

        Call<WeatherResponse> searchCall = weatherApi.searchWeatherByCity(
                cityName, WeatherApi.API_KEY, "metric", "vi");

        searchCall.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weather = response.body();
                    double lat = weather.getCoord().getLat();
                    double lon = weather.getCoord().getLon();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("lat", lat);
                    resultIntent.putExtra("lon", lon);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(SearchActivity.this,
                            "Không tìm thấy thành phố: " + cityName, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(SearchActivity.this,
                        "Lỗi tìm kiếm: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}