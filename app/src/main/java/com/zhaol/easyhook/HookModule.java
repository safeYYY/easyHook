package com.zhaol.easyhook;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookModule implements IXposedHookLoadPackage {
    private final String TAG = "easy_Hook";
    private String packageName = null;
    private String className = null;
    private String methodName = null;
    private int hookType = 0;
    private Class<?>[] paramList = null;
    private static final String HEX_DIGITS = "0123456789abcdef";
    static String strClassName = "";

    /**
     * Byte mask.
     */
    private static final int BYTE_MSK = 0xFF;

    /**
     * Hex digit mask.
     */
    private static final int HEX_DIGIT_MASK = 0xF;

    /**
     * Number of bits per Hex digit (4).
     */
    private static final int HEX_DIGIT_BITS = 4;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        getValue(lpparam.classLoader);
        if (lpparam.packageName.equals(packageName)) {
            try {
                Log.d(TAG, "已经hook到了指定app: " + lpparam.packageName);
                writeFile("已经hook到了指定app: " + lpparam.packageName + "\n");
                final Class<?> claszz = lpparam.classLoader.loadClass(className);  //指定类名和包名
                getValue(lpparam.classLoader);
                switch (hookType) {
                    case 0: {
                        Method m = XposedHelpers.findMethodExact(claszz, methodName, paramList);
                        hook(m);
                        break;
                    }
                    case 1: {
                        Constructor<?> m = XposedHelpers.findConstructorExact(claszz, paramList);
                        hook(m);
                        break;
                    }
                    default: {
                        Log.d(TAG, "建设中。。。");
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "easyHook: " + e);
                findContext();
            }
        }
    }

    private void findContext() {
        XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!param.hasThrowable()) {
                    try {
                        String strClazz = param.args[0].toString();
                        if (!strClazz.startsWith("android.")
                                && !strClazz.startsWith(".system")
                                && !strClazz.startsWith("androidx.")
                                && !strClazz.startsWith("java.")
                                && !strClazz.startsWith("org.")
                                && !strClazz.contains("umeng.")
                                && !strClazz.contains("easyhook.")
                                && !strClazz.contains("com.google")
                                // && !strClazz.contains(".alipay")
                                && !strClazz.contains(".netease")
                                // && !strClazz.contains(".alibaba")
                                && !strClazz.contains(".pgyersdk")
                                && !strClazz.contains(".daohen")
                                && !strClazz.contains("mini")
                                && !strClazz.contains("xposed")) {
                            Class<?> clazz = (Class<?>) param.getResult();
                            synchronized (this.getClass()) {
                                strClassName = strClazz;
                                // 获取被hook的目标类的名称
                                if (strClazz.equals(className)) {
                                    Log.d(TAG, "loadClass: " + strClazz);
                                    Method[] methods = clazz.getDeclaredMethods();
                                    // 遍历类的所有方法
                                    if (methods.length > 0) {
                                        for (Method method : methods) {
                                            if (method.getName().equals(methodName)) {
                                                hook(method);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    private void hook(Member m) {
        Log.d(TAG, "method-tostring: " + m.toString());
        XposedBridge.hookMethod(m, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Log.d(TAG, "hook到了" + m.toString());
                writeFile("hook到了" + m.toString() + "\n");
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                for (int i = 0; i < param.args.length; i++) {
                    if (param.args[i] instanceof byte[]) {
                        Log.d(TAG, "参数[" + i + "]:" + byteArrayToString((byte[]) param.args[i]));
                        writeFile("参数[" + i + "]:(" + byteArrayToString((byte[]) param.args[i]) + ")或者(" + toHexString((byte[]) param.args[i]) + ")\n");
                    } else {
                        Log.d(TAG, "参数[" + i + "]:" + param.args[i].toString());
                        writeFile("参数[" + i + "]:" + param.args[i].toString() + "\n");
                    }
                }
                try {
                    Log.d(TAG, "返回值：" + param.getResult());
                    writeFile("返回值：" + param.getResult() + "\n");
                } catch (Exception e) {
                    Log.e(TAG, "line 195:" + e);
                }
            }
        });
    }

    public static String toHexString(final byte[] byteArray) {
        StringBuilder sb = new StringBuilder(byteArray.length * 2);
        for (byte value : byteArray) {
            int b = value & BYTE_MSK;
            sb.append(HEX_DIGITS.charAt(b >>> HEX_DIGIT_BITS)).append(
                    HEX_DIGITS.charAt(b & HEX_DIGIT_MASK));
        }
        return sb.toString();
    }

    private static String byteArrayToString(byte[] input) {
        if (input == null)
            return "";
        String out = new String(input);
        int tmp = 0;
        for (int i = 0; i < out.length(); i++) {
            int c = out.charAt(i);

            if (c >= 32 && c < 127) {
                tmp++;
            }
        }
        if (tmp > (out.length() * 0.60)) {
            out = new String(input);
        } else {
            out = Base64.encodeToString(input, Base64.NO_WRAP).substring(0, 20) + "......";
        }
        return out;
    }

    private void writeFile(String str) {
        File sdcardDir = Environment.getExternalStorageDirectory();
        String filePath = sdcardDir.getAbsolutePath() + "/hookResult.txt";
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(filePath, true)));
            writer.write(str);
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getValue(ClassLoader classLoader) {
        File sdcardDir = Environment.getExternalStorageDirectory();
        String filePath = sdcardDir.getAbsolutePath() + "/hookTarget.txt";
        try {
            FileReader fileReader = new FileReader(filePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = bufferedReader.readLine();
            String[] str = line.split("_&_");
            packageName = str[0];
            // writeFile("找到类名：" + className);
            className = str[1];
            // writeFile("找到类名：" + className);
            methodName = str[2];
            // writeFile("找到方法名：" + methodName);
            String params = str[3];
            hookType = Integer.parseInt(str[4]);
            String[] strList = params.split(",");
            paramList = new Class<?>[strList.length];

            for (int i = 0; i < strList.length; i++) {
                switch (strList[i]) {
                    case "int.class":
                        paramList[i] = int.class;
                        break;
                    case "String.class":
                        paramList[i] = String.class;
                        break;
                    case "byte[].class":
                        paramList[i] = byte[].class;
                        break;
                    case "boolean.class":
                        paramList[i] = boolean.class;
                        break;
                    case "short.class":
                        paramList[i] = short.class;
                        break;
                    case "long.class":
                        paramList[i] = long.class;
                        break;
                    case "double.class":
                        paramList[i] = double.class;
                        break;
                    case "float.class":
                        paramList[i] = float.class;
                        break;
                    default:
                        try {
                            paramList[i] = Class.forName(strList[i]);
                        } catch (Exception e) {
                            paramList[i] = classLoader.loadClass(strList[i]);
                        }
                        break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
