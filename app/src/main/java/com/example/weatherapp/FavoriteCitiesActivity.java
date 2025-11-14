package com.example.weatherapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.weatherapp.adapter.FavoriteCitiesAdapter;
import com.example.weatherapp.api.WeatherApi;
import com.example.weatherapp.model.WeatherResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FavoriteCitiesActivity extends AppCompatActivity {

    private ListView favoritesListView;
    private LinearLayout emptyStateLayout;
    private Button backButton;
    private SharedPreferences favoritesPrefs;
    private FavoriteCitiesAdapter adapter;
    private List<FavoriteCity> favoriteCities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_cities);

        initViews();
        loadFavoriteCities();
    }

    private void initViews() {
        favoritesListView = findViewById(R.id.favoritesListView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        backButton = findViewById(R.id.backButton);

        favoritesPrefs = getSharedPreferences("favorite_cities", MODE_PRIVATE);
        favoriteCities = new ArrayList<>();

        adapter = new FavoriteCitiesAdapter(this, favoriteCities);
        favoritesListView.setAdapter(adapter);

        // Xử lý khi nhấn vào thành phố
        favoritesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FavoriteCity city = favoriteCities.get(position);
                openCityWeather(city);
            }
        });

        // Xử lý nút back
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadFavoriteCities() {
        favoriteCities.clear();

        Map<String, ?> allFavorites = favoritesPrefs.getAll();

        if (allFavorites.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            for (Map.Entry<String, ?> entry : allFavorites.entrySet()) {
                String cityInfo = entry.getValue().toString();
                String[] parts = cityInfo.split(",");
                if (parts.length >= 2) {
                    String cityName = parts[0].trim();
                    String country = parts.length > 1 ? parts[1].trim() : "";
                    favoriteCities.add(new FavoriteCity(cityName, country, cityInfo));
                }
            }

            // Lấy thông tin thời tiết cho từng thành phố
            for (FavoriteCity city : favoriteCities) {
                getWeatherForCity(city);
            }
        }
    }

    private void getWeatherForCity(FavoriteCity city) {
        WeatherApi weatherApi = com.example.weatherapp.api.ApiClient.getWeatherApi();
        Call<WeatherResponse> call = weatherApi.searchWeatherByCity(
                city.getCityName(), WeatherApi.API_KEY, "metric", "vi");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weather = response.body();
                    city.setTemperature((int) weather.getMain().getTemp());
                    city.setWeatherDescription(weather.getWeather().get(0).getDescription());
                    city.setLat(weather.getCoord().getLat());
                    city.setLon(weather.getCoord().getLon());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                // Không làm gì nếu không lấy được thời tiết
            }
        });
    }

    private void openCityWeather(FavoriteCity city) {
        if (city.getLat() != 0 && city.getLon() != 0) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("lat", city.getLat());
            resultIntent.putExtra("lon", city.getLon());
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            // Fallback: tìm kiếm lại bằng tên thành phố
            WeatherApi weatherApi = com.example.weatherapp.api.ApiClient.getWeatherApi();
            Call<WeatherResponse> call = weatherApi.searchWeatherByCity(
                    city.getCityName(), WeatherApi.API_KEY, "metric", "vi");

            call.enqueue(new Callback<WeatherResponse>() {
                @Override
                public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        WeatherResponse weather = response.body();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("lat", weather.getCoord().getLat());
                        resultIntent.putExtra("lon", weather.getCoord().getLon());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(FavoriteCitiesActivity.this,
                                "Không thể tải thông tin thành phố", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<WeatherResponse> call, Throwable t) {
                    Toast.makeText(FavoriteCitiesActivity.this,
                            "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void removeFavoriteCity(FavoriteCity city) {
        favoritesPrefs.edit().remove(city.getCityName()).apply();
        favoriteCities.remove(city);
        adapter.notifyDataSetChanged();

        if (favoriteCities.isEmpty()) {
            showEmptyState();
        }

        Toast.makeText(this, "Đã xóa " + city.getCityName() + " khỏi yêu thích",
                Toast.LENGTH_SHORT).show();
    }

    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        favoritesListView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateLayout.setVisibility(View.GONE);
        favoritesListView.setVisibility(View.VISIBLE);
    }

    // Model class cho thành phố yêu thích
    public static class FavoriteCity {
        private String cityName;
        private String country;
        private String fullInfo;
        private int temperature;
        private String weatherDescription;
        private double lat;
        private double lon;

        public FavoriteCity(String cityName, String country, String fullInfo) {
            this.cityName = cityName;
            this.country = country;
            this.fullInfo = fullInfo;
            this.temperature = 0;
            this.weatherDescription = "";
            this.lat = 0;
            this.lon = 0;
        }

        // Getters and setters
        public String getCityName() { return cityName; }
        public String getCountry() { return country; }
        public String getFullInfo() { return fullInfo; }
        public int getTemperature() { return temperature; }
        public void setTemperature(int temperature) { this.temperature = temperature; }
        public String getWeatherDescription() { return weatherDescription; }
        public void setWeatherDescription(String weatherDescription) { this.weatherDescription = weatherDescription; }
        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }
    }
}