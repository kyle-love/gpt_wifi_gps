package com.example.gpt_wifi_gps;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import android.os.Handler;
public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private LocationManager locationManager;
    private TextView textView;
    private List<ScanResult> wifiList;
    private Location gpsLocation;
    private String wifiInfoString;

    private Handler handler;
    private Runnable wifiScannerRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();
        wifiScannerRunnable = new Runnable() {
            @Override
            public void run() {
                updateWifiInfo();
                handler.postDelayed(this, 3000); // 每隔3秒扫描一次
            }
        };

        // 初始化控件和服务
        textView = findViewById(R.id.textView_info);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Button buttonRefresh = findViewById(R.id.button_refresh);
        Button buttonRecord = findViewById(R.id.button_record);

        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
            }
        }

        // 刷新按钮点击事件
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateWifiInfo();
                updateLocationInfo();
                displayInfo();
            }
        });

        // 记录按钮点击事件
        buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInfoToCsv();
            }
        });
        wifiScannerRunnable.run(); // 在 onCreate 方法末尾调用
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(wifiScannerRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(wifiScannerRunnable);
    }

    // 更新Wi-Fi信息
    private void updateWifiInfo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            wifiManager.startScan();
            wifiList = wifiManager.getScanResults();
        }
    }

    private void updateLocationInfo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    gpsLocation = location;
                }
            });
        }
    }

    private void displayInfo() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("当前时间: ").append(System.currentTimeMillis());
        if (gpsLocation != null) {
            stringBuilder.append("\n经度: ").append(gpsLocation.getLongitude())
                    .append("\n纬度: ").append(gpsLocation.getLatitude())
                    .append("\n精度: ").append(gpsLocation.getAccuracy());
        }
        if (wifiList != null) {
            for (ScanResult result : wifiList) {
                if (result.SSID == null || result.SSID.isEmpty()) {
                    continue;
                }
//                        .append("\n时间戳: ").append(result.timestamp)
                stringBuilder.append("\nSSID: ").append(result.SSID)
                        .append("\nBSSID: ").append(result.BSSID)
                        .append("\n信号强度: ").append(result.level)
                        .append("\n");
            }
        }
        wifiInfoString = stringBuilder.toString();
        textView.setText(wifiInfoString);
    }

    // && gpsLocation != null
    private void saveInfoToCsv() {
        if (wifiInfoString != null && !wifiInfoString.isEmpty() && gpsLocation != null) {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = System.currentTimeMillis() + "_wifi_gps_info.csv";
            File file = new File(path, fileName);
            FileWriter writer = null;

            try {
                writer = new FileWriter(file, false);
                String[] lines = wifiInfoString.split("\n");
                // 添加这个条件检查
                if (lines.length < 4) {
                    Toast.makeText(MainActivity.this, "无法保存信息，请确保获取到正确的信息", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] time_stamp = lines[0].split(" ");
                String[] lontitudeInfo = lines[1].split(" ");
                String[] latitudeInfo = lines[2].split(" ");
                String[] accuracyInfo = lines[3].split(" ");

                // 添加系统毫秒级时间戳
                writer.append(time_stamp[1]).append("\n");

                // 添加GPS信息
                writer.append(lontitudeInfo[1]).append(",")
                        .append(latitudeInfo[1]).append(",")
                        .append(accuracyInfo[1]).append("\n");

                // 添加Wi-Fi信息
                for (int i = 4; i + 2 < lines.length; i += 4) {
                    String[] ssidInfo = lines[i].split(" ");
                    String[] bssidInfo = lines[i + 1].split(" ");
                    String[] levelInfo = lines[i + 2].split(" ");

                    writer.append("\"").append(ssidInfo[1]).append("\"").append(",")
                            .append(bssidInfo[1]).append(",")
                            .append(levelInfo[1]).append("\n");
                    writer.flush(); // 在每次循环中添加 writer.flush();
                }

                Toast.makeText(MainActivity.this, "信息已成功保存到文件 " + fileName, Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "保存信息时发生错误，请检查存储权限", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.flush(); // 将 flush() 移到 finally 语句块中
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (gpsLocation == null) {
            Toast.makeText(MainActivity.this, "GPS信息为空，记录共现关系信息", Toast.LENGTH_SHORT).show();

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = System.currentTimeMillis() + "_wifi_relation.csv";
            File file = new File(path, fileName);
            FileWriter writer = null;

            try {
                writer = new FileWriter(file, false);
                String[] lines = wifiInfoString.split("\n");
                // 添加这个条件检查
                if (lines.length < 4) {
                    Toast.makeText(MainActivity.this, "无法保存信息，请确保获取到正确的信息", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] time_stamp = lines[0].split(" ");
                String[] lontitudeInfo = lines[1].split(" ");
                String[] latitudeInfo = lines[2].split(" ");
                String[] accuracyInfo = lines[3].split(" ");

                // 添加系统毫秒级时间戳
                writer.append(time_stamp[1]).append("\n");

                // 添加GPS信息
                writer.append(lontitudeInfo[1]).append(",")
                        .append(latitudeInfo[1]).append(",")
                        .append(accuracyInfo[1]).append("\n");

                // 添加Wi-Fi信息
                for (int i = 4; i + 2 < lines.length; i += 4) {
                    String[] ssidInfo = lines[i].split(" ");
                    String[] bssidInfo = lines[i + 1].split(" ");
                    String[] levelInfo = lines[i + 2].split(" ");

                    writer.append("\"").append(ssidInfo[1]).append("\"").append(",")
                            .append(bssidInfo[1]).append(",")
                            .append(levelInfo[1]).append("\n");
                    writer.flush(); // 在每次循环中添加 writer.flush();
                }

                Toast.makeText(MainActivity.this, "信息已成功保存到文件 " + fileName, Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "保存信息时发生错误，请检查存储权限", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.flush(); // 将 flush() 移到 finally 语句块中
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

//    else if (gpsLocation == null) {
//        Toast.makeText(MainActivity.this, "GPS信息为空", Toast.LENGTH_SHORT).show();
//    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateWifiInfo();
                updateLocationInfo();
            }
        }
    }
}
