package com.example.weatherapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.weatherapp.api.WeatherApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AirQualityActivity extends AppCompatActivity {

    private TextView tvAqiValue;
    private TextView tvAqiLevel;
    private TextView tvAqiDescription;
    private TextView tvCo, tvNo2, tvO3, tvSo2, tvPm25, tvPm10;
    private ProgressBar pbAqi;
    private TextView tvLastUpdated;
    private Button btnBack;

    private double lat;
    private double lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_quality);

        lat = getIntent().getDoubleExtra("lat", 21.0285);
        lon = getIntent().getDoubleExtra("lon", 105.8542);

        initViews();
        loadAirQualityData();
    }

    private void initViews() {
        tvAqiValue = findViewById(R.id.tvAqiValue);
        tvAqiLevel = findViewById(R.id.tvAqiLevel);
        tvAqiDescription = findViewById(R.id.tvAqiDescription);
        tvCo = findViewById(R.id.tvCo);
        tvNo2 = findViewById(R.id.tvNo2);
        tvO3 = findViewById(R.id.tvO3);
        tvSo2 = findViewById(R.id.tvSo2);
        tvPm25 = findViewById(R.id.tvPm25);
        tvPm10 = findViewById(R.id.tvPm10);
        pbAqi = findViewById(R.id.pbAqi);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        btnBack = findViewById(R.id.backButton);

        btnBack.setOnClickListener(v -> finish());

        // Show loading state
        showLoadingData();
    }

    private void showLoadingData() {
        tvAqiValue.setText("--");
        tvAqiLevel.setText("Đang tải...");
        tvAqiDescription.setText("Đang lấy dữ liệu chất lượng không khí");
        tvCo.setText("-- µg/m³");
        tvNo2.setText("-- µg/m³");
        tvO3.setText("-- µg/m³");
        tvSo2.setText("-- µg/m³");
        tvPm25.setText("-- µg/m³");
        tvPm10.setText("-- µg/m³");
        pbAqi.setProgress(0);
        tvLastUpdated.setText("Đang cập nhật...");
    }

    private void loadAirQualityData() {
        try {
            // Sử dụng weatherAPI.com
            WeatherApi weatherApiService = com.example.weatherapp.api.ApiClient.getWeatherApiComService();

            // Tạo query string từ lat,lon
            String query = lat + "," + lon;

            Log.d("AirQuality", "Calling weatherAPI.com with query: " + query);
            Log.d("AirQuality", "API Key: " + WeatherApi.WEATHER_API_KEY);
            Log.d("AirQuality", "Base URL: " + WeatherApi.WEATHER_API_BASE_URL);

            // Gọi API chất lượng không khí từ weatherAPI.com
            Call<WeatherApi.WeatherApiAirQualityResponse> call = weatherApiService.getWeatherApiAirQuality(
                    WeatherApi.WEATHER_API_KEY,
                    query,
                    "yes" // Bật chế độ lấy AQI
            );

            call.enqueue(new Callback<WeatherApi.WeatherApiAirQualityResponse>() {
                @Override
                public void onResponse(Call<WeatherApi.WeatherApiAirQualityResponse> call,
                                       Response<WeatherApi.WeatherApiAirQualityResponse> response) {

                    Log.d("AirQuality", "Response code: " + response.code());
                    Log.d("AirQuality", "Response success: " + response.isSuccessful());

                    if (response.isSuccessful() && response.body() != null) {
                        Log.d("AirQuality", "Response received successfully");
                        Log.d("AirQuality", "Response body: " + response.body().toString());

                        if (response.body().getCurrent() != null) {
                            Log.d("AirQuality", "Current data available");

                            if (response.body().getCurrent().getAir_quality() != null) {
                                Log.d("AirQuality", "Air quality data available");
                                updateAirQualityUI(response.body());
                            } else {
                                Log.d("AirQuality", "Air quality data is null - checking if we can get basic info");
                                // Thử lấy dữ liệu cơ bản nếu air_quality null
                                showAlternativeData(response.body());
                            }
                        } else {
                            Log.d("AirQuality", "Current data is null");
                            showSampleData("Không có dữ liệu thời tiết");
                        }
                    } else {
                        String errorMsg = "Lỗi API: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                errorMsg = "Lỗi: " + response.errorBody().string();
                            }
                        } catch (Exception e) {
                            Log.e("AirQuality", "Error reading error body", e);
                        }
                        Log.e("AirQuality", errorMsg);
                        showSampleData(errorMsg);
                    }
                }

                @Override
                public void onFailure(Call<WeatherApi.WeatherApiAirQualityResponse> call, Throwable t) {
                    Log.e("AirQuality", "API call failed: " + t.getMessage());
                    t.printStackTrace();
                    showSampleData("Lỗi kết nối: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e("AirQuality", "Exception in loadAirQualityData: " + e.getMessage());
            e.printStackTrace();
            showSampleData("Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void updateAirQualityUI(WeatherApi.WeatherApiAirQualityResponse response) {
        if (response == null || response.getCurrent() == null) {
            showSampleData("Không có dữ liệu response");
            return;
        }

        WeatherApi.WeatherApiAirQualityResponse.Current.AirQuality airQuality =
                response.getCurrent().getAir_quality();

        if (airQuality == null) {
            showAlternativeData(response);
            return;
        }

        // Lấy chỉ số AQI (sử dụng US EPA Index từ weatherAPI.com)
        int aqi = airQuality.getUs_epa_index();
        Log.d("AirQuality", "AQI Value: " + aqi);

        // Update AQI value
        tvAqiValue.setText(String.valueOf(aqi));

        // Update progress bar (AQI từ 1-6 cho US EPA Index)
        int progress = Math.min((int) ((aqi / 6.0) * 100), 100);
        pbAqi.setProgress(progress);

        // Update last updated time
        String lastUpdated = response.getCurrent().getLast_updated();
        if (lastUpdated != null && !lastUpdated.isEmpty()) {
            tvLastUpdated.setText("Cập nhật: " + lastUpdated);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
            tvLastUpdated.setText("Cập nhật: " + sdf.format(new Date()));
        }

        // Set AQI level và màu (dựa trên US EPA Index)
        setAqiLevel(aqi);

        // Update pollutant values
        updatePollutantValues(airQuality);
    }

    private void showAlternativeData(WeatherApi.WeatherApiAirQualityResponse response) {
        // Hiển thị dữ liệu thay thế nếu không có air_quality
        Log.d("AirQuality", "Showing alternative data");

        // Sử dụng dữ liệu mẫu nhưng có thông tin từ response nếu có
        int aqi = 2; // Mặc định trung bình

        tvAqiValue.setText(String.valueOf(aqi));
        pbAqi.setProgress(33);

        // Update last updated time
        if (response.getCurrent() != null && response.getCurrent().getLast_updated() != null) {
            tvLastUpdated.setText("Cập nhật: " + response.getCurrent().getLast_updated());
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
            tvLastUpdated.setText("Cập nhật: " + sdf.format(new Date()) + " (Dữ liệu mẫu)");
        }

        setAqiLevel(aqi);
        showDefaultPollutantValues();

        tvAqiDescription.setText("Dữ li chất lượng không khí đang được cập nhật");
    }

    private void updatePollutantValues(WeatherApi.WeatherApiAirQualityResponse.Current.AirQuality airQuality) {
        // Hiển thị giá trị ô nhiễm, nếu null thì hiển thị "N/A"
        if (airQuality.getCo() > 0) {
            tvCo.setText(String.format(Locale.getDefault(), "%.1f µg/m³", airQuality.getCo()));
        } else {
            tvCo.setText("N/A");
        }

        if (airQuality.getNo2() > 0) {
            tvNo2.setText(String.format(Locale.getDefault(), "%.1f µg/m³", airQuality.getNo2()));
        } else {
            tvNo2.setText("N/A");
        }

        if (airQuality.getO3() > 0) {
            tvO3.setText(String.format(Locale.getDefault(), "%.1f µg/m³", airQuality.getO3()));
        } else {
            tvO3.setText("N/A");
        }

        if (airQuality.getSo2() > 0) {
            tvSo2.setText(String.format(Locale.getDefault(), "%.1f µg/m³", airQuality.getSo2()));
        } else {
            tvSo2.setText("N/A");
        }

        if (airQuality.getPm2_5() > 0) {
            tvPm25.setText(String.format(Locale.getDefault(), "%.1f µg/m³", airQuality.getPm2_5()));
        } else {
            tvPm25.setText("N/A");
        }

        if (airQuality.getPm10() > 0) {
            tvPm10.setText(String.format(Locale.getDefault(), "%.1f µg/m³", airQuality.getPm10()));
        } else {
            tvPm10.setText("N/A");
        }
    }

    private void showDefaultPollutantValues() {
        // Hiển thị giá trị mẫu cho các chất ô nhiễm
        tvCo.setText("0.5 µg/m³");
        tvNo2.setText("15.2 µg/m³");
        tvO3.setText("45.6 µg/m³");
        tvSo2.setText("3.2 µg/m³");
        tvPm25.setText("12.5 µg/m³");
        tvPm10.setText("25.3 µg/m³");
    }

    private void setAqiLevel(int usEpaIndex) {
        // US EPA Index: 1=Good, 2=Moderate, 3=Unhealthy for sensitive,
        // 4=Unhealthy, 5=Very Unhealthy, 6=Hazardous

        String level;
        String description;
        int color;

        if (usEpaIndex >= 1 && usEpaIndex <= 6) {
            switch (usEpaIndex) {
                case 1:
                    level = "Tốt";
                    description = "Chất lượng không khí tốt";
                    color = Color.parseColor("#4CAF50"); // Green
                    break;
                case 2:
                    level = "Trung bình";
                    description = "Chất lượng không khí chấp nhận được";
                    color = Color.parseColor("#FFC107"); // Yellow
                    break;
                case 3:
                    level = "Kém";
                    description = "Có thể ảnh hưởng nhóm nhạy cảm";
                    color = Color.parseColor("#FF9800"); // Orange
                    break;
                case 4:
                    level = "Xấu";
                    description = "Ảnh hưởng sức khỏe mọi người";
                    color = Color.parseColor("#F44336"); // Red
                    break;
                case 5:
                    level = "Rất xấu";
                    description = "Cảnh báo sức khỏe";
                    color = Color.parseColor("#8B0000"); // Dark Red
                    break;
                case 6:
                    level = "Nguy hiểm";
                    description = "Cảnh báo sức khỏe khẩn cấp";
                    color = Color.parseColor("#660000"); // Very Dark Red
                    break;
                default:
                    level = "Không xác định";
                    description = "Dữ liệu không hợp lệ";
                    color = Color.parseColor("#666666"); // Gray
                    break;
            }
        } else {
            // Nếu AQI ngoài phạm vi 1-6
            level = "Không xác định";
            description = "Chỉ số AQI không hợp lệ: " + usEpaIndex;
            color = Color.parseColor("#666666"); // Gray
        }

        tvAqiLevel.setText(level);
        tvAqiLevel.setTextColor(color);
        tvAqiValue.setTextColor(color);
        tvAqiDescription.setText(description);
        pbAqi.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void showSampleData(String errorMessage) {
        Log.d("AirQuality", "Showing sample data: " + errorMessage);

        // Hiển thị dữ liệu mẫu với thông báo lỗi
        int aqi = 2; // Trung bình

        tvAqiValue.setText(String.valueOf(aqi));
        tvAqiLevel.setText("Trung bình");
        tvAqiDescription.setText("Chất lượng không khí chấp nhận được");
        showDefaultPollutantValues();
        pbAqi.setProgress(33);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
        tvLastUpdated.setText("Dữ liệu mẫu - " + sdf.format(new Date()) + " (" + errorMessage + ")");

        // Set màu vàng cho AQI trung bình
        int color = Color.parseColor("#FFC107");
        tvAqiValue.setTextColor(color);
        tvAqiLevel.setTextColor(color);
        pbAqi.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    }
}