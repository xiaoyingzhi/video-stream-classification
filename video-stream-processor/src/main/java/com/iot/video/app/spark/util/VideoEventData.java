package com.iot.video.app.spark.util;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Java Bean to hold JSON message
 *
 * @author abaghel
 */
@Data
public class VideoEventData implements Serializable {

    private String cameraId;
    private Timestamp timestamp;
    private int rows;
    private int cols;
    private int type;
    private String data;
}
