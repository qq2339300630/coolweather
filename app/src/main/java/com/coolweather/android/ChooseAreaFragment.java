package com.coolweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String>adapter;

    private List<String>dataList = new ArrayList<>(  );

    private List<Province>provinceList;
    private List<City>cityList;
    private List<County>countyList;

    private Province selectedprovince;
    private City selectedcity;

    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate( R.layout.choose_area,container,false );
        titleText = (TextView)view.findViewById( R.id.title_text );
        backButton = (Button)view.findViewById( R.id.back_button );
        listView = (ListView)view.findViewById( R.id.list_view );
        adapter = new ArrayAdapter<>( getContext(),android.R.layout.simple_list_item_1,dataList );
        listView.setAdapter( adapter );

        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated( savedInstanceState );
        listView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(currentLevel == LEVEL_PROVINCE){
                    selectedprovince = provinceList.get( i );
                    queryClities();
                }else if(currentLevel == LEVEL_CITY){
                    selectedcity = cityList.get( i );
                    queryCounties();
                }else if(currentLevel == LEVEL_COUNTY) {
                    String weatherId = countyList.get( i ).getWeatherId();
                    Intent intent = new Intent( getActivity(),WeatherActivity.class );
                    intent.putExtra( "weather_id",weatherId );
                    startActivity( intent );
                    getActivity().finish();
                }
            }
        } );
        backButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentLevel == LEVEL_COUNTY){
                    queryClities();
                }else if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        } );
        queryProvinces();
    }



    private void queryProvinces(){
        titleText.setText( "中国" );
        backButton.setVisibility( View.GONE );
        provinceList = DataSupport.findAll( Province.class );
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add( province.getProvinceName() );
            }
            adapter.notifyDataSetChanged();
            listView.setSelection( 0 );
            currentLevel = LEVEL_PROVINCE;
        }else{
            String address = "http://guolin.tech/api/china";
            queryFormServer(address,"province");
        }
    }

    private void queryClities(){
        titleText.setText( selectedprovince.getProvinceName() );
        backButton.setVisibility( View.VISIBLE);
        cityList = DataSupport.where( "provinceid = ?",String.valueOf( selectedprovince.getId() ) ).find( City.class );
        if(cityList.size()>0){
            dataList.clear();
            for(City city:cityList){
                dataList.add( city.getCityName() );
            }
            adapter.notifyDataSetChanged();
            listView.setSelection( 0 );
            currentLevel = LEVEL_CITY;
        }else{
            int provinceCode = selectedprovince.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+provinceCode;
            queryFormServer( address,"city" );
        }
    }

    private void queryCounties(){
        titleText.setText( selectedcity.getCityName() );
        backButton.setVisibility( View.VISIBLE);
        countyList = DataSupport.where( "cityid = ?",String.valueOf( selectedcity.getId() ) ).find( County.class );
        if(countyList.size()>0){
            dataList.clear();
            for (County county:countyList){
                dataList.add( county.getCountryName() );
            }
            adapter.notifyDataSetChanged();
            listView.setSelection( 0 );
            currentLevel = LEVEL_COUNTY;
        }else{
            int provinceCode = selectedprovince.getProvinceCode();
            int cityCode = selectedcity.getCityCode();
            String address =  "http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFormServer( address,"county" );
        }

    }


    private void queryFormServer(String address,final String type){
        HttpUtil.sendOkHttpRequest( address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
               getActivity().runOnUiThread( new Runnable() {
                   @Override
                   public void run() {
                       closeProgressDialog();
                       Toast.makeText( getContext(),"加载失败",Toast.LENGTH_LONG ).show();
                   }
               } );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
               String responseText = response.body().string();
               boolean result = false;
               if("province".equals( type )){
                   result = Utility.handleProvinceResonse( responseText );
               }else if("city".equals( type )){
                   result = Utility.handleCityResonse( responseText,selectedprovince.getId() );
               }else if("county".equals( type )){
                   result = Utility.handleCOuntyResonse( responseText,selectedcity.getId() );
               }
               if(result){
                   getActivity().runOnUiThread( new Runnable() {
                       @Override
                       public void run() {
                           closeProgressDialog();
                           if("province".equals( type )){
                               queryProvinces();
                           }else if("city".equals( type )){
                               queryClities();
                           }else if("county".equals( type )){
                                queryCounties();
                           }
                       }
                   } );
               }
            }
        } );
    }




    private  void showProgressDialog(){
        if(progressDialog == null){
            progressDialog = new ProgressDialog( getActivity() );
            progressDialog.setMessage( "正在加载....." );
            progressDialog.setCanceledOnTouchOutside( false );
        }
        progressDialog.show();
    }

    private void closeProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }
}
