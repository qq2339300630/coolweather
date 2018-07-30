package com.coolweather.android;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.w3c.dom.Text;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId;

    public DrawerLayout drawerLayout;
    private Button navButton;

    private ImageView bingPicImg;
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdatetime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_weather );
        weatherLayout = (ScrollView)findViewById( R.id.weather_layout );
        titleCity = (TextView)findViewById( R.id.title_city );
        titleUpdatetime = (TextView)findViewById( R.id.title_update_time );
        degreeText = (TextView)findViewById( R.id.degree_text );
        weatherInfoText = (TextView)findViewById( R.id.weather_info_text );
        forecastLayout = (LinearLayout)findViewById( R.id.forecast_layout );
        aqiText = (TextView)findViewById( R.id.aqi_text );
        pm25Text = (TextView)findViewById( R.id.pm25_text );
        comfortText = (TextView)findViewById( R.id.comfort_text );
        carWashText = (TextView)findViewById( R.id.car_wash_text );
        sportText = (TextView)findViewById( R.id.sport_text );
        bingPicImg = (ImageView)findViewById( R.id.bing_pic_img );
        swipeRefresh = (SwipeRefreshLayout)findViewById( R.id.swipe_refresh );
        swipeRefresh.setColorSchemeResources( R.color.colorPrimary );
        drawerLayout = (DrawerLayout)findViewById( R.id.drawer_layout );
        navButton = (Button)findViewById( R.id.nav_button );
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( this );
        String weatherString = pref.getString( "weather",null );
        if(weatherString!=null){
            Weather weather = Utility.handleWeatherResponse( weatherString );
            mWeatherId = weather.basic.weatherid;
            showWeatherInfo( weather );
        }else{
            mWeatherId = getIntent().getStringExtra( "weather_id" );
            weatherLayout.setVisibility( View.INVISIBLE );
            requestWeather( mWeatherId );
        }
        String bingPic = pref.getString( "bing_pic",null );
        if(bingPic !=null){
            Glide.with( this ).load( bingPic ).into( bingPicImg );
        }else{
            loadBingPic();
        }

        swipeRefresh.setOnRefreshListener( new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather( mWeatherId );
            }
        } );

        navButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer( GravityCompat.START );
            }
        } );
    }
     public void requestWeather(final String weatherId){
         String WeatherUrl = "http://guolin.tech/api/weather?cityid="+weatherId +
                 "&key=1cdf499758734312b853a584d8e18672";
         HttpUtil.sendOkHttpRequest( WeatherUrl, new Callback() {
             @Override
             public void onFailure(Call call, IOException e) {
                 e.printStackTrace();
                 runOnUiThread( new Runnable() {
                     @Override
                     public void run() {
                         Toast.makeText( WeatherActivity.this,"获取天气失败",Toast.LENGTH_LONG ).show();
                         swipeRefresh.setRefreshing( false );
                     }
                 } );
             }

             @Override
             public void onResponse(Call call, Response response) throws IOException {
                 final String responseText = response.body().string();
                 final Weather weather = Utility.handleWeatherResponse( responseText );
                 runOnUiThread( new Runnable() {
                     @Override
                     public void run() {
                         if(weather != null && "ok".equals( weather.status )){
                             SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences( WeatherActivity.this ).edit();
                             editor.putString( "weather",responseText );
                             editor.apply();
                             mWeatherId = weather.basic.weatherid;
                             showWeatherInfo( weather );
                         }else {
                             Toast.makeText( WeatherActivity.this,"获取天气失败",Toast.LENGTH_LONG ).show();
                         }
                         swipeRefresh.setRefreshing( false );
                     }
                 } );
             }
         } );
         loadBingPic();
     }

     private void showWeatherInfo(Weather weather){
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime;
        String degree = weather.now.temperature+"C";
        String weatherInfo = weather.now.more.info;
        titleCity.setText( cityName );
        titleUpdatetime.setText( updateTime );
        degreeText.setText( degree );
        weatherInfoText.setText( weatherInfo );
        forecastLayout.removeAllViews();
        for (Forecast forecast:weather.forecastList){
                View view = LayoutInflater.from( this ).inflate( R.layout.forecast_item, forecastLayout, false );
                TextView dateText = (TextView)view. findViewById( R.id.date_text );
                TextView infoText = (TextView)view. findViewById( R.id.info_text );
                TextView maxText = (TextView) view.findViewById( R.id.max_text );
                TextView minText = (TextView)view.findViewById( R.id.min_text );
               // Log.e(  "showWeatherInfo: ",""+forecast.date+":55555555"  );
                String data = forecast.date;
                String info = forecast.more.info;
                String max = forecast.temperature.max;
                String min = forecast.temperature.min;
                dateText.setText( data );
                infoText.setText( info );
                maxText.setText( max );
                minText.setText( min );
                forecastLayout.addView( view );
        }
        if(weather.aqi != null){
            aqiText.setText( weather.aqi.city.aqi );
            pm25Text.setText( weather.aqi.city.pm25 );
        }
        String comfotr = "舒适度:"+weather.suggestion.comfort.info;
        String carWath = "洗车指数:"+weather.suggestion.carWash.info;
        String sport = "运动建议:"+weather.suggestion.sport.info;
        comfortText.setText( comfotr );
        carWashText.setText( carWath );
        sportText.setText( sport );
        weatherLayout.setVisibility( View.VISIBLE );
     }

     private void loadBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest( requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences( WeatherActivity.this ).edit();
                editor.putString( "bing_pic",bingPic );
                editor.apply();
                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        Glide.with( WeatherActivity.this ).load( bingPic ).into( bingPicImg );
                    }
                } );
            }
        } );
     }
}
