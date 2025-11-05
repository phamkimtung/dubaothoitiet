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

public class DailyForecastAdapter extends RecyclerView.Adapter<DailyForecastAdapter.ViewHolder> {
    private List<ForecastResponse.ForecastItem> dailyItems;

    public DailyForecastAdapter(List<ForecastResponse.ForecastItem> dailyItems) {
        this.dailyItems = dailyItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_forecast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ForecastResponse.ForecastItem item = dailyItems.get(position);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        String dayName = dateFormat.format(new Date(item.getDt() * 1000));

        SimpleDateFormat dateFormat2 = new SimpleDateFormat("dd/MM", Locale.getDefault());
        String date = dateFormat2.format(new Date(item.getDt() * 1000));

        holder.dateTextView.setText(dayName + "\n" + date);

        holder.tempTextView.setText(String.format(Locale.getDefault(),
                "%.0f°C", item.getMain().getTemp()));

        if (item.getWeather() != null && !item.getWeather().isEmpty()) {
            holder.descTextView.setText(item.getWeather().get(0).getDescription());

            // Weather icon
            String iconUrl = "https://openweathermap.org/img/wn/" +
                    item.getWeather().get(0).getIcon() + "@2x.png";
            Glide.with(holder.itemView.getContext())
                    .load(iconUrl)
                    .into(holder.weatherIcon);
        }
    }

    @Override
    public int getItemCount() {
        return dailyItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView;
        TextView tempTextView;
        TextView descTextView;
        ImageView weatherIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            tempTextView = itemView.findViewById(R.id.tempTextView);
            descTextView = itemView.findViewById(R.id.descTextView);
            weatherIcon = itemView.findViewById(R.id.weatherIcon);
        }
    }
}