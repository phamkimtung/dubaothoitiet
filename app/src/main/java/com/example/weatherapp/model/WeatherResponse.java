package com.example.weatherapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WeatherResponse {
    @SerializedName("coord")
    private Coord coord;

    @SerializedName("weather")
    private List<Weather> weather;

    @SerializedName("main")
    private Main main;

    @SerializedName("wind")
    private Wind wind;

    @SerializedName("name")
    private String name;

    @SerializedName("dt")
    private long dt;

    @SerializedName("sys")
    private Sys sys;

    // Getters and setters
    public Coord getCoord() { return coord; }
    public void setCoord(Coord coord) { this.coord = coord; }

    public List<Weather> getWeather() { return weather; }
    public void setWeather(List<Weather> weather) { this.weather = weather; }

    public Main getMain() { return main; }
    public void setMain(Main main) { this.main = main; }

    public Wind getWind() { return wind; }
    public void setWind(Wind wind) { this.wind = wind; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getDt() { return dt; }
    public void setDt(long dt) { this.dt = dt; }

    public Sys getSys() { return sys; }
    public void setSys(Sys sys) { this.sys = sys; }

    //Tọa độ địa lý (vĩ độ, kinh độ)
    public static class Coord {
        @SerializedName("lon")
        private double lon;

        @SerializedName("lat")
        private double lat;

        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
    }
    //Mô tả ngắn thời tiết
    public static class Weather {
        @SerializedName("id")
        private int id;

        @SerializedName("main")
        private String main;

        @SerializedName("description")
        private String description;

        @SerializedName("icon")
        private String icon;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getMain() { return main; }
        public void setMain(String main) { this.main = main; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }
    //Thông tin nhiệt độ, độ ẩm, áp suất
    public static class Main {
        @SerializedName("temp")
        private double temp;

        @SerializedName("feels_like")
        private double feelsLike;

        @SerializedName("temp_min")
        private double tempMin;

        @SerializedName("temp_max")
        private double tempMax;

        @SerializedName("pressure")
        private int pressure;

        @SerializedName("humidity")
        private int humidity;

        public double getTemp() { return temp; }
        public void setTemp(double temp) { this.temp = temp; }

        public double getFeelsLike() { return feelsLike; }
        public void setFeelsLike(double feelsLike) { this.feelsLike = feelsLike; }

        public double getTempMin() { return tempMin; }
        public void setTempMin(double tempMin) { this.tempMin = tempMin; }

        public double getTempMax() { return tempMax; }
        public void setTempMax(double tempMax) { this.tempMax = tempMax; }

        public int getPressure() { return pressure; }
        public void setPressure(int pressure) { this.pressure = pressure; }

        public int getHumidity() { return humidity; }
        public void setHumidity(int humidity) { this.humidity = humidity; }
    }
    //Tốc độ và hướng gió
    public static class Wind {
        @SerializedName("speed")
        private double speed;

        @SerializedName("deg")
        private int deg;

        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }

        public int getDeg() { return deg; }
        public void setDeg(int deg) { this.deg = deg; }
    }
    //Thông tin quốc gia, giờ mặt trời mọc/lặn
    public static class Sys {
        @SerializedName("country")
        private String country;

        @SerializedName("sunrise")
        private long sunrise;

        @SerializedName("sunset")
        private long sunset;

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public long getSunrise() { return sunrise; }
        public void setSunrise(long sunrise) { this.sunrise = sunrise; }

        public long getSunset() { return sunset; }
        public void setSunset(long sunset) { this.sunset = sunset; }
    }
}