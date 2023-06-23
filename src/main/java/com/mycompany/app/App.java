package com.mycompany.app;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.io.InputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import com.fazecast.jSerialComm.SerialPort;

public class App {
    public static final int QUEUE_MAX_SIZE = 4000;
    private static Deque<Integer> queue = new LinkedList<>();
    private static double[][] data = new double[2][QUEUE_MAX_SIZE];
    private static DefaultXYDataset dataset = new DefaultXYDataset();
    private static JFreeChart chart;
    private static ChartFrame frame;

    // 创建定时任务以更新图表数据并刷新图表
    public static TimerTask updateDataTask = new TimerTask() {
        @Override
        public void run() {
            int i = 0;
            for (Integer number : queue) {
                if (number == 0) {
                    System.out.println("00000000000000000000000000000000000000000000000000");
                }
                data[0][i] = i++;
                data[1][i] = number-2000;
            }
            if(i<QUEUE_MAX_SIZE){
                int tem_i = i;
                data[0][i] = tem_i;
                for(;i<QUEUE_MAX_SIZE;i++){
                    try{
                        data[1][i] = queue.getLast()-2000;
                    }catch(Exception e){
                        data[1][i] = 0;
                    }
                }
            }
            dataset.removeSeries("Values");
            dataset.addSeries("Values", data);
            chart.fireChartChanged();
            //frame.dispose();
            //frame = new ChartFrame("实时折线图", chart);
            //frame.pack();
            //frame.setVisible(true);
            frame.repaint();
        }
    };

    public static void main(String[] args) {
        System.out.println("Hello World!");
        SerialPort[] comPorts = SerialPort.getCommPorts();
        System.out.println("Select a port:");
        int i = 1;
        for (SerialPort port : comPorts) {
            System.out.println(i++ + ": " + port.getSystemPortName());
        }
        int chosenPort;
        try {
            chosenPort = Integer.parseInt(System.console().readLine());
        } catch (Exception e) {
            System.out.println("Invalid port.");
            return;
        }
        SerialPort comPort = comPorts[chosenPort - 1];
        comPort.openPort();
        comPort.setComPortParameters(115200, 8, 1, 0);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
        InputStream in = comPort.getInputStream();

        // 创建图表并显示
        chart = createChart();
        frame = new ChartFrame("实时折线图", chart);
        frame.pack();
        frame.setVisible(true);
        
        Timer timer = new Timer();
        timer.schedule(updateDataTask, 0, 1000); // 每秒更新一次数据
        int countBetweenPuse = 0;
        try {
            Scanner scanner = new Scanner(in);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                //if (line.matches("V=\\d{4}mV")) {
                //    int number = Integer.parseInt(line.substring(2, 6));
                //    queue.offer(number);
                //    System.out.println(number);
                //    if (queue.size() >= QUEUE_MAX_SIZE) {
                //        queue.poll();
                //    }
                //}
                int number = Integer.parseInt(line);
                countBetweenPuse++;
                if (number < 2200 && countBetweenPuse > 10) {
                    Double rate = 60.0 / (countBetweenPuse * 0.01);
                    System.out.println("rate: " + rate);
                    if (rate > 100) {
                        System.out.println("心率过高请注意休息");
                    } else if (rate < 55) {
                        System.out.println("心率过低");
                    }
                    //System.out.println(60.0/(countBetweenPuse*0.01));
                    countBetweenPuse = 0;
                }
                queue.offer(number);
                //System.out.println(number);
                if (queue.size() >= QUEUE_MAX_SIZE) {
                    queue.poll();
                }
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }



    }


    // 创建折线图
    private static JFreeChart createChart() {
        int i = 0;
        for (Integer number : queue) {
            data[0][i] = i++;
            data[1][i] = number;
        }
        dataset.addSeries("Values", data);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Real-time Line Chart", // 图表标题
                "X", // x轴标签
                "Y", // y轴标签
                dataset, // 数据集
                PlotOrientation.VERTICAL, // 图表方向
                true, // 是否显示图例
                true, // 是否生成工具
                false // 是否生成URL链接
        );

        //TODO:使图表的y轴范围在2000-2500之间
        return chart;
    }
}
