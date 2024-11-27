package com.xu.layout.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xuzc
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Detection {

    private String label;

    private int labelIndex;

    private float[] bbox;

    private float confidence;

}
