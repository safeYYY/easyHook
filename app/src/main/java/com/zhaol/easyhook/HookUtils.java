package com.zhaol.easyhook;

import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/*******************************************************
 *
 * Created by julis.wang on 2021/06/08 10:52
 *
 * Description :
 *
 * History   :
 *
 *******************************************************/

public final class HookUtils {
    private static final String HEX_DIGITS = "0123456789abcdef";

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

    public static String toHexString(final byte[] byteArray) {
        StringBuilder sb = new StringBuilder(byteArray.length * 2);
        for (byte value : byteArray) {
            int b = value & BYTE_MSK;
            sb.append(HEX_DIGITS.charAt(b >>> HEX_DIGIT_BITS)).append(
                    HEX_DIGITS.charAt(b & HEX_DIGIT_MASK));
        }
        return sb.toString();
    }

    public static String byteArrayToString(byte[] input) {
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

    public static void writeFile(String str) {
        Log.d(HookModule.TAG, str);
        str = str + "\n";
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

}
