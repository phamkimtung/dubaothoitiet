package com.example.weatherapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.weatherapp.adapter.DailyForecastAdapter;
import com.example.weatherapp.adapter.HourlyForecastAdapter;
import com.example.weatherapp.api.WeatherApi;
import com.example.weatherapp.model.WeatherResponse;
import com.example.weatherapp.model.ForecastResponse;
import com.example.weatherapp.service.WeatherNotificationService;
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
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 200;

    //vị trí GPS hiện tại của thiết bị
    private FusedLocationProviderClient fusedLocationClient;

    private TextView locationTextView, currentTempTextView, weatherDescTextView;
    private TextView humidityTextView, windTextView, sunriseTextView, sunsetTextView;
    private RecyclerView hourlyRecyclerView, dailyRecyclerView;
    private ImageButton searchButton, locationButton, favoriteButton;
    private Button mapButton;
    private LinearLayout rootLayout;

    private HourlyForecastAdapter hourlyAdapter;
    private DailyForecastAdapter dailyAdapter;

    //Tọa độ mặc định (Hà Nội)
    private double currentLat = 21.0285;
    private double currentLon = 105.8542;
    private boolean isUsingCurrentLocation = true;
    private SharedPreferences favoritesPrefs;
    private ImageButton favoritesListButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        checkLocationPermission();
        checkNotificationPermission();
        startNotificationService();
    }
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Giải thích lý do cần quyền notification
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Quyền thông báo")
                            .setMessage("Ứng dụng cần quyền thông báo để gửi thông tin thời tiết hàng ngày và cảnh báo thời tiết quan trọng.")
                            .setPositiveButton("Đồng ý", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestNotificationPermission();
                                }
                            })
                            .setNegativeButton("Không", null)
                            .show();
                } else {
                    requestNotificationPermission();
                }
            } else {
                // Đã có quyền, khởi động service
                startNotificationService();
            }
        } else {
            // Android < 13 không cần xin quyền
            startNotificationService();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
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
        favoriteButton = findViewById(R.id.favoriteButton);
        rootLayout = findViewById(R.id.rootLayout);
        favoritesListButton = findViewById(R.id.favoritesListButton);

        favoritesListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FavoriteCitiesActivity.class);
                startActivityForResult(intent, SEARCH_REQUEST_CODE);
            }
        });

        favoritesPrefs = getSharedPreferences("favorite_cities", MODE_PRIVATE);

        locationButton.setVisibility(View.GONE);

        // Setup hourly RecyclerView
        hourlyRecyclerView = findViewById(R.id.hourlyRecyclerView);
        hourlyRecyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));
        hourlyAdapter = new HourlyForecastAdapter(new ArrayList<>());
        hourlyRecyclerView.setAdapter(hourlyAdapter);

        // Setup daily RecyclerView
        dailyRecyclerView = findViewById(R.id.dailyRecyclerView);
        dailyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dailyAdapter = new DailyForecastAdapter(new ArrayList<>());
        dailyRecyclerView.setAdapter(dailyAdapter);

        // Setup search button click
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivityForResult(intent, SEARCH_REQUEST_CODE);
            }
        });

        // Setup location button click
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnToCurrentLocation();
            }
        });

        // Setup map button click
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, WeatherMapActivity.class);
                intent.putExtra("lat", currentLat);
                intent.putExtra("lon", currentLon);
                startActivity(intent);
            }
        });

        // Setup favorite button click
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCurrentCityToFavorites();
            }
        });
    }

    private void startNotificationService() {
        Intent serviceIntent = new Intent(this, WeatherNotificationService.class);
        startService(serviceIntent);
    }

    private void addCurrentCityToFavorites() {
        String currentCity = locationTextView.getText().toString();
        if (!currentCity.isEmpty()) {
            // Lưu thành phố hiện tại vào favorites
            String cityKey = currentCity.split(",")[0].trim(); // Lấy tên thành phố
            favoritesPrefs.edit().putString(cityKey, currentCity).apply();
            Toast.makeText(this, "Đã thêm " + cityKey + " vào yêu thích", Toast.LENGTH_SHORT).show();
        }
    }

    //Quay lại vị trí hiện tại
    private void returnToCurrentLocation() {
        isUsingCurrentLocation = true;
        locationButton.setVisibility(View.GONE);
        checkLocationPermission();
    }

    //chọn địa điểm mới trong SearchActivity và quay lại Mainactivity
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

    //Kiểm tra xem người dùng đã cấp quyền truy cập vị trí chưa
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

    //Kết quả sau khi xin quyền vị trí xong
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
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Đã cấp quyền, khởi động service
                startNotificationService();
                Toast.makeText(this, "Đã bật thông báo thời tiết", Toast.LENGTH_SHORT).show();
            } else {
                // Từ chối quyền, vẫn chạy app nhưng không có notification
                Toast.makeText(this, "Bạn sẽ không nhận được thông báo thời tiết", Toast.LENGTH_LONG).show();
            }
        }
    }

    //Lấy tọa độ GPS hiện tại từ thiết bị.
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

    //Lấy thời tiết hiện tại
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

    //Lấy dữ liệu dự báo thời tiết (5 ngày, chia theo giờ).
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

    //Cập nhật giao diện thời tiết hiện tại trên màn hình chính
    private void updateCurrentWeather(WeatherResponse weather) {
        locationTextView.setText(weather.getName() + ", " + weather.getSys().getCountry());
        currentTempTextView.setText(String.format(Locale.getDefault(), "%.0f°C", weather.getMain().getTemp()));

        if (weather.getWeather() != null && !weather.getWeather().isEmpty()) {
            String description = weather.getWeather().get(0).getDescription();
            String mainWeather = weather.getWeather().get(0).getMain().toLowerCase();
            weatherDescTextView.setText(description);

            // Thay đổi nền theo thời tiết
            updateBackgroundBasedOnWeather(mainWeather);

            // Kiểm tra cảnh báo thời tiết
            checkWeatherWarnings(weather, mainWeather);
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

        // Lưu vị trí cuối cùng cho notification
        saveLastLocation(weather.getCoord().getLat(), weather.getCoord().getLon());
    }

    private void updateBackgroundBasedOnWeather(String weatherCondition) {
        GradientDrawable gradient = new GradientDrawable();

        if (weatherCondition.contains("clear")) {
            // Nắng - gradient vàng cam
            gradient.setColors(new int[]{0xFFFFD700, 0xFFFFA500});
        } else if (weatherCondition.contains("rain") || weatherCondition.contains("drizzle")) {
            // Mưa - gradient xám
            gradient.setColors(new int[]{0xFF808080, 0xFF696969});
        } else if (weatherCondition.contains("cloud")) {
            // Mây - gradient xanh nhạt
            gradient.setColors(new int[]{0xFF87CEEB, 0xFFB0C4DE});
        } else if (weatherCondition.contains("snow")) {
            // Tuyết - gradient trắng xanh
            gradient.setColors(new int[]{0xFFF0F8FF, 0xFFE6E6FA});
        } else {
            // Mặc định - gradient xanh da trời
            gradient.setColors(new int[]{0xFFE3F2FD, 0xFFBBDEFB});
        }

        gradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        gradient.setCornerRadius(0f);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            rootLayout.setBackground(gradient);
        } else {
            rootLayout.setBackgroundDrawable(gradient);
        }
    }

    private void checkWeatherWarnings(WeatherResponse weather, String weatherCondition) {
        double temp = weather.getMain().getTemp();

        if (temp > 35) {
            showWeatherWarning("Nhiệt độ cao", "Nhiệt độ lên tới " + (int)temp + "°C. Hãy uống nhiều nước!");
        } else if (temp < 10) {
            showWeatherWarning("Nhiệt độ thấp", "Trời lạnh " + (int)temp + "°C. Nhớ mặc ấm!");
        }

        if (weatherCondition.contains("rain") || weatherCondition.contains("storm")) {
            showWeatherWarning("Mưa", "Trời mưa. Nhớ mang theo ô!");
        }
    }

    private void showWeatherWarning(String title, String message) {
        Toast.makeText(this, "⚠️ " + title + ": " + message, Toast.LENGTH_LONG).show();
    }

    private void saveLastLocation(double lat, double lon) {
        SharedPreferences prefs = getSharedPreferences("weather_prefs", MODE_PRIVATE);
        prefs.edit()
                .putLong("last_lat", Double.doubleToRawLongBits(lat))
                .putLong("last_lon", Double.doubleToRawLongBits(lon))
                .apply();
    }

    //Xử lý dữ liệu trả về từ API dự báo để hiển thị dự báo theo giờ và 7 ngày tới
    private void updateForecastData(ForecastResponse forecast) {
        List<ForecastResponse.ForecastItem> allItems = forecast.getList();
        List<ForecastResponse.ForecastItem> hourlyItems = new ArrayList<>();
        List<ForecastResponse.ForecastItem> dailyItems = new ArrayList<>();

        long currentTime = System.currentTimeMillis() / 1000;

        // Lấy 12 giờ tiếp theo
        for (ForecastResponse.ForecastItem item : allItems) {
            if (hourlyItems.size() >= 12) break;

            if (item.getDt() > currentTime) {
                hourlyItems.add(item);
            }
        }

        // Lấy 7 ngày tiếp theo
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