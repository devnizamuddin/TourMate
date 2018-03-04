package com.example.nizamuddinshamrat.tourmate;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by Nizam Uddin Shamrat on 1/17/2018.
 */

public interface DirectionService {

    @GET
    Call<DirectionResponse>getDirections(@Url String urlString);
}
