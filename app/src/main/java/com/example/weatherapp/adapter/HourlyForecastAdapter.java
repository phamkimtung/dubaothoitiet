package com.example.weatherapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.weatherapp.R;
import com.example.weatherapp.model.ForecastResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HourlyForecastAdapter extends RecyclerView.Adapter<HourlyForecastAdapter.ViewHolder> {
    private List<ForecastResponse.ForecastItem> hourlyItems;

    public HourlyForecastAdapter(List<ForecastResponse.ForecastItem> hourlyItems) {
        this.hourlyItems = hourlyItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hourly_forecast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ForecastResponse.ForecastItem item = hourlyItems.get(position);

        // Format time với AM/PM
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = timeFormat.format(new Date(item.getDt() * 1000));
        holder.timeTextView.setText(time);

        // Temperature
        holder.tempTextView.setText(String.format(Locale.getDefault(), "%.0f°C", item.getMain().getTemp()));

        // Weather icon
        if (item.getWeather() != null && !item.getWeather().isEmpty()) {
            String iconUrl = "https://openweathermap.org/img/wn/" +
                    item.getWeather().get(0).getIcon() + "@2x.png";
            Glide.with(holder.itemView.getContext())
                    .load(iconUrl)
                    .into(holder.weatherIcon);

            // Hiển thị mô tả thời tiết ngắn
            holder.descTextView.setText(item.getWeather().get(0).getMain());
        }
    }

    @Override
    public int getItemCount() {
        return hourlyItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeTextView;
        TextView tempTextView;
        TextView descTextView;
        ImageView weatherIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            tempTextView = itemView.findViewById(R.id.tempTextView);
            descTextView = itemView.findViewById(R.id.descTextView);
            weatherIcon = itemView.findViewById(R.id.weatherIcon);
        }
    }
}