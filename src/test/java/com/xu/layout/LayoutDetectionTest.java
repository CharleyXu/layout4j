package com.xu.layout;

import cn.hutool.core.io.resource.ResourceUtil;
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
        String savePath = Paths.get("").toAbsolutePath() + "/img";
        LayoutExtractor pdfLayoutDetection = new LayoutExtractor(savePath);
        try (InputStream inputStream = ResourceUtil.getStream("石油报.pdf")) {
            String text = pdfLayoutDetection.detectionPdf(inputStream);
            log.info("text: {}", text);
        }
    }

}
