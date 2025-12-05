package com.example.weatherapp.api;

import com.example.weatherapp.model.WeatherResponse;
import com.example.weatherapp.model.ForecastResponse;
import com.example.weatherapp.model.AirQualityResponse;
import com.google.gson.annotations.SerializedName;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApi {
    // OpenWeatherMap API (cho thời tiết)
    String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    String API_KEY = "65cd5bbb91f8778e2242fbdc4d185eb7";

    // WeatherAPI.com API (cho chất lượng không khí) - DÙNG HTTPS
    String WEATHER_API_BASE_URL = "https://api.weatherapi.com/v1/";
    String WEATHER_API_KEY = "4c4b9c2332be4ee8898134853250412";

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

    // API cho chất lượng không khí từ weatherAPI.com (HTTPS)
    @GET("current.json")
    Call<WeatherApiAirQualityResponse> getWeatherApiAirQuality(
            @Query("key") String key,
            @Query("q") String query,
            @Query("aqi") String aqi
    );

    // API cho chất lượng không khí từ OpenWeatherMap
    @GET("air_pollution")
    Call<AirQualityResponse> getAirQuality(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String appid
    );

    // Model cho response từ weatherAPI.com
    class WeatherApiAirQualityResponse {
        @SerializedName("current")
        private Current current;

        @SerializedName("location")
        private Location location;

        public Current getCurrent() { return current; }
        public void setCurrent(Current current) { this.current = current; }

        public Location getLocation() { return location; }
        public void setLocation(Location location) { this.location = location; }

        @Override
        public String toString() {
            return "WeatherApiAirQualityResponse{" +
                    "current=" + (current != null ? current.toString() : "null") +
                    ", location=" + (location != null ? location.toString() : "null") +
                    '}';
        }

        public class Current {
            @SerializedName("air_quality")
            private AirQuality air_quality;

            @SerializedName("last_updated")
            private String last_updated;

            public AirQuality getAir_quality() { return air_quality; }
            public void setAir_quality(AirQuality air_quality) { this.air_quality = air_quality; }

            public String getLast_updated() { return last_updated; }
            public void setLast_updated(String last_updated) { this.last_updated = last_updated; }

            @Override
            public String toString() {
                return "Current{" +
                        "air_quality=" + (air_quality != null ? air_quality.toString() : "null") +
                        ", last_updated='" + last_updated + '\'' +
                        '}';
            }

            public class AirQuality {
                @SerializedName("co")
                private double co;

                @SerializedName("no2")
                private double no2;

                @SerializedName("o3")
                private double o3;

                @SerializedName("so2")
                private double so2;

                @SerializedName("pm2_5")
                private double pm2_5;

                @SerializedName("pm10")
                private double pm10;

                @SerializedName("us-epa-index")
                private int us_epa_index;

                @SerializedName("gb-defra-index")
                private int gb_defra_index;

                public double getCo() { return co; }
                public void setCo(double co) { this.co = co; }

                public double getNo2() { return no2; }
                public void setNo2(double no2) { this.no2 = no2; }

                public double getO3() { return o3; }
                public void setO3(double o3) { this.o3 = o3; }

                public double getSo2() { return so2; }
                public void setSo2(double so2) { this.so2 = so2; }

                public double getPm2_5() { return pm2_5; }
                public void setPm2_5(double pm2_5) { this.pm2_5 = pm2_5; }

                public double getPm10() { return pm10; }
                public void setPm10(double pm10) { this.pm10 = pm10; }

                public int getUs_epa_index() { return us_epa_index; }
                public void setUs_epa_index(int us_epa_index) { this.us_epa_index = us_epa_index; }

                public int getGb_defra_index() { return gb_defra_index; }
                public void setGb_defra_index(int gb_defra_index) { this.gb_defra_index = gb_defra_index; }

                @Override
                public String toString() {
                    return "AirQuality{" +
                            "co=" + co +
                            ", no2=" + no2 +
                            ", o3=" + o3 +
                            ", so2=" + so2 +
                            ", pm2_5=" + pm2_5 +
                            ", pm10=" + pm10 +
                            ", us_epa_index=" + us_epa_index +
                            ", gb_defra_index=" + gb_defra_index +
                            '}';
                }
            }
        }

        public class Location {
            @SerializedName("name")
            private String name;

            @SerializedName("region")
            private String region;

            @SerializedName("country")
            private String country;

            @SerializedName("lat")
            private double lat;

            @SerializedName("lon")
            private double lon;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }

            public String getRegion() { return region; }
            public void setRegion(String region) { this.region = region; }

            public String getCountry() { return country; }
            public void setCountry(String country) { this.country = country; }

            public double getLat() { return lat; }
            public void setLat(double lat) { this.lat = lat; }

            public double getLon() { return lon; }
            public void setLon(double lon) { this.lon = lon; }

            @Override
            public String toString() {
                return "Location{" +
                        "name='" + name + '\'' +
                        ", region='" + region + '\'' +
                        ", country='" + country + '\'' +
                        ", lat=" + lat +
                        ", lon=" + lon +
                        '}';
            }
        }
    }
}