package com.ryeex.groot.lib.common.util;

import java.util.Random;

/**
 * Created by chenhao on 2017/10/17.
 */

public class RandomUtil {

    private static Random mRandom;

    public static double randomFloat() {
        if (mRandom == null) {
            mRandom = new Random();
            mRandom.setSeed(System.currentTimeMillis());
        }
        return mRandom.nextDouble();
    }

    public static int randomInt() {
        if (mRandom == null) {
            mRandom = new Random();
            mRandom.setSeed(System.currentTimeMillis());
        }
        return mRandom.nextInt();
    }

    public static int randomInt(int bound) {
        if (mRandom == null) {
            mRandom = new Random();
            mRandom.setSeed(System.currentTimeMillis());
        }
        return mRandom.nextInt(bound);
    }
}
