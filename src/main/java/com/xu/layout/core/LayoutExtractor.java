package com.xu.layout.core;

import ai.onnxruntime.OrtException;
import com.xu.layout.entity.RegionBlock;
import com.xu.layout.exception.ModelException;
import com.xu.layout.utils.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.xu.layout.utils.Utils.generateUUID;

/**
 * @author xuzc
 */
@Slf4j
@SuppressWarnings("unused")
public class LayoutExtractor {

    private final String drawPredictionsOutputDir;

    private final ModelDetection modelDetection;

    public LayoutExtractor(String drawPredictionsOutputDir) {
        this.drawPredictionsOutputDir = drawPredictionsOutputDir;
        String modelPath = "yolov8n_layout_general6.onnx";
        String labelPath = "labels.names";
        modelDetection = buildModelDetection(modelPath, labelPath);
    }

    public LayoutExtractor(String drawPredictionsOutputDir, String modelPath, String labelPath) {
        this.drawPredictionsOutputDir = drawPredictionsOutputDir;
        modelDetection = buildModelDetection(modelPath, labelPath);
    }

    /**
     * PDF 文件布局检测方法
     * 该方法读取输入流中的 PDF 文件，并对每一页进行布局检测和内容提取
     *
     * @param inputStream PDF 文件的输入流
     */
    public String detectionPdf(InputStream inputStream) {
        log.info("开始执行PDF 文件布局检测");
        long startTime = Instant.now().toEpochMilli();
        StringBuilder stringBuilder = new StringBuilder();
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                PDPage page = document.getPage(pageIndex);
                stringBuilder.append(detectPage(pdfRenderer, page, pageIndex));
            }
            log.info("PDF 文件布局检测结束, 耗时: {}ms", Instant.now().toEpochMilli() - startTime);
            return stringBuilder.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    private String detectPage(PDFRenderer pdfRenderer, PDPage page, int pageIndex) {
        // 将 PDF 页面渲染为图像
        BufferedImage img;
        try {
            // 设置DPI, 提高清晰度
            img = pdfRenderer.renderImageWithDPI(pageIndex, 150);
            // 执行布局检测
            List<Detection> detections = detectLayout(modelDetection, img);
            // 提取标签内容
            List<RegionBlock> regionBlocks = extractPdfContents(img, page, pageIndex, detections);
            // 排序
            List<RegionBlock> sortRegionBlocks = LayoutParser.processLayout(regionBlocks);
            String content = sortRegionBlocks.stream().map(RegionBlock::getSimpleContent).collect(Collectors.joining("\n"));
            // 在图像上绘制检测结果并保存
            drawPredictionsPage(img, pageIndex, sortRegionBlocks);
            return content;
        } catch (IOException e) {
            log.error("detectPage错误， pageIndex: {}", pageIndex, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 布局检测逻辑
     */
    public List<Detection> detectLayout(ModelDetection modelDet, BufferedImage img) {
        try {
            return modelDet.detectObjects(img);
        } catch (OrtException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    /**
     * 从给定的图像和页面中提取标签内容
     * 此方法根据检测到的标签和边界框，从图像或页面中提取相应的内容或图像URL
     *
     * @param img        图像数据，用于提取图像标签的内容
     * @param page       PDF页面对象，用于提取文本标签的内容
     * @param pageIndex  页面索引，用于标识内容来源
     * @param detections 检测到的标签和边界框信息列表
     * @return 返回一个包含标签内容的列表
     */
    public List<RegionBlock> extractPdfContents(BufferedImage img, PDPage page, int pageIndex, List<Detection> detections) {
        // 初始化标签内容列表
        List<RegionBlock> regionBlocks = new ArrayList<>();
        // 对检测结果进行布局排序，以符合页面上的实际排列顺序
        // 遍历每个检测结果，提取其内容或图像URL
        for (Detection detection : detections) {
            // 将提取到的内容添加到标签内容列表中
            regionBlocks.add(extractContentFromRect(img, detection, page, pageIndex));
        }
        // 返回标签内容列表
        return regionBlocks;
    }

    private RegionBlock extractContentFromRect(BufferedImage img, Detection detection, PDPage page, int pageIndex) {
        // 获取检测结果的标签
        String label = detection.getLabel();
        String content;
        // 非图像标签的内容提取
        if (!"Figure".equals(label)) {
            // 边界框区域
            Rectangle rect = buildRect(detection, 40, 5);
            // 从指定区域提取文本内容
            content = extractTextFromRegion(img, rect, pageIndex, page);
        } else {
            // 边界框区域
            Rectangle rect = buildRect(detection, 0, 0);
            // 对于图像标签，上传图像并获取URL
            content = String.format("<img src=\"%s\" /> \n", uploadImage(rect, img, pageIndex));
        }
        // 将提取到的内容添加到标签内容列表中
        return new RegionBlock(label, detection.getLabelIndex(), content, detection.getBbox(), detection.getConfidence());
    }

    /**
     * 构建模型检测对象
     * 此方法负责加载模型和标签文件，如果加载过程中发生任何错误，将抛出自定义异常
     *
     * @param modelPath 模型文件路径
     * @param labelPath 标签文件路径
     * @return 返回一个构建好的模型检测对象
     * @throws ModelException 如果模型或标签文件加载失败，则抛出此异常
     */
    private ModelDetection buildModelDetection(String modelPath, String labelPath) {
        ModelDetection modelDetection;
        try {
            // 尝试使用给定的模型和标签路径构建模型检测对象
            modelDetection = new ModelDetection(modelPath, labelPath);
        } catch (Exception e) {
            // 如果构建过程中发生异常，抛出自定义的模型异常
            throw new ModelException(e);
        }
        return modelDetection;
    }

    /**
     * 使用 ImageUploader 实现的上传图片方法
     */
    private String uploadImage(Rectangle rect, BufferedImage img, int pageIndex) {
        // 裁剪指定区域
        BufferedImage subImage = img.getSubimage(rect.x, rect.y, rect.width, rect.height);
        // 将 BufferedImage 转为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(subImage, "png", baos);
            byte[] imageData = baos.toByteArray();
            FigureResolver figureResolver = new DefaultFigureResolver();
            return figureResolver.resolve(new ByteArrayInputStream(imageData), generateUUID() + "_figure.png", imageData.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 下载检测过的文档页图片，方便对比
     */
    private void drawPredictionsPage(BufferedImage img, int pageIndex, List<RegionBlock> regionBlocks) {
        ImageUtil.drawPredictions(img, regionBlocks);
        Path path = Paths.get(drawPredictionsOutputDir, pageIndex + "---" + generateUUID() + "_detected.png");
        if (path.isAbsolute()) {
            try {
                ImageIO.write(img, "png", new File(path.toString()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Extracts text from a specified region of a PDF page.
     * <p>
     * This method maps an image region to a PDF page region, then uses PDFTextStripperByArea to extract text from that region.
     *
     * @param img        The image matrix, used to determine the size of the region.
     * @param matRect    The rectangle defining the region in the image.
     * @param pageNumber The page number of the PDF document, 0-based.
     * @param page       The PDPage object representing the PDF page.
     * @return The text extracted from the specified region.
     */
    public String extractTextFromRegion(BufferedImage img, Rectangle matRect, int pageNumber, PDPage page) {
        // Map the image region to a PDF rectangle
        Rectangle region = mapMatToPDFRectangle(img, matRect, page);
        // Create a PDFTextStripperByArea instance
        PDFTextStripperByArea stripper;
        try {
            stripper = new PDFTextStripperByArea();
            // Add the region from which you want to extract text (pass the Rectangle region)
            stripper.addRegion("textRegion", region);
            // Set the page range (start and end page) for text extraction
            stripper.setStartPage(pageNumber + 1);  // PDFBox uses 1-based page numbers
            stripper.setEndPage(pageNumber + 1);  // Same for end page
            // Extract text from the specified region
            stripper.extractRegions(page);
            // Get the extracted text from the region
            return stripper.getTextForRegion("textRegion");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Maps a rectangle in image coordinates to a PDF coordinate rectangle, ensuring the rectangle
     * does not exceed image boundaries.
     *
     * @param img     The image providing dimensions.
     * @param matRect The rectangle in the image coordinates to be mapped.
     * @param pdfPage The PDPage object used to get PDF page dimensions.
     * @return A Rectangle object representing the mapped region in PDF coordinates.
     */
    public Rectangle mapMatToPDFRectangle(BufferedImage img, Rectangle matRect, PDPage pdfPage) {
        // 获取图像宽度和高度
        int imageWidth = img.getWidth();
        int imageHeight = img.getHeight();
        // Get the PDF page dimensions
        float pdfWidth = pdfPage.getMediaBox().getWidth();
        float pdfHeight = pdfPage.getMediaBox().getHeight();

        // Calculate the scaling factors
        float scaleX = pdfWidth / imageWidth;
        float scaleY = pdfHeight / imageHeight;

        // Map the OpenCV Rect to the PDF coordinates
        int x = (int) (matRect.x * scaleX);
        int y = (int) (matRect.y * scaleY);
        int width = (int) (matRect.width * scaleX);
        int height = (int) (matRect.height * scaleY);

        return new Rectangle(x, y, width, height);
    }

    /**
     * 构建并适当扩大边界框区域（基于像素值扩展）
     */
    private Rectangle buildRect(Detection detection, int expandWidth, int expandHeight) {
        float[] bbox = detection.getBbox();
        // 计算原始边界框的坐标和大小
        int x = Math.round(bbox[0]);
        int y = Math.round(bbox[1]);
        int width = Math.round(bbox[2] - bbox[0]);
        int height = Math.round(bbox[3] - bbox[1]);

        // 计算扩展后的宽度和高度
        int expandedWidth = width + expandWidth;
        int expandedHeight = height + expandHeight;

        // 计算扩展后的起始位置（确保不超出图像边界）
        int expandedX = Math.max(0, x - (expandWidth / 2));  // 在x方向扩展时，向左偏移
        int expandedY = Math.max(0, y - (expandHeight / 2)); // 在y方向扩展时，向上偏移

        return new Rectangle(expandedX, expandedY, expandedWidth, expandedHeight);
    }

}
