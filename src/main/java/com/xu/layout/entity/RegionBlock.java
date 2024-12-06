package com.xu.layout.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xuzc
 * <p>
 * 区域块
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionBlock {

    // [x1, y1, x2, y2]
    public String label;

    private int labelIndex;

    public String content;

    public float[] bbox;

    public float confidence;

    public String getSimpleContent() {
        return content.replace("\n", "").replace("\r", "");
    }

}
