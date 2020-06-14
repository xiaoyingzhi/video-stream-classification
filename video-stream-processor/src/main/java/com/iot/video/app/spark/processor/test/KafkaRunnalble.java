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
    private String server;
    private String topic;

    public KafkaRunnalble(DefaultTableModel model, String server, String topic) {
        this.model = model;
        this.server = server;
        this.topic = topic;
    }

    @Override
    public void run() {
        KafkaUtils.getMsgFromKafka(model, server, topic);
    }
}
