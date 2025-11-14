package com.example.weatherapp.service;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.example.weatherapp.MainActivity;
import com.example.weatherapp.R;
import com.example.weatherapp.api.WeatherApi;
import com.example.weatherapp.model.WeatherResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Calendar;
import java.util.Locale;

public class WeatherNotificationService extends Service {
    private static final String CHANNEL_ID = "weather_channel";
    private static final String WARNING_CHANNEL_ID = "weather_warning_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int ALARM_REQUEST_CODE = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        scheduleDailyNotification();
        getWeatherAndSendNotification();
        return START_STICKY;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel cho thông báo thời tiết thông thường
            CharSequence name = "Thông báo thời tiết";
            String description = "Thông báo thời tiết hàng ngày";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Channel cho cảnh báo thời tiết (ưu tiên cao)
            CharSequence warningName = "Cảnh báo thời tiết";
            String warningDescription = "Cảnh báo thời tiết nguy hiểm";
            int warningImportance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel warningChannel = new NotificationChannel(WARNING_CHANNEL_ID, warningName, warningImportance);
            warningChannel.setDescription(warningDescription);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            notificationManager.createNotificationChannel(warningChannel);
        }
    }

    private void scheduleDailyNotification() {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, WeatherNotificationService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, ALARM_REQUEST_CODE, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 7);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            // Nếu thời gian đã qua trong ngày hôm nay, lên lịch cho ngày mai
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (alarmManager != null) {
                // Sử dụng setExactAndAllowWhileIdle để đảm bảo thông báo hoạt động ngay cả ở chế độ Doze
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY, pendingIntent);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void getWeatherAndSendNotification() {
        SharedPreferences prefs = getSharedPreferences("weather_prefs", Context.MODE_PRIVATE);
        double lat = Double.longBitsToDouble(prefs.getLong("last_lat", Double.doubleToLongBits(21.0285)));
        double lon = Double.longBitsToDouble(prefs.getLong("last_lon", Double.doubleToLongBits(105.8542)));

        WeatherApi weatherApi = com.example.weatherapp.api.ApiClient.getWeatherApi();
        Call<WeatherResponse> call = weatherApi.getCurrentWeather(lat, lon, WeatherApi.API_KEY, "metric", "vi");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weather = response.body();
                    // Kiểm tra quyền trước khi gửi notification
                    if (hasNotificationPermission()) {
                        sendWeatherNotification(weather);
                        checkWeatherWarnings(weather);
                    }
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                // Có thể thêm logic retry ở đây nếu cần
            }
        });
    }

    // Kiểm tra quyền notification
    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        // Android < 13 không cần quyền runtime
        return true;
    }

    private void sendWeatherNotification(WeatherResponse weather) {
        if (!hasNotificationPermission()) {
            return;
        }

        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String temp = String.format(Locale.getDefault(), "%.0f°C", weather.getMain().getTemp());
            String description = weather.getWeather() != null && !weather.getWeather().isEmpty() ?
                    weather.getWeather().get(0).getDescription() : "Không xác định";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Thời tiết hôm nay tại " + weather.getName())
                    .setContentText(temp + " - " + description)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Nhiệt độ: " + temp + "\n" +
                                    "Thời tiết: " + description + "\n" +
                                    "Độ ẩm: " + weather.getMain().getHumidity() + "%\n" +
                                    "Gió: " + weather.getWind().getSpeed() + " m/s"))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkWeatherWarnings(WeatherResponse weather) {
        if (!hasNotificationPermission()) {
            return;
        }

        try {
            double temp = weather.getMain().getTemp();
            String mainWeather = weather.getWeather() != null && !weather.getWeather().isEmpty() ?
                    weather.getWeather().get(0).getMain().toLowerCase() : "";

            if (temp > 35) {
                showWarningNotification("Nhiệt độ cao", "Nhiệt độ hôm nay lên tới " + (int)temp + "°C. Hãy uống nhiều nước!");
            } else if (temp < 10) {
                showWarningNotification("Nhiệt độ thấp", "Trời lạnh " + (int)temp + "°C. Nhớ mặc ấm!");
            }

            if (mainWeather.contains("rain") || mainWeather.contains("storm")) {
                showWarningNotification("Mưa", "Hôm nay trời mưa. Nhớ mang theo ô!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showWarningNotification(String title, String message) {
        if (!hasNotificationPermission()) {
            return;
        }

        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, WARNING_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_warning)
                    .setContentTitle("⚠️ " + title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                // Sử dụng timestamp làm ID để mỗi cảnh báo có ID duy nhất
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}