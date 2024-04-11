package com.tencent.qqmusic.qplayer.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;


import java.text.DecimalFormat;
import java.util.ArrayList;

@SuppressWarnings("WeakerAccess")
public class QQMusicUtil {

    public final static String FORMAT_T = "TB";
    public final static String FORMAT_G = "GB";
    public final static String FORMAT_M = "MB";
    public final static String FORMAT_K = "KB";
    public final static String FORMAT_B = "B";

    private static final int MAGIC_KB = 1 << 10;
    private static final int MAGIC_MB = 1 << 20;
    private static final int MAGIC_GB = 1 << 30;

    private final static String notLegalText1 = "unknown";
    private final static String notLegalText2 = "未知";

    public static String formatSize(long bytes, int decimal) {
        return formatSize(bytes, decimal, getBestFormat(bytes));
    }

    public static String formatSize(long bytes, int decimal, String formatType) {
        if (bytes <= 0) {
            return 0 + FORMAT_B;
        }
        StringBuilder formatString = new StringBuilder("#0");
        if (decimal > 0) {
            formatString.append(".");
        }
        for (int i = 0; i < decimal; i++) {
            formatString.append("0");
        }
        DecimalFormat df = new DecimalFormat(formatString.toString());
        switch (formatType) {
            case FORMAT_T:
                return df.format((double) (bytes / MAGIC_GB) / MAGIC_KB) + FORMAT_T;
            case FORMAT_G:
                return df.format((double) bytes / MAGIC_GB) + FORMAT_G;
            case FORMAT_M:
                return df.format((double) bytes / MAGIC_MB) + FORMAT_M;
            case FORMAT_K:
                return df.format((double) bytes / MAGIC_KB) + FORMAT_K;
            default:
                return df.format((double) bytes) + FORMAT_B;
        }
    }

    public static String getBestFormat(long bytes) {
        if (bytes / MAGIC_GB > MAGIC_KB) {
            return FORMAT_T;
        } else if (bytes > MAGIC_GB) {
            return FORMAT_G;
        } else if (bytes > MAGIC_MB) {
            return FORMAT_M;
        } else if (bytes > MAGIC_KB) {
            return FORMAT_K;
        } else {
            return FORMAT_B;
        }
    }
}
