package com.iot.video.app.spark.processor.test;

import lombok.Data;

import javax.swing.table.DefaultTableModel;

/**
 * @author wangheng
 * @date 2020-05-22
 */
@Data
public class KafkaRunnalble implements Runnable {

    private DefaultTableModel model;

    public KafkaRunnalble(DefaultTableModel model) {
        this.model = model;
    }

    @Override
    public void run() {
        KafkaUtils.getMsgFromKafka(model);
    }
}
