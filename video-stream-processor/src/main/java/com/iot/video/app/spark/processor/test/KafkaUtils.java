package com.iot.video.app.spark.processor.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.iot.video.app.spark.util.PropertyFileReader;
import com.iot.video.app.spark.util.VideoEventData;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.swing.table.DefaultTableModel;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

/**
 * @author wangheng
 * @date 2020-05-22
 */
@Slf4j
public class KafkaUtils {

    private static Table<String, String, Consumer<String, String>> table = HashBasedTable.create();

    private static String outputDir;

    /**
     * 消费者
     */
    static {
        //很重要
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        //Read properties
    }

    /**
     * 从kafka上接收对象消息，将json字符串转化为对象，便于获取消息的时候可以使用get方法获取。
     */
    public static void getMsgFromKafka(DefaultTableModel model, String server, String topic) {
        while (true) {
            ConsumerRecords<String, String> records = KafkaUtils.getKafkaConsumer(server, topic).poll(100);
            if (records.count() > 0) {
                for (ConsumerRecord<String, String> record : records) {
                    com.alibaba.fastjson.JSONObject jsonAlarmMsg = JSON.parseObject(record.value());
                    VideoEventData eventData = JSONObject.toJavaObject(jsonAlarmMsg, VideoEventData.class);
                    double imageWidth = 640;
                    double imageHeight = 480;
                    Size sz = new Size(imageWidth, imageHeight);
                    Mat frame = getMat(eventData);
                    Imgproc.resize(frame, frame, sz);

                    MatOfByte bytemat = new MatOfByte();
                    Imgcodecs.imencode(".jpg", frame, bytemat);
                    saveImageAndData(frame, eventData, outputDir, model);
                }
            }
        }
    }

    public static Consumer<String, String> getKafkaConsumer(String server, String topic) {
        System.out.println(server + "   " + topic);
        if (table.get(server, topic) != null) {
            return table.get(server, topic);
        }
        return connect(server, topic);
    }

    public static Consumer<String, String> connect(String server, String topic) {
        Properties prop = null;
        try {
            prop = PropertyFileReader.readPropertyFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        outputDir = prop.getProperty("processed.output.dir");
        Properties properties = new Properties();
        //服务器ip:端口号，集群用逗号分隔
        properties.put("bootstrap.servers", server);
        properties.put("kafka.max.partition.fetch.bytes", prop.getProperty("kafka.max.partition.fetch.bytes"));
        properties.put("kafka.max.poll.records", prop.getProperty("kafka.max.poll.records"));
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("group.id", "cameraId");
        Consumer<String, String> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Arrays.asList(topic));
        table.put(server, topic, consumer);
        return consumer;
    }

    public static void closeKafkaConsumer() {
        //consumer.close();
    }

    private static void saveImageAndData(Mat mat, VideoEventData ed, String outputDir, DefaultTableModel model) {
        String imagePath = outputDir + ed.getCameraId() + "-T-" + ed.getTimestamp().getTime() + ".jpg";
        log.warn("Saving images to " + imagePath);
        boolean result = Imgcodecs.imwrite(imagePath, mat);
        if (!result) {
            log.error("Couldn't save images to path " + outputDir + ".Please check if this path exists. This is configured in processed.output.dir key of property file.");
        }
        Object[] objects = new Object[]{ed.getCameraId(), imagePath};
        model.addRow(objects);
    }

    private static Mat getMat(VideoEventData ed) {
        Mat mat = new Mat(ed.getRows(), ed.getCols(), ed.getType());
        mat.put(0, 0, Base64.getDecoder().decode(ed.getData()));
        return mat;
    }

}
