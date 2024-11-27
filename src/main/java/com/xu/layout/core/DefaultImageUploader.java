package com.xu.layout.core;

import java.io.InputStream;

/**
 * @author xuzc
 */
public class DefaultImageUploader implements ImageUploader {

    @Override
    public String upload(InputStream imageInputStream, String fileName, int byteSize) {
        return fileName;
    }

}
