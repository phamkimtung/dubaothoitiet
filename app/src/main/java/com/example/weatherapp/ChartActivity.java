package com.example.weatherapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.weatherapp.api.WeatherApi;
import com.example.weatherapp.model.ForecastResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChartActivity extends AppCompatActivity {

    private TextView chartTitle;
    private Button btnTempChart;
    private Button btnRainChart;
    private LinearLayout chartContainer;
    private TextView tvTempData;
    private TextView tvRainData;
    private double lat;
    private double lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_simple);

        lat = getIntent().getDoubleExtra("lat", 21.0285);
        lon = getIntent().getDoubleExtra("lon", 105.8542);

        initViews();
        loadForecastData();
    }

    private void initViews() {
        chartTitle = findViewById(R.id.chartTitle);
        btnTempChart = findViewById(R.id.btnTempChart);
        btnRainChart = findViewById(R.id.btnRainChart);
        chartContainer = findViewById(R.id.chartContainer);
        tvTempData = findViewById(R.id.tvTempData);
        tvRainData = findViewById(R.id.tvRainData);

        Button btnBack = findViewById(R.id.backButton);
        btnBack.setOnClickListener(v -> finish());

        btnTempChart.setOnClickListener(v -> showTemperatureChart());
        btnRainChart.setOnClickListener(v -> showRainChart());

        showTemperatureChart();
    }

    private void loadForecastData() {
        WeatherApi weatherApi = com.example.weatherapp.api.ApiClient.getWeatherApi();
        Call<ForecastResponse> call = weatherApi.getForecast(
                lat, lon, WeatherApi.API_KEY, "metric", "vi");

        call.enqueue(new Callback<ForecastResponse>() {
            @Override
            public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateChartData(response.body());
                } else {
                    showSampleData();
                }
            }

            @Override
            public void onFailure(Call<ForecastResponse> call, Throwable t) {
                showSampleData();
            }
        });
    }

    private void updateChartData(ForecastResponse forecast) {
        List<ForecastResponse.ForecastItem> items = forecast.getList();
        StringBuilder tempData = new StringBuilder();
        StringBuilder rainData = new StringBuilder();

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        tempData.append("🌡️ NHIỆT ĐỘ 12H TỚI:\n\n");
        rainData.append("🌧️ DỰ BÁO MƯA 12H TỚI:\n\n");

        int count = 0;
        long currentTime = System.currentTimeMillis() / 1000;

        for (ForecastResponse.ForecastItem item : items) {
            if (count >= 12) break;

            if (item.getDt() > currentTime) {
                String time = timeFormat.format(new Date(item.getDt() * 1000));

                // Temperature data với emoji
                double temp = item.getMain().getTemp();
                String tempEmoji = getTemperatureEmoji(temp);
                String tempLevel = getTemperatureLevel(temp);
                tempData.append(String.format(Locale.getDefault(),
                        "• %s: %.1f°C %s (%s)\n", time, temp, tempEmoji, tempLevel));

                // Rain data với tỉ lệ khả năng mưa
                String weatherInfo = "Không mưa";
                String rainEmoji = "☀️";
                int rainProbability = 0;
                String rainLevel = "Thấp";

                if (item.getWeather() != null && !item.getWeather().isEmpty()) {
                    String weatherMain = item.getWeather().get(0).getMain().toLowerCase();
                    String weatherDesc = item.getWeather().get(0).getDescription().toLowerCase();

                    // Xác định tỉ lệ khả năng mưa dựa trên mô tả thời tiết
                    if (weatherMain.contains("rain")) {
                        if (weatherDesc.contains("nhẹ") || weatherDesc.contains("light")) {
                            weatherInfo = "Mưa nhẹ";
                            rainEmoji = "🌦️";
                            rainProbability = 30;
                            rainLevel = "Trung bình";
                        } else if (weatherDesc.contains("vừa") || weatherDesc.contains("moderate")) {
                            weatherInfo = "Mưa vừa";
                            rainEmoji = "🌧️";
                            rainProbability = 60;
                            rainLevel = "Cao";
                        } else if (weatherDesc.contains("to") || weatherDesc.contains("heavy")) {
                            weatherInfo = "Mưa to";
                            rainEmoji = "⛈️";
                            rainProbability = 80;
                            rainLevel = "Rất cao";
                        } else {
                            weatherInfo = "Có mưa";
                            rainEmoji = "🌧️";
                            rainProbability = 50;
                            rainLevel = "Cao";
                        }
                    } else if (weatherMain.contains("drizzle")) {
                        weatherInfo = "Mưa phùn";
                        rainEmoji = "🌦️";
                        rainProbability = 40;
                        rainLevel = "Trung bình";
                    } else if (weatherMain.contains("thunderstorm")) {
                        weatherInfo = "Giông bão";
                        rainEmoji = "⛈️";
                        rainProbability = 70;
                        rainLevel = "Rất cao";
                    } else if (weatherMain.contains("cloud")) {
                        if (weatherDesc.contains("nhiều") || weatherDesc.contains("broken") || weatherDesc.contains("overcast")) {
                            weatherInfo = "Nhiều mây";
                            rainEmoji = "☁️";
                            rainProbability = 20;
                            rainLevel = "Thấp";
                        } else {
                            weatherInfo = "Ít mây";
                            rainEmoji = "⛅";
                            rainProbability = 10;
                            rainLevel = "Rất thấp";
                        }
                    } else if (weatherMain.contains("clear")) {
                        weatherInfo = "Trời quang";
                        rainEmoji = "☀️";
                        rainProbability = 5;
                        rainLevel = "Rất thấp";
                    } else if (weatherMain.contains("snow")) {
                        weatherInfo = "Tuyết";
                        rainEmoji = "❄️";
                        rainProbability = 0;
                        rainLevel = "Không";
                    }
                }

                // Thêm thanh tiến trình cho khả năng mưa
                String rainBar = getRainBar(rainProbability);
                rainData.append(String.format("• %s: %s %s\n", time, weatherInfo, rainEmoji));
                rainData.append(String.format("  ↳ Khả năng mưa: %d%% %s (%s)\n",
                        rainProbability, rainBar, rainLevel));

                count++;
            }
        }

        // Thêm tổng kết
        rainData.append("\n📊 TỔNG KẾT KHẢ NĂNG MƯA:\n");
        rainData.append("• 0-20%: Khả năng thấp\n");
        rainData.append("• 21-50%: Khả năng trung bình\n");
        rainData.append("• 51-80%: Khả năng cao\n");
        rainData.append("• 81-100%: Khả năng rất cao\n");

        tvTempData.setText(tempData.toString());
        tvRainData.setText(rainData.toString());
    }

    private String getTemperatureEmoji(double temp) {
        if (temp > 35) return "🔥";
        else if (temp > 30) return "🥵";
        else if (temp > 25) return "☀️";
        else if (temp > 20) return "😊";
        else if (temp > 15) return "⛅";
        else if (temp > 10) return "🧥";
        else if (temp > 5) return "❄️";
        else return "🥶";
    }

    private String getTemperatureLevel(double temp) {
        if (temp > 35) return "Rất nóng";
        else if (temp > 30) return "Nóng";
        else if (temp > 25) return "Ấm áp";
        else if (temp > 20) return "Dễ chịu";
        else if (temp > 15) return "Mát mẻ";
        else if (temp > 10) return "Hơi lạnh";
        else if (temp > 5) return "Lạnh";
        else return "Rất lạnh";
    }

    private String getRainBar(int probability) {
        StringBuilder bar = new StringBuilder();
        int filled = probability / 10; // Mỗi 10% = 1 ô

        // Thêm ô đã điền
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }

        // Thêm ô trống
        for (int i = filled; i < 10; i++) {
            bar.append("░");
        }

        return bar.toString();
    }

    private void showSampleData() {
        String tempData = "🌡️ NHIỆT ĐỘ 12H TỚI (DỮ LIỆU MẪU):\n\n";
        tempData += "• 06:00: 22°C 🌅 (Dễ chịu)\n";
        tempData += "• 07:00: 23°C ⛅ (Dễ chịu)\n";
        tempData += "• 08:00: 24°C ☀️ (Dễ chịu)\n";
        tempData += "• 09:00: 25°C ☀️ (Ấm áp)\n";
        tempData += "• 10:00: 26°C ☀️ (Ấm áp)\n";
        tempData += "• 11:00: 27°C 🔥 (Ấm áp)\n";
        tempData += "• 12:00: 28°C 🔥 (Ấm áp)\n";
        tempData += "• 13:00: 29°C 🔥 (Nóng)\n";
        tempData += "• 14:00: 30°C 🔥 (Nóng)\n";
        tempData += "• 15:00: 29°C ☀️ (Nóng)\n";
        tempData += "• 16:00: 28°C ☀️ (Ấm áp)\n";
        tempData += "• 17:00: 27°C 🌇 (Ấm áp)\n";

        String rainData = "🌧️ DỰ BÁO MƯA 12H TỚI (DỮ LIỆU MẪU):\n\n";
        rainData += "• 06:00: Trời quang ☀️\n";
        rainData += "  ↳ Khả năng mưa: 5% ░░░░░░░░░░ (Rất thấp)\n";
        rainData += "• 07:00: Trời quang ☀️\n";
        rainData += "  ↳ Khả năng mưa: 5% ░░░░░░░░░░ (Rất thấp)\n";
        rainData += "• 08:00: Ít mây ⛅\n";
        rainData += "  ↳ Khả năng mưa: 10% ░░░░░░░░░░ (Rất thấp)\n";
        rainData += "• 09:00: Ít mây ⛅\n";
        rainData += "  ↳ Khả năng mưa: 15% ░░░░░░░░░░ (Thấp)\n";
        rainData += "• 10:00: Nhiều mây ☁️\n";
        rainData += "  ↳ Khả năng mưa: 20% ░░░░░░░░░░ (Thấp)\n";
        rainData += "• 11:00: Mưa nhẹ 🌦️\n";
        rainData += "  ↳ Khả năng mưa: 40% ████░░░░░░ (Trung bình)\n";
        rainData += "• 12:00: Có mưa 🌧️\n";
        rainData += "  ↳ Khả năng mưa: 60% ██████░░░░ (Cao)\n";
        rainData += "• 13:00: Mưa vừa 🌧️\n";
        rainData += "  ↳ Khả năng mưa: 70% ███████░░░ (Cao)\n";
        rainData += "• 14:00: Mưa to ⛈️\n";
        rainData += "  ↳ Khả năng mưa: 85% ████████░░ (Rất cao)\n";
        rainData += "• 15:00: Mưa nhẹ 🌦️\n";
        rainData += "  ↳ Khả năng mưa: 50% █████░░░░░ (Trung bình)\n";
        rainData += "• 16:00: Nhiều mây ☁️\n";
        rainData += "  ↳ Khả năng mưa: 25% ██░░░░░░░░ (Thấp)\n";
        rainData += "• 17:00: Ít mây 🌤️\n";
        rainData += "  ↳ Khả năng mưa: 15% ░░░░░░░░░░ (Thấp)\n";

        rainData += "\n📊 TỔNG KẾT KHẢ NĂNG MƯA:\n";
        rainData += "• 0-20%: Khả năng thấp\n";
        rainData += "• 21-50%: Khả năng trung bình\n";
        rainData += "• 51-80%: Khả năng cao\n";
        rainData += "• 81-100%: Khả năng rất cao\n";

        tvTempData.setText(tempData);
        tvRainData.setText(rainData);
    }

    private void showTemperatureChart() {
        chartTitle.setText("Biểu đồ nhiệt độ 12h");
        tvTempData.setVisibility(View.VISIBLE);
        tvRainData.setVisibility(View.GONE);
        btnTempChart.setBackgroundColor(Color.parseColor("#4A90E2"));
        btnTempChart.setTextColor(Color.WHITE);
        btnRainChart.setBackgroundColor(Color.parseColor("#E0E0E0"));
        btnRainChart.setTextColor(Color.parseColor("#666666"));
    }

    private void showRainChart() {
        chartTitle.setText("Biểu đồ dự báo mưa 12h");
        tvTempData.setVisibility(View.GONE);
        tvRainData.setVisibility(View.VISIBLE);
        btnTempChart.setBackgroundColor(Color.parseColor("#E0E0E0"));
        btnTempChart.setTextColor(Color.parseColor("#666666"));
        btnRainChart.setBackgroundColor(Color.parseColor("#4A90E2"));
        btnRainChart.setTextColor(Color.WHITE);
    }
}