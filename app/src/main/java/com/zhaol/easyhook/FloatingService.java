package com.zhaol.easyhook;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import static java.lang.Thread.sleep;

public class FloatingService extends Service {
    public FloatingService() {
    }

    public static boolean isStarted = false;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private boolean running = false;
    private TextView hookResult;
    private ScrollView scrollView;
    private LinearLayout linearLayout;
    private TextView clear;
    private Handler handler = null;
    private TextView move;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        // throw new UnsupportedOperationException("Not yet implemented");
        return new MyBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isStarted = true;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.width = (int) (MainActivity.SCREEN_WIDTH * 0.6);
        layoutParams.height = (int) (MainActivity.SCREEN_HEIGHT * 0.32);
        layoutParams.x = 0;
        layoutParams.y = 50;

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                String text = (String) msg.obj;
                if (!hookResult.getText().toString().equals(text)) {
                    hookResult.setText(text);
                    hookResult.clearFocus();
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
                return false;
            }
        });
    }

    public void closeWindow() {
        try {
            windowManager.removeView(linearLayout);
            running = false;
            MainActivity.isFloatServiceRunning = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class MyBinder extends Binder {
        public void closewindow() {
            closeWindow();
        }
    }

    private void readValue() {
        new Thread() {
            public void run() {
                while (running) {
                    File sdcardDir = Environment.getExternalStorageDirectory();
                    String filePath = sdcardDir.getAbsolutePath() + "/hookResult.txt";
                    int length = 0;
                    StringBuffer sb = new StringBuffer();
                    String line = "";
                    try {
                        FileReader fileReader = new FileReader(filePath);
                        BufferedReader bufferedReader = new BufferedReader(fileReader);
                        while ((line = bufferedReader.readLine()) != null) {
                            if (line.isEmpty()) {
                                break;
                            } else {
                                sb.append(line + "\n");
                            }
                            length += line.length();
                        }
                        fileReader.close();
                    } catch (Exception ignored) {
                    }
                    Message message = Message.obtain();
                    message.obj = sb.toString();
                    handler.sendMessage(message);
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //button.setText(sb.toString());
            }
        }.start();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showFloatingWindow();
        readValue();
        return super.onStartCommand(intent, flags, startId);
    }

    private void showFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                show();
            }
        } else {
            show();
        }
    }

    private void show() {
        hookResult = new TextView(getApplicationContext());
        hookResult.setTextColor(Color.WHITE);
        hookResult.setTextSize(12);
        hookResult.setBackgroundColor(Color.BLACK);
        hookResult.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        hookResult.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("Labbel", hookResult.getText().toString());
                cm.setPrimaryClip(clipData);
                Toast.makeText(com.zhaol.easyhook.FloatingService.this, "成功复制到剪切板", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        clear = new TextView(getApplicationContext());
        clear.setText("清空");
        clear.setTextSize(12);
        clear.setBackgroundColor(Color.GRAY);
        clear.setTextColor(Color.RED);

        move = new TextView(getApplicationContext());
        move.setBackgroundColor(Color.GRAY);
        move.setText("  X  ");
        move.setGravity(Gravity.RIGHT);
        move.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 40));


        clear.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));

        scrollView = new ScrollView(getApplicationContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (MainActivity.SCREEN_HEIGHT * 0.32) - 80));
        scrollView.addView(hookResult);

        linearLayout = new LinearLayout(getApplicationContext());
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (MainActivity.SCREEN_HEIGHT * 0.32)));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(Color.BLACK);
        linearLayout.setGravity(Gravity.RIGHT);
        linearLayout.setAlpha((float) 0.555);

        linearLayout.addView(move);
        linearLayout.addView(scrollView);
        linearLayout.addView(clear);

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File sdcardDir = Environment.getExternalStorageDirectory();
                String resultPath = sdcardDir.getAbsolutePath() + "/hookResult.txt";
                File file = new File(resultPath);
                try {
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        move.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeWindow();
            }
        });


        windowManager.addView(linearLayout, layoutParams);

        linearLayout.setOnTouchListener(new FloatingOnTouchListener());
        running = true;
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    windowManager.updateViewLayout(linearLayout, layoutParams);
                    break;
                default:
                    break;
            }
            return false;
        }
    }

}