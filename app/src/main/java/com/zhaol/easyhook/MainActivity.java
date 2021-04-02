package com.zhaol.easyhook;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private EditText packageET = null;
    private EditText classET = null;
    private EditText methodET = null;
    private EditText paramET = null;
    private String filePath = null;


    public static Boolean isFloatServiceRunning = false;
    private MyServiceConnection conn;
    private final String[] hookTypeList = {"方法", "构造函数"};
    private Spinner sp;

    public static int SCREEN_HEIGHT;
    public static int SCREEN_WIDTH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        packageET = findViewById(R.id.packageName);
        classET = findViewById(R.id.className);
        methodET = findViewById(R.id.methodName);
        paramET = findViewById(R.id.paramList);
        Button submitBt = findViewById(R.id.submit);
        // clearBt = findViewById(R.id.clear);
        Button showFloatBt = findViewById(R.id.showFloat);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        SCREEN_HEIGHT = metrics.heightPixels;
        SCREEN_WIDTH = metrics.widthPixels;

        showFloatBt.setOnClickListener(v -> {
            if (!isFloatServiceRunning) {
                startFloatingButtonService();
                isFloatServiceRunning = true;
            } else {
                isFloatServiceRunning = false;
                conn = new MyServiceConnection();
                bindService(new Intent(MainActivity.this, FloatingService.class), conn, Context.BIND_ABOVE_CLIENT);
            }
        });

        Button stringBt = findViewById(R.id.stringBt);
        stringBt.setOnClickListener(v -> addValue("String.class"));
        Button intBt = findViewById(R.id.intBt);
        intBt.setOnClickListener(v -> addValue("int.class"));
        Button byteBt = findViewById(R.id.byteBt);
        byteBt.setOnClickListener(v -> addValue("byte[].class"));
        Button booleanBt = findViewById(R.id.booleanBt);
        booleanBt.setOnClickListener(v -> addValue("boolean.class"));
        Button shortBt = findViewById(R.id.shortBt);
        shortBt.setOnClickListener(v -> addValue("short.class"));
        Button longBt = findViewById(R.id.longBt);
        longBt.setOnClickListener(v -> addValue("long.class"));
        Button doubleBt = findViewById(R.id.doubleBt);
        doubleBt.setOnClickListener(v -> addValue("double.class"));
        Button floatBt = findViewById(R.id.floatBt);
        floatBt.setOnClickListener(v -> addValue("float.class"));

        File sdcardDir = Environment.getExternalStorageDirectory();
        filePath = sdcardDir.getAbsolutePath() + "/hookTarget.txt";

        requestPermission();
        initSpinner();
        initValue();

        submitBt.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            sb.append(packageET.getText().toString());
            sb.append("_&_");
            sb.append(classET.getText().toString());
            sb.append("_&_");
            sb.append(methodET.getText().toString());
            sb.append("_&_");
            sb.append(paramET.getText().toString());
            sb.append("_&_");
            sb.append(sp.getSelectedItemId());
            File file = new File(filePath);
            try {
                FileWriter writer = new FileWriter(file);
                writer.append(sb.toString());
                writer.flush();
                writer.close();
                Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void initSpinner() {
        ArrayAdapter<String> starAdapter = new ArrayAdapter<>(this, R.layout.item_select, hookTypeList);
        starAdapter.setDropDownViewResource(R.layout.item_dropdown);
        sp = findViewById(R.id.hookType);
        sp.setPrompt("选择hook类型");
        sp.setAdapter(starAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        conn = new MyServiceConnection();
//        bindService(new Intent(MainActivity.this, FloatingService.class), conn, Context.BIND_ABOVE_CLIENT);
    }

    private void addValue(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(paramET.getText().toString());
        String TAG = "easy_Hook";
        Log.d(TAG, paramET.getText().toString());
        if (!paramET.getText().toString().endsWith(",") && !paramET.getText().toString().equals("")) {
            sb.append(",");
        }
        sb.append(s);
        paramET.setText(sb.toString());
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_PERMISSION_STORAGE = 100;
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(permissions, REQUEST_CODE_PERMISSION_STORAGE);
                    return;
                }
            }
        }
    }

    private void initValue() {
        try {
            FileReader fileReader = new FileReader(filePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = bufferedReader.readLine();
            String[] str = line.split("_&_");
            packageET.setText(str[0]);
            classET.setText(str[1]);
            methodET.setText(str[2]);
            paramET.setText(str[3]);
            sp.setSelection(Integer.parseInt(str[4]));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startFloatingButtonService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT).show();
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
            } else {
                startService(new Intent(MainActivity.this, FloatingService.class));
            }
        } else {
            startService(new Intent(MainActivity.this, FloatingService.class));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                    startService(new Intent(MainActivity.this, FloatingService.class));
                }
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                startService(new Intent(MainActivity.this, FloatingService.class));
            }
        }
    }

    public static class MyServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloatingService.MyBinder myBinder = (FloatingService.MyBinder) service;
            myBinder.closewindow();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }
}
