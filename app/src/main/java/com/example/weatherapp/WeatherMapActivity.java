package com.example.weatherapp;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class WeatherMapActivity extends AppCompatActivity {

    private WebView webView;
    private double lat, lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_map);

        lat = getIntent().getDoubleExtra("lat", 21.0285);
        lon = getIntent().getDoubleExtra("lon", 105.8542);

        initViews();
        loadWeatherMap();
    }

    private void initViews() {
        webView = findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
    }

    private void loadWeatherMap() {
        String url = "https://openweathermap.org/weathermap?basemap=map&cities=true&layer=clouds&lat=" +
                lat + "&lon=" + lon + "&zoom=10";

        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}