package com.xu.layout;

import com.xu.layout.core.DefaultImageUploader;
import com.xu.layout.core.LayoutExtractor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

/**
 * @author xuzc
 */
@Slf4j
public class LayoutDetectionTest {

    @Test
    public void detectTest() throws IOException {
        // 石油报.pdf
        // Attention Is All You Need.pdf
        // 研报.pdf
        String resourceName = "石油报.pdf";
        DefaultImageUploader defaultImageUploader = new DefaultImageUploader();
        String savePath = Paths.get("").toAbsolutePath() + "/img";
        LayoutExtractor pdfLayoutDetection = new LayoutExtractor(defaultImageUploader, true, savePath);
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            String text = pdfLayoutDetection.detectionPdf(inputStream);
            log.info("text: {}", text);
        }
    }

    public static void main(String[] args) {
        String resourceName = "石油报.pdf";
        DefaultImageUploader defaultImageUploader = new DefaultImageUploader();
        String savePath = Paths.get("").toAbsolutePath() + "/img";
        LayoutExtractor pdfLayoutDetection = new LayoutExtractor(defaultImageUploader, true, savePath);
        try (InputStream inputStream = LayoutDetectionTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            String text = pdfLayoutDetection.detectionPdf(inputStream);
            log.info("text: {}", text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
