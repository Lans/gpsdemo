package com.lans.gpsdemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * author:       lans
 * date:         2019-04-1816:13
 * description: 后台定位服务
 **/
public class LocationForegroundService extends Service {
    private static final String TAG = "ForegroundService";
    private LocalBinder localBinder = new LocalBinder();
    private NotificationChannel channel;
    private LocationListener locationListener = new LocationListener(LocationManager.NETWORK_PROVIDER);

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "--->onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "--->onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    public Location gps() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = null;
        //不加这段话会导致下面爆红,（这个俗称版本压制，哈哈哈哈哈哈）
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {//是否支持Network定位
            //获取最后的network定位信息
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        }
        //网络获取定位为空时，每隔1秒请求一次
        if (location == null) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, locationListener);
        }

        return location;
    }

    //显示后台定位通知栏（此为8.0版本通知栏）
    private void showNotify() {
        if (channel == null) {
            channel = createNotificationChannel();
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), getPackageName());
        Intent intent = new Intent(this, MainActivity.class);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("正在后台定位")
                .setContentText("定位进行中")
                .setWhen(System.currentTimeMillis());
        Notification build = builder.build();
        //调用这个方法把服务设置成前台服务
        startForeground(110, build);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private NotificationChannel createNotificationChannel() {
        NotificationChannel notificationChannel = new NotificationChannel(getPackageName(), getPackageName(), NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);
        return notificationChannel;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    public class LocalBinder extends Binder {
        public LocationForegroundService getLocationForegroundService() {
            //Android O上才显示通知栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                showNotify();
            }
            return LocationForegroundService.this;
        }
    }


    public interface LocationCallback {
        /**
         * 当前位置
         */
        void onLocation(Location location);
    }

    private LocationCallback mLocationCallback;

    public void setLocationCallback(LocationCallback mLocationCallback) {
        this.mLocationCallback = mLocationCallback;
    }

    private class LocationListener implements android.location.LocationListener {
        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + "当前坐标：" + location.getLatitude() + " : " + location.getLongitude());
            if (mLocationCallback != null) {
                mLocationCallback.onLocation(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }
}
