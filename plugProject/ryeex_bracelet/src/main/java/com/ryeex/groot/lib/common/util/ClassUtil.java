package com.ryeex.groot.lib.common.util;

import java.util.Stack;

/**
 * Created by chenhao on 2017/8/15.
 */

public class ClassUtil {
    public static Class<?> guessClass(String clazz) throws ClassNotFoundException {
        Stack<String> innerClassStack = new Stack<String>();
        Class<?> clz = null;
        while (clazz != null) {
            try {
                clz = Class.forName(clazz);
            } catch (ClassNotFoundException e) {
                // ignore
            }

            if (clz != null) {
                while (!innerClassStack.isEmpty() && clz != null) {
                    Class<?>[] innerClasses = clz.getClasses();
                    String c = innerClassStack.pop();
                    clz = null;
                    for (Class<?> ic : innerClasses) {
                        if (ic.getSimpleName().equals(c)) {
                            clz = ic;
                        }
                    }
                }
                break;
            } else {
                int dotIndex = clazz.lastIndexOf('.');
                if (dotIndex > 0 && dotIndex < clazz.length() - 1) {
                    innerClassStack.add(clazz.substring(dotIndex + 1));
                    clazz = clazz.substring(0, dotIndex);
                } else {
                    clazz = null;
                }
            }
        }
        if (clz == null) {
            throw new ClassNotFoundException("failed to guess class: " + clazz);
        }
        return clz;
    }
}
