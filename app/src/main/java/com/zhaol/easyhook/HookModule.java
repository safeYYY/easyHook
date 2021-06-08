package com.zhaol.easyhook;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.zhaol.easyhook.HookUtils.byteArrayToString;
import static com.zhaol.easyhook.HookUtils.toHexString;
import static com.zhaol.easyhook.HookUtils.writeFile;

public class HookModule implements IXposedHookLoadPackage {
    public static final String TAG = "easy_Hook";
    private String packageName = null;
    private String className = null;
    private String methodName = null;
    private boolean isStartHook = false;
    private int hookType = 0; // 0->method 1->construct

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        initValue();
        String curProcessName = lpparam.processName;
        if (curProcessName == null || !curProcessName.equals(packageName)) {
            return;
        }
        prepareHook(lpparam);
    }

    /**
     * 对于加固的 App 找到对应的 Application并 attach
     *
     * @param lpparam
     */
    private void prepareHook(XC_LoadPackage.LoadPackageParam lpparam) {
        List<String> applicationList = Arrays.asList(
                "com.stub.StubApp",
                "com.wrapper.proxyapplication.WrapperProxyApplication"
        );
        String attachMethod = "attachBaseContext";

        Class<?> applicationClass = null;
        for (String appClassString : applicationList) {
            applicationClass = XposedHelpers.findClassIfExists(appClassString, lpparam.classLoader);
            if (applicationClass != null) {
                Log.e(TAG, "Found reinforcement application: " + appClassString);
                break;
            }
        }

        if (applicationClass == null) {
            applicationClass = Application.class;
            attachMethod = "attach";
            Log.e(TAG, "Not Found reinforcement application, will use default Application to attach.");
        }

        XposedHelpers.findAndHookMethod(applicationClass, attachMethod, Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!isStartHook) {
                            Context context = (Context) param.args[0];
                            ClassLoader loader = context.getClassLoader();
                            startHook(loader);
                            isStartHook = true;
                        }
                    }
                }
        );
    }

    private void startHook(ClassLoader loader) {
        Class<?> clazz = null;
        try {
            clazz = loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (clazz == null) {
            Log.e(TAG, "Not found class named " + className);
            return;
        }
        if (hookType == 0) {
            Method[] methods = clazz.getDeclaredMethods();
            if (methods.length > 0) {
                for (Method method : methods) {
                    if (method.getName().equals(methodName)) {
                        hook(method);
                    }
                }
            }
        } else {
            Constructor<?>[] constructors = clazz.getConstructors();
            if (constructors.length > 0) {
                for (Constructor<?> c : constructors) {
                    hook(c);
                }
            }
        }

    }

    private void hook(Member m) {
        Log.d(TAG, "method-toString: " + m.toString());
        XposedBridge.hookMethod(m, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                writeFile("hook到了" + m.toString());
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                for (int i = 0; i < param.args.length; i++) {
                    if (param.args[i] instanceof byte[]) {
                        writeFile("参数[" + i + "]:(" + byteArrayToString((byte[]) param.args[i])
                                + ")或者(" + toHexString((byte[]) param.args[i]) + ")");
                    } else {
                        writeFile("参数[" + i + "]:" + param.args[i].toString());
                    }
                }
                try {
                    writeFile("返回值：" + param.getResult());
                } catch (Exception e) {
                    Log.e(TAG, "Easy hook exception:" + e);
                }
            }
        });
    }

    /**
     * File content like:
     * com.xx.xxx&_com.xxx.sdk.openadsdk.xxx_&_test_&_1
     */
    private void initValue() {
        File sdcardDir = Environment.getExternalStorageDirectory();
        String filePath = sdcardDir.getAbsolutePath() + "/hookTarget.txt";
        try {
            FileReader fileReader = new FileReader(filePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = bufferedReader.readLine();
            String[] str = line.split("_&_");
            packageName = str[0];
            className = str[1];
            methodName = str[2];
            hookType = Integer.parseInt(str[3]);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
