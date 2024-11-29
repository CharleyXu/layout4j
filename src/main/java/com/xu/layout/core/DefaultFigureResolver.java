package com.xu.layout.core;

import java.io.InputStream;

/**
 * @author xuzc
 */
public class DefaultFigureResolver implements FigureResolver {

    @Override
    public String resolve(InputStream imageInputStream, String fileName, int byteSize) {
        return fileName;
    }

}
