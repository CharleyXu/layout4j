package com.xu.layout.core;

import ai.onnxruntime.*;
import com.xu.layout.utils.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

/**
 * @author xuzc
 */
@Slf4j
public class ModelDetection {

    float confThreshold;
    float iouThreshold;
    int inputHeight;
    int inputWidth;
    long[] inputShape;
    int numInputElements;
    OnnxJavaType inputType;
    OrtEnvironment env;
    OrtSession session;
    String inputName;
    String outputName;

    long rawImgHeight;
    long rawImgWidth;

    public List<String> labelNames;

    public ModelDetection(String modelPath, String labelPath) throws OrtException {
        this(modelPath, labelPath, 0.3f, 0.5f);
    }

    public ModelDetection(String modelPath, String labelPath, float confThres, float iouThres) throws OrtException {
        this.confThreshold = confThres;
        this.iouThreshold = iouThres;
        initializeModel(modelPath);
        initializeLabel(labelPath);
    }

    private void initializeModel(String modelPath) throws OrtException {
        this.initializeModel(modelPath, -1);
    }

    private void initializeModel(String modelPath, int gpuDeviceId) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
        sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        if (gpuDeviceId >= 0) {
            sessionOptions.addCPU(false);
            sessionOptions.addCUDA(gpuDeviceId);
        } else {
            sessionOptions.addCPU(true);
        }
        this.session = this.env.createSession(modelPath, sessionOptions);

        Map<String, NodeInfo> inputMetaMap = this.session.getInputInfo();
        this.inputName = this.session.getInputNames().iterator().next();
        NodeInfo inputMeta = inputMetaMap.get(this.inputName);
        this.inputType = ((TensorInfo) inputMeta.getInfo()).type;
        this.inputShape = ((TensorInfo) inputMeta.getInfo()).getShape();
        this.numInputElements = (int) (this.inputShape[1] * this.inputShape[2] * this.inputShape[3]);
        this.inputHeight = (int) this.inputShape[2];
        this.inputWidth = (int) this.inputShape[3];
        this.outputName = this.session.getOutputNames().iterator().next();
    }

    private void initializeLabel(String labelPath) {
        try (InputStream resourceAsStream = this.getClass().getResourceAsStream(labelPath)) {
            if (Objects.nonNull(resourceAsStream)) {
                labelNames = IOUtils.readLines(resourceAsStream, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Detection> detectObjects(BufferedImage img) throws OrtException {
        Map<String, OnnxTensor> inputMap = this.prepareInput(img);
        float[][] predictions = inference(inputMap);
        return this.processOutput(predictions);
    }

    private float[][] inference(Map<String, OnnxTensor> inputMap) throws OrtException {
        OrtSession.Result result = this.session.run(inputMap);
        return ((float[][][]) result.get(0).getValue())[0];
    }

    private Map<String, OnnxTensor> prepareInput(BufferedImage img) throws OrtException {
        // 1. 获取图像原始尺寸
        this.rawImgHeight = img.getHeight();
        this.rawImgWidth = img.getWidth();

        // 2. 转换为 RGB 格式
        BufferedImage rgbImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics g = rgbImage.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();

        // 3. 调整大小到模型要求的尺寸
        BufferedImage resizedImage = new BufferedImage(inputWidth, inputHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(rgbImage, 0, 0, inputWidth, inputHeight, null);
        g2d.dispose();

        // 4. 转换为浮点数组并归一化
        byte[] pixelData = ((DataBufferByte) resizedImage.getRaster().getDataBuffer()).getData();
        float[] whc = new float[numInputElements];
        for (int i = 0; i < pixelData.length; i++) {
            whc[i] = (pixelData[i] & 0xFF) / 255.0f; // 将 0-255 的值归一化到 0-1
        }

        // 5. 重排列数据为 CHW 格式
        float[] chw = ImageUtil.whc2cwh(whc); // 重用已有的 whc2cwh 方法

        // 6. 准备 ONNX Tensor
        FloatBuffer inputBuffer = FloatBuffer.wrap(chw);
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputBuffer, inputShape);

        // 7. 返回输入映射
        Map<String, OnnxTensor> inputMap = new HashMap<>();
        inputMap.put(inputName, inputTensor);
        return inputMap;
    }

    private List<Detection> processOutput(float[][] predictions) {
        // prediction
        predictions = transposeMatrix(predictions);
        Map<Integer, List<float[]>> class2Bbox = new HashMap<>();
        for (float[] bbox : predictions) {
            float[] condProb = Arrays.copyOfRange(bbox, 4, bbox.length);
            int label = predMax(condProb);
            float conf = condProb[label];
            if (conf < this.confThreshold) {
                continue;
            }
            bbox[4] = conf;
            // xmin, ymin, xmax, ymax -> (xmin_raw, ymin_raw, xmax_raw, ymax_raw)
            rescaleBoxes(bbox);
            // xywh -> (x1, y1, x2, y2)
            ImageUtil.xywh2xyxy(bbox);
            //skip invalid prediction
            if (bbox[0] >= bbox[2] || bbox[1] >= bbox[3]) {
                continue;
            }
            class2Bbox.putIfAbsent(label, new ArrayList<>());
            class2Bbox.get(label).add(bbox);
        }
        //apply non-max suppression for each class
        List<Detection> detectionList = new ArrayList<>();
        for (Map.Entry<Integer, List<float[]>> entry : class2Bbox.entrySet()) {
            int label = entry.getKey();
            List<float[]> bboxList = entry.getValue();
            bboxList = ImageUtil.nonMaxSuppression(bboxList, this.iouThreshold);
            for (float[] bbox : bboxList) {
                if (label < labelNames.size()) {
                    String labelString = labelNames.get(label);
                    detectionList.add(new Detection(labelString, label, Arrays.copyOfRange(bbox, 0, 4), bbox[4]));
                } else {
                    log.warn("label: {}", label);
                }
            }
        }
        return detectionList;
    }

    public float[][] transposeMatrix(float[][] matrix) {
        float[][] transMatrix = new float[matrix[0].length][matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                transMatrix[j][i] = matrix[i][j];
            }
        }
        return transMatrix;
    }

    private int predMax(float[] probabilities) {
        float maxVal = Float.NEGATIVE_INFINITY;
        int idx = 0;
        for (int i = 0; i < probabilities.length; i++) {
            if (probabilities[i] > maxVal) {
                maxVal = probabilities[i];
                idx = i;
            }
        }
        return idx;
    }

    public void rescaleBoxes(float[] bbox) {
        bbox[0] /= this.inputWidth;
        bbox[0] *= this.rawImgWidth;
        bbox[1] /= this.inputHeight;
        bbox[1] *= this.rawImgHeight;
        bbox[2] /= this.inputWidth;
        bbox[2] *= this.rawImgWidth;
        bbox[3] /= this.inputHeight;
        bbox[3] *= this.rawImgHeight;
    }

}
