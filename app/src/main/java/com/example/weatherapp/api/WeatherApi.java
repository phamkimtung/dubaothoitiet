package com.example.weatherapp.api;

import com.example.weatherapp.model.WeatherResponse;
import com.example.weatherapp.model.ForecastResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApi {
    String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    String API_KEY = "65cd5bbb91f8778e2242fbdc4d185eb7";

    @GET("weather")
    Call<WeatherResponse> getCurrentWeather(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String appid,
            @Query("units") String units,
            @Query("lang") String lang
    );

    @GET("forecast")
    Call<ForecastResponse> getForecast(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String appid,
            @Query("units") String units,
            @Query("lang") String lang
    );

    @GET("weather")
    Call<WeatherResponse> searchWeatherByCity(
            @Query("q") String cityName,
            @Query("appid") String appid,
            @Query("units") String units,
            @Query("lang") String lang
    );
}