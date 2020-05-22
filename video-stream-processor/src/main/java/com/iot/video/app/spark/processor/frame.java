package com.iot.video.app.spark.processor;

import com.iot.video.app.spark.processor.test.ImageViewer;
import com.iot.video.app.spark.processor.test.KafkaRunnalble;
import com.iot.video.app.spark.processor.test.KafkaUtils;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@Slf4j
public class frame extends JFrame implements ActionListener {

    static {
        //很重要
        System.out.println(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static JFrame frame;
    private static JPanel panel;
    private static JButton StartButton;
    private static JButton ShowButton;
    private static JButton StopButton;
    private static JButton CleanButton;
    private static JLabel TextLabel;
    private static DefaultTableModel model;
    private static JTable ListTable;
    private static JScrollPane Jpane;

    public frame() {
        setTitle("可视化工具");
        // 新建面板
        panel = new JPanel();
        panel.setLayout(null);
        ShowButton = new JButton("显示");
        ShowButton.setFont(new Font("宋体", Font.PLAIN, 16));
        StartButton = new JButton("开始");
        StartButton.setFont(new Font("宋体", Font.PLAIN, 16));
        StopButton = new JButton("停止");
        StopButton.setFont(new Font("宋体", Font.PLAIN, 16));
        CleanButton = new JButton("清除");
        CleanButton.setFont(new Font("宋体", Font.PLAIN, 16));

        TextLabel = new JLabel("识别列车图片列表");
        TextLabel.setFont(new Font("宋体", Font.PLAIN, 16));

        String[] columns = {"CamId", "imag"};
        Object[][] data = {{
                "15436", "pic/1.png"
        }, {
                "15437", "pic/logo.jpeg"
        }};
        model = new DefaultTableModel(data, columns);
        ListTable = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ShowButton.setBounds(20, 180, 80, 30);
        StopButton.setBounds(150, 180, 80, 30);
        StartButton.setBounds(410, 180, 80, 30);

        CleanButton.setBounds(280, 180, 80, 30);
        TextLabel.setBounds(40, 250, 200, 30);
        ListTable.setBounds(20, 300, 300, 200);
        Jpane = new JScrollPane(ListTable);
        Jpane.setBounds(20, 300, 300, 200);
        panel.add(ShowButton);
        panel.add(StopButton);
        panel.add(CleanButton);
        panel.add(StartButton);
        panel.add(TextLabel);
        panel.add(Jpane);

        ShowButton.addActionListener(this);
        StopButton.addActionListener(this);
        StartButton.addActionListener(this);
        CleanButton.addActionListener(this);
    }

    public static void main(String[] s) {
        frame = new frame();

        // Setting the width and height of frame
        frame.setSize(700, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        frame.getContentPane().add(panel);
        frame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //开始按钮
        if (e.getSource().equals(StartButton)) {
            KafkaRunnalble kafkaRunnalble = new KafkaRunnalble(model);
            new Thread(kafkaRunnalble).start();
            System.out.println("连接成功");
        }

        //停止按钮
        if (e.getSource().equals(StopButton)) {
            closeConnection();
        }

        //展示按钮
        if (e.getSource().equals(ShowButton)) {
            int row = ListTable.getSelectedRow();
            if (row == -1) {
                //todo 实际上可以改成提示框
                System.out.println("请选择需要展示的图片");
                return;
            }
            showPic((String) model.getValueAt(row, 1));
        }

        //清除按钮
        if (e.getSource().equals(CleanButton)) {
            cleanAll();
        }
    }

    //连接kafka
    private void getConnection() {
        KafkaUtils.getMsgFromKafka(model);
    }

    //关闭kafka
    private void closeConnection() {
        KafkaUtils.closeKafkaConsumer();
        System.out.println("已经关闭kafka连接");
    }

    private void cleanAll() {
        System.out.println("已经清除所有东西");
    }

    private void showPic(String url) {
        Mat mat = Imgcodecs.imread(url);
        ImageViewer viewer = new ImageViewer(mat);
        viewer.imshow();
    }
}

