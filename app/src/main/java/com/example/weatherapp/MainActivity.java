package com.example.weatherapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.weatherapp.adapter.DailyForecastAdapter;
import com.example.weatherapp.adapter.HourlyForecastAdapter;
import com.example.weatherapp.api.WeatherApi;
import com.example.weatherapp.model.WeatherResponse;
import com.example.weatherapp.model.ForecastResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int SEARCH_REQUEST_CODE = 100;

    //vị trí GPS hiện tại của thiết bị
    private FusedLocationProviderClient fusedLocationClient;

    private TextView locationTextView, currentTempTextView, weatherDescTextView;
    private TextView humidityTextView, windTextView, sunriseTextView, sunsetTextView;
    private RecyclerView hourlyRecyclerView, dailyRecyclerView;
    private ImageButton searchButton, locationButton;
    private Button mapButton;

    private HourlyForecastAdapter hourlyAdapter;
    private DailyForecastAdapter dailyAdapter;

    //Tọa độ mặc định (Hà Nội)
    private double currentLat = 21.0285;
    private double currentLon = 105.8542;
    private boolean isUsingCurrentLocation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        checkLocationPermission();
    }

    private void initViews() {
        locationTextView = findViewById(R.id.locationTextView);
        currentTempTextView = findViewById(R.id.currentTempTextView);
        weatherDescTextView = findViewById(R.id.weatherDescTextView);
        humidityTextView = findViewById(R.id.humidityTextView);
        windTextView = findViewById(R.id.windTextView);
        sunriseTextView = findViewById(R.id.sunriseTextView);
        sunsetTextView = findViewById(R.id.sunsetTextView);
        searchButton = findViewById(R.id.searchButton);
        locationButton = findViewById(R.id.locationButton);
        mapButton = findViewById(R.id.mapButton);

        locationButton.setVisibility(View.GONE);

        hourlyRecyclerView = findViewById(R.id.hourlyRecyclerView);
        hourlyRecyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));
        hourlyAdapter = new HourlyForecastAdapter(new ArrayList<>());
        hourlyRecyclerView.setAdapter(hourlyAdapter);

        dailyRecyclerView = findViewById(R.id.dailyRecyclerView);
        dailyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dailyAdapter = new DailyForecastAdapter(new ArrayList<>());
        dailyRecyclerView.setAdapter(dailyAdapter);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivityForResult(intent, SEARCH_REQUEST_CODE);
            }
        });

        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnToCurrentLocation();
            }
        });

        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, WeatherMapActivity.class);
                intent.putExtra("lat", currentLat);
                intent.putExtra("lon", currentLon);
                startActivity(intent);
            }
        });
    }

    private void returnToCurrentLocation() {
        isUsingCurrentLocation = true;
        locationButton.setVisibility(View.GONE);
        checkLocationPermission();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SEARCH_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                double lat = data.getDoubleExtra("lat", currentLat);
                double lon = data.getDoubleExtra("lon", currentLon);
                currentLat = lat;
                currentLon = lon;
                isUsingCurrentLocation = false;
                locationButton.setVisibility(View.VISIBLE);
                getWeatherData(lat, lon);
            }
        }
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Cần quyền truy cập vị trí để hiển thị thời tiết",
                        Toast.LENGTH_LONG).show();
                getWeatherData(currentLat, currentLon);
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            currentLat = location.getLatitude();
                            currentLon = location.getLongitude();
                            getWeatherData(currentLat, currentLon);
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Không thể lấy vị trí hiện tại", Toast.LENGTH_SHORT).show();
                            getWeatherData(currentLat, currentLon);
                        }
                    }
                });
    }

    private void getWeatherData(double lat, double lon) {
        WeatherApi weatherApi = com.example.weatherapp.api.ApiClient.getWeatherApi();

        Call<WeatherResponse> currentWeatherCall = weatherApi.getCurrentWeather(
                lat, lon, WeatherApi.API_KEY, "metric", "vi");

        currentWeatherCall.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateCurrentWeather(response.body());
                } else {
                    Toast.makeText(MainActivity.this,
                            "Lỗi khi lấy dữ liệu thời tiết", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        getForecastData(lat, lon);
    }

    private void getForecastData(double lat, double lon) {
        WeatherApi weatherApi = com.example.weatherapp.api.ApiClient.getWeatherApi();

        Call<ForecastResponse> forecastCall = weatherApi.getForecast(
                lat, lon, WeatherApi.API_KEY, "metric", "vi");

        forecastCall.enqueue(new Callback<ForecastResponse>() {
            @Override
            public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateForecastData(response.body());
                }
            }

            @Override
            public void onFailure(Call<ForecastResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "Lỗi khi lấy dữ liệu dự báo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCurrentWeather(WeatherResponse weather) {
        locationTextView.setText(weather.getName() + ", " + weather.getSys().getCountry());
        currentTempTextView.setText(String.format(Locale.getDefault(), "%.0f°C", weather.getMain().getTemp()));

        if (weather.getWeather() != null && !weather.getWeather().isEmpty()) {
            weatherDescTextView.setText(weather.getWeather().get(0).getDescription());
        }

        humidityTextView.setText("Độ ẩm: " + weather.getMain().getHumidity() + "%");
        windTextView.setText("Gió: " + weather.getWind().getSpeed() + " m/s");

        if (weather.getSys() != null) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            String sunriseTime = timeFormat.format(new Date(weather.getSys().getSunrise() * 1000));
            String sunsetTime = timeFormat.format(new Date(weather.getSys().getSunset() * 1000));

            sunriseTextView.setText("🌅 Mặt trời mọc: " + sunriseTime);
            sunsetTextView.setText("🌇 Mặt trời lặn: " + sunsetTime);
        }
    }

    private void updateForecastData(ForecastResponse forecast) {
        List<ForecastResponse.ForecastItem> allItems = forecast.getList();
        List<ForecastResponse.ForecastItem> hourlyItems = new ArrayList<>();
        List<ForecastResponse.ForecastItem> dailyItems = new ArrayList<>();

        long currentTime = System.currentTimeMillis() / 1000;

        for (ForecastResponse.ForecastItem item : allItems) {
            if (hourlyItems.size() >= 12) break;

            if (item.getDt() > currentTime) {
                hourlyItems.add(item);
            }
        }

        Map<String, ForecastResponse.ForecastItem> dailyMap = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date(currentTime * 1000));

        for (ForecastResponse.ForecastItem item : allItems) {
            if (dailyMap.size() >= 7) break;

            String itemDate = dateFormat.format(new Date(item.getDt() * 1000));

            if (!itemDate.equals(today)) {
                if (!dailyMap.containsKey(itemDate)) {
                    dailyMap.put(itemDate, item);
                } else {
                    ForecastResponse.ForecastItem existingItem = dailyMap.get(itemDate);
                    SimpleDateFormat hourFormat = new SimpleDateFormat("HH", Locale.getDefault());
                    int currentHour = Integer.parseInt(hourFormat.format(new Date(item.getDt() * 1000)));
                    int existingHour = Integer.parseInt(hourFormat.format(new Date(existingItem.getDt() * 1000)));

                    if (Math.abs(currentHour - 12) < Math.abs(existingHour - 12)) {
                        dailyMap.put(itemDate, item);
                    }
                }
            }
        }

        dailyItems.addAll(dailyMap.values());

        Collections.sort(dailyItems, new Comparator<ForecastResponse.ForecastItem>() {
            @Override
            public int compare(ForecastResponse.ForecastItem item1, ForecastResponse.ForecastItem item2) {
                if (item1.getDt() < item2.getDt()) {
                    return -1;
                } else if (item1.getDt() > item2.getDt()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        if (dailyItems.size() > 7) {
            dailyItems = dailyItems.subList(0, 7);
        }

        hourlyAdapter = new HourlyForecastAdapter(hourlyItems);
        hourlyRecyclerView.setAdapter(hourlyAdapter);

        dailyAdapter = new DailyForecastAdapter(dailyItems);
        dailyRecyclerView.setAdapter(dailyAdapter);

        Log.d("WeatherApp", "Hourly items: " + hourlyItems.size());
        Log.d("WeatherApp", "Daily items: " + dailyItems.size());
    }
}