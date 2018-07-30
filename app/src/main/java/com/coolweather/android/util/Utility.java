package com.coolweather.android.util;

import android.text.TextUtils;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

public class Utility {

    public static boolean handleProvinceResonse(String response){
        if(!TextUtils.isEmpty( response )){
            try{
                JSONArray allProvinces = new JSONArray( response );
                for(int i=0;i<allProvinces.length();i++){
                    JSONObject provinceObject = allProvinces.getJSONObject( i );
                    Province province = new Province();
                    province.setProvinceName( provinceObject.getString( "name" ) );
                    province.setProvinceCode( provinceObject.getInt( "id" ) );
                    province.save();
                }
                return true;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean handleCityResonse(String response,int provinceId){
        if(!TextUtils.isEmpty( response )){
            try{
                JSONArray allCities = new JSONArray( response );
                for(int i=0;i<allCities.length();i++){
                    JSONObject cityObject = allCities.getJSONObject( i );
                    City city = new City();
                    city.setCityName( cityObject.getString( "name" ) );
                    city.setCityCode( cityObject.getInt( "id" ) );
                    city.setProvinceId( provinceId );
                    city.save();
                }
                return true;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }


    public static boolean handleCOuntyResonse(String response,int cityId){
        if(!TextUtils.isEmpty( response )){
            try{
                JSONArray allCounies = new JSONArray( response );
                for(int i=0;i<allCounies.length();i++){
                    JSONObject cityObject = allCounies.getJSONObject( i );
                    County county = new County();
                    county.setCountryName( cityObject.getString( "name" ) );
                    county.setWeatherId( cityObject.getString( "weather_id" ) );
                    county.setCityId( cityId );
                    county.save();
                }
                return true;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }

    public static Weather handleWeatherResponse(String response){
        try {
            JSONObject jsonObject = new JSONObject( response );
            JSONArray jsonArray = jsonObject.getJSONArray( "HeWeather" );
            String weatherContent = jsonArray.getJSONObject( 0 ).toString();
            return  new Gson().fromJson( weatherContent,Weather.class );
        }catch (Exception e){
            e.printStackTrace();
        }
        return  null;
    }
}
