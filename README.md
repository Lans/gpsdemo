# gpsdemo
安卓8.0定位适配

## 官方安卓8.0改动介绍

 https://developer.android.google.cn/about/versions/oreo/background-location-limits
##### 截取部分图片
![image.png](https://upload-images.jianshu.io/upload_images/1834083-993c1855640df38c.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

####  根据以上图片介绍，我们决定用Service显示一个前台通知，将获取的定位信息传给Activity。
 
 1.services中bind部分

         public class LocalBinder extends Binder {
                    public LocationForegroundService getLocationForegroundService() {
                        //Android O上才显示通知栏
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            showNotify();
                        }
                        return LocationForegroundService.this;
                    }
                }
2. 显示后台定位通知栏

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
3.获取定位信息

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
4.定位监听

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
    
5.记得释放Service

      @Override
        public void onDestroy() {
            stopForeground(true);
            stopSelf();
            super.onDestroy();
        }

#### services准备工作已经完成，记得在AndroidManifest中注册  


1.Activity中记得请求运行时权限，否则无法获取定位

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

2.安卓8.0获取定位需要打开定位服务，反正无法获取定位信息
    
       private boolean GPSOpen() {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            // 通过GPS卫星定位,定位级别可以精确到街(通过24颗卫星定位,在室外和空旷的地方定位准确、速度快)
            boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            // 通过WLAN或移动网络(3G/2G)确定的位置(也称作AGPS,辅助GPS定位。主要用于在室内或遮盖物(建筑群或茂密的深林等)密集的地方定位)
            boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            return gps || network;
        }


    public void getLocation(View view) {
        
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            //判断定位服务开启没有,没有跳转到设置页面
            if (!GPSOpen()) {
                Intent i = new Intent();
                i.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        }
        //绑定服务
        Intent intent = new Intent(this, LocationForegroundService.class);
        isBound= bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }
    
3.同样的Activity销毁后也要解绑Service,为了避免ServiceConnection空指针需要定义一个变量 isBound
    
             @Override
        protected void onDestroy() {
            if (isBound) {
                unbindService(connection)
                isBound = false
            }
            super.onDestroy();
        }
        
到此适配已经完成了。看不明白的同学可以下载demo查看。demo中无 isBound 变量自行按照文章中的方式添加

demo地址：https://github.com/Lans/gpsdemo
