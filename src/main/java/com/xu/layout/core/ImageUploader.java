package com.xu.layout.core;

import java.io.InputStream;

/**
 * @author xuzc
 */
public interface ImageUploader {

    /**
     * 将图像数据上传并返回图像的URL
     *
     * @param imageInputStream 图像的字节数据流
     * @param fileName         上传文件名
     * @return 上传后的图像URL
     */
    String upload(InputStream imageInputStream, String fileName, int byteSize);

}
