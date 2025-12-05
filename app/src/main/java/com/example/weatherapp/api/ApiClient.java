package com.example.weatherapp.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static Retrofit weatherRetrofit = null;
    private static Retrofit weatherApiRetrofit = null;

    // Retrofit cho OpenWeatherMap
    public static Retrofit getWeatherClient() {
        if (weatherRetrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);

            weatherRetrofit = new Retrofit.Builder()
                    .baseUrl(WeatherApi.BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return weatherRetrofit;
    }

    // Retrofit cho WeatherAPI.com với logging
    public static Retrofit getWeatherApiClient() {
        if (weatherApiRetrofit == null) {
            // Thêm logging interceptor để debug
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(loggingInterceptor); // Thêm logging

            weatherApiRetrofit = new Retrofit.Builder()
                    .baseUrl(WeatherApi.WEATHER_API_BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return weatherApiRetrofit;
    }

    public static WeatherApi getWeatherApi() {
        return getWeatherClient().create(WeatherApi.class);
    }

    public static WeatherApi getWeatherApiComService() {
        return getWeatherApiClient().create(WeatherApi.class);
    }
}