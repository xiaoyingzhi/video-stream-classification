package com.iot.video.app.spark.processor;

import com.iot.video.app.spark.util.PropertyFileReader;
import com.iot.video.app.spark.util.VideoEventData;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;
import org.apache.spark.TaskContext;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.MapGroupsWithStateFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.GroupState;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.Iterator;
import java.util.Properties;

/**
 * Class to consume incoming JSON messages from Kafka and process them using Spark Structured Streaming.
 *
 * @author abaghel
 */
@Slf4j
public class VideoStreamProcessor {
    private static final Logger logger = Logger.getLogger(VideoStreamProcessor.class);

    public static void main(String[] args) throws Exception {
        //Read properties
        Properties prop = PropertyFileReader.readPropertyFile();

        //SparkSesion
        SparkSession spark = SparkSession
                .builder()
                .appName("VideoStreamProcessor")
                .master(prop.getProperty("spark.master.url"))
                .getOrCreate();

        //directory to save image files with motion detected
        final String processedImageDir = prop.getProperty("processed.output.dir");
        logger.warn("Output directory for saving processed images is set to " + processedImageDir + ". This is configured in processed.output.dir key of property file.");

        //create schema for json message
        StructType schema = DataTypes.createStructType(new StructField[]{
                DataTypes.createStructField("cameraId", DataTypes.StringType, true),
                DataTypes.createStructField("timestamp", DataTypes.TimestampType, true),
                DataTypes.createStructField("rows", DataTypes.IntegerType, true),
                DataTypes.createStructField("cols", DataTypes.IntegerType, true),
                DataTypes.createStructField("type", DataTypes.IntegerType, true),
                DataTypes.createStructField("data", DataTypes.StringType, true)
        });

        //Create DataSet from stream messages from kafka
        Dataset<VideoEventData> ds = spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", prop.getProperty("kafka.bootstrap.servers"))
                .option("subscribe", prop.getProperty("kafka.topic"))
                .option("kafka.max.partition.fetch.bytes", prop.getProperty("kafka.max.partition.fetch.bytes"))
                .option("kafka.max.poll.records", prop.getProperty("kafka.max.poll.records"))
                .load()
                .selectExpr("CAST(value AS STRING) as message")
                .select(functions.from_json(functions.col("message"), schema).as("json"))
                .select("json.*")
                .as(Encoders.bean(VideoEventData.class));

        System.out.println("111111111" + ds);

        //key-value pair of cameraId-VideoEventData
        KeyValueGroupedDataset<String, VideoEventData> kvDataset = ds.groupByKey(new MapFunction<VideoEventData, String>() {
            @Override
            public String call(VideoEventData value) throws Exception {
                return value.getCameraId();
            }
        }, Encoders.STRING());

        //process
        Dataset<VideoEventData> processedDataset = kvDataset.mapGroupsWithState(new MapGroupsWithStateFunction<String, VideoEventData, VideoEventData, VideoEventData>() {
            @Override
            public VideoEventData call(String key, Iterator<VideoEventData> values, GroupState<VideoEventData> state) throws Exception {
                logger.warn("CameraId=" + key + " PartitionId=" + TaskContext.getPartitionId());
                VideoEventData existing = null;
                //check previous state
                if (state.exists()) {
                    existing = state.get();
                }
                //classify image
                VideoEventData processed = ImageProcessor.process(key, values, processedImageDir, existing);

                //update last processed
                if (processed != null) {
                    state.update(processed);
                }
                return processed;
            }
        }, Encoders.bean(VideoEventData.class), Encoders.bean(VideoEventData.class));

        System.out.println("22222" + processedDataset.toString());

        //start
        StreamingQuery query = processedDataset.writeStream()
                .outputMode("update")
                .format("console")
                .start();

        //await
        query.awaitTermination();
    }

    public static KeyValueGroupedDataset<String, VideoEventData> con() {
        //Read properties
        Properties prop = null;
        try {
            prop = PropertyFileReader.readPropertyFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //SparkSesion
        SparkSession spark = SparkSession
                .builder()
                .appName("VideoStreamProcessor")
                .master(prop.getProperty("spark.master.url"))
                .getOrCreate();

        //directory to save image files with motion detected
        final String processedImageDir = prop.getProperty("processed.output.dir");
        log.warn("Output directory for saving processed images is set to " + processedImageDir + ". This is configured in processed.output.dir key of property file.");

        //create schema for json message
        StructType schema = DataTypes.createStructType(new StructField[]{
                DataTypes.createStructField("cameraId", DataTypes.StringType, true),
                DataTypes.createStructField("timestamp", DataTypes.TimestampType, true),
                DataTypes.createStructField("rows", DataTypes.IntegerType, true),
                DataTypes.createStructField("cols", DataTypes.IntegerType, true),
                DataTypes.createStructField("type", DataTypes.IntegerType, true),
                DataTypes.createStructField("data", DataTypes.StringType, true)
        });

        //Create DataSet from stream messages from kafka
        Dataset<VideoEventData> ds = spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", prop.getProperty("kafka.bootstrap.servers"))
                .option("subscribe", prop.getProperty("kafka.topic"))
                .option("kafka.max.partition.fetch.bytes", prop.getProperty("kafka.max.partition.fetch.bytes"))
                .option("kafka.max.poll.records", prop.getProperty("kafka.max.poll.records"))
                .load()
                .selectExpr("CAST(value AS STRING) as message")
                .select(functions.from_json(functions.col("message"), schema).as("json"))
                .select("json.*")
                .as(Encoders.bean(VideoEventData.class));

        //key-value pair of cameraId-VideoEventData
        return ds.groupByKey((MapFunction<VideoEventData, String>) VideoEventData::getCameraId, Encoders.STRING());
    }
}


