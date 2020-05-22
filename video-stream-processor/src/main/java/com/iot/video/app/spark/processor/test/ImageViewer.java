package com.iot.video.app.spark.processor.test;

import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * @author wangheng
 * @date 2020-05-22
 */
public class ImageViewer {
    private JLabel imageView;
    private Mat image;
    private String windowName;

    /**
     * 如果使用junit测试时调用该方法，图像会一闪而过，可通过sleep()等方式暂时显示
     *
     * @param
     */
    public ImageViewer(Mat image) {
        this.image = image;
    }

    /**
     * @param image      要显示的mat
     * @param windowName 窗口标题
     */
    public ImageViewer(Mat image, String windowName) {
        this.image = image;
        this.windowName = windowName;
    }

    /**
     * 图片显示
     */
    public void imshow() {
        setSystemLookAndFeel();
        Image loadedImage = toBufferedImage(image);
        JFrame frame = createJFrame(windowName, image.width(), image.height());
        imageView.setIcon(new ImageIcon(loadedImage));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        // 用户点击窗口关闭
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    private JFrame createJFrame(String windowName, int width, int height) {
        JFrame frame = new JFrame(windowName);
        imageView = new JLabel();
        final JScrollPane imageScrollPane = new JScrollPane(imageView);
        imageScrollPane.setPreferredSize(new Dimension(width, height));
        frame.add(imageScrollPane, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        return frame;
    }

    private Image toBufferedImage(Mat matrix) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (matrix.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = matrix.channels() * matrix.cols() * matrix.rows();
        byte[] buffer = new byte[bufferSize];
        // 获取所有的像素点
        matrix.get(0, 0, buffer);
        BufferedImage image = new BufferedImage(matrix.cols(), matrix.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
        return image;
    }
}
