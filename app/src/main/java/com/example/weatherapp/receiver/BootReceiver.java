package com.example.weatherapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.weatherapp.service.WeatherNotificationService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Restart weather notification service on boot
            Intent serviceIntent = new Intent(context, WeatherNotificationService.class);
            context.startService(serviceIntent);
        }
    }
}