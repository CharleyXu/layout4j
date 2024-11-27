package com.xu.layout.utils;

import java.util.UUID;

/**
 * @author xuzc
 */
public class Utils {

    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
}
