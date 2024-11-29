package com.xu.layout.core;

import java.io.InputStream;

/**
 * @author xuzc
 */
public interface FigureResolver {

    /**
     * 解析图像数据
     *
     * @param imageInputStream 图像的字节数据流
     * @param fileName         上传文件名
     * @return 上传后的图像URL
     */
    String resolve(InputStream imageInputStream, String fileName, int byteSize);

}
