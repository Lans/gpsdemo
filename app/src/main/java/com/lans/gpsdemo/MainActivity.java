package com.lans.gpsdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView txt;
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG, "--->onServiceConnected");
            LocationForegroundService locationForegroundService = ((LocationForegroundService.LocalBinder) service).getLocationForegroundService();
            Location location = locationForegroundService.gps();
            if (location != null) {
                txt.setText("经度" + location.getLatitude() + "\n纬度:" + location.getLongitude());
            } else {
                txt.setText("无法获取地理位置1111");
            }

            locationForegroundService.setLocationCallback(new LocationForegroundService.LocationCallback() {
                @Override
                public void onLocation(Location location) {
                    if (location != null) {
                        txt.setText("经度" + location.getLatitude() + "\n纬度:" + location.getLongitude());
                    } else {
                        txt.setText("无法获取地理位置2222");
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt = findViewById(R.id.txt);
    }

    public void getLocation(View view) {

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            if (!GPSOpen()) {
                Intent i = new Intent();
                i.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        }

        Intent intent = new Intent(this, LocationForegroundService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private boolean GPSOpen() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位,定位级别可以精确到街(通过24颗卫星定位,在室外和空旷的地方定位准确、速度快)
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置(也称作AGPS,辅助GPS定位。主要用于在室内或遮盖物(建筑群或茂密的深林等)密集的地方定位)
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gps || network;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
}
