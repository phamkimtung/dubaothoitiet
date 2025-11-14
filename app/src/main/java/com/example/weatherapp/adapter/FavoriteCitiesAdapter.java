package com.example.weatherapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import com.example.weatherapp.FavoriteCitiesActivity;
import com.example.weatherapp.R;
import java.util.List;

public class FavoriteCitiesAdapter extends BaseAdapter {
    private FavoriteCitiesActivity activity;
    private List<FavoriteCitiesActivity.FavoriteCity> cities;

    public FavoriteCitiesAdapter(FavoriteCitiesActivity activity, List<FavoriteCitiesActivity.FavoriteCity> cities) {
        this.activity = activity;
        this.cities = cities;
    }

    @Override
    public int getCount() {
        return cities.size();
    }

    @Override
    public Object getItem(int position) {
        return cities.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(activity).inflate(R.layout.item_favorite_city, parent, false);
            holder = new ViewHolder();
            holder.cityNameTextView = convertView.findViewById(R.id.cityNameTextView);
            holder.temperatureTextView = convertView.findViewById(R.id.temperatureTextView);
            holder.weatherDescTextView = convertView.findViewById(R.id.weatherDescTextView);
            holder.deleteButton = convertView.findViewById(R.id.deleteButton);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FavoriteCitiesActivity.FavoriteCity city = cities.get(position);

        holder.cityNameTextView.setText(city.getCityName() + ", " + city.getCountry());

        if (city.getTemperature() != 0) {
            holder.temperatureTextView.setText(city.getTemperature() + "°C");
            holder.weatherDescTextView.setText(city.getWeatherDescription());
            holder.temperatureTextView.setVisibility(View.VISIBLE);
            holder.weatherDescTextView.setVisibility(View.VISIBLE);
        } else {
            holder.temperatureTextView.setVisibility(View.GONE);
            holder.weatherDescTextView.setVisibility(View.GONE);
        }

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.removeFavoriteCity(city);
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView cityNameTextView;
        TextView temperatureTextView;
        TextView weatherDescTextView;
        ImageButton deleteButton;
    }
}