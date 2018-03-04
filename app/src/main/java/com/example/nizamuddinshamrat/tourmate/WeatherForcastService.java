package com.example.nizamuddinshamrat.tourmate;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by saran on 12/31/2017.
 */

public interface WeatherForcastService {
    @GET()
    Call<WeatherForcastResponse> getWeatherForcastRespose(@Url String string);
}
