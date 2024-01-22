package tw.xserver;

import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GUI {
    public static void main(String[] args) {
        // 創建窗口框架
        JFrame frame = new JFrame("搶票大師 (Discord: xs._.b)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        // 創建面板
        JPanel panel = new JPanel();
        frame.add(panel);
        placeComponents(panel);

        // 設置窗口可見
        frame.setVisible(true);
    }

    private static void placeComponents(JPanel panel) {
        panel.setLayout(null);

        // 創建網址輸入框
        JLabel urlLabel = new JLabel("網址");
        JTextField urlField = new JTextField(20);
        urlLabel.setBounds(10, 20, 50, 25);
        urlField.setBounds(50, 20, 420, 25);
        panel.add(urlLabel);
        panel.add(urlField);


        // 創建日期輸入框
        JLabel dateLabel = new JLabel("日期 (2025/07/01)");
        JTextField dateField = new JTextField("", 10);
        dateLabel.setBounds(60, 60, 120, 25);
        dateField.setBounds(50, 85, 120, 25);
        panel.add(dateLabel);
        panel.add(dateField);


        String[] numbers10 = new String[10];
        for (int i = 0; i < 10; i++) numbers10[i] = Integer.toString(i + 1);

        JLabel indexLabel = new JLabel("排序");
        JComboBox<String> index = new JComboBox<>(numbers10);
        indexLabel.setBounds(260, 60, 50, 25);
        index.setBounds(250, 85, 70, 25);
        panel.add(indexLabel);
        panel.add(index);


        String[] numbers20 = new String[20];
        for (int i = 0; i < 20; i++) numbers20[i] = Integer.toString(i + 1);

        JLabel ticketCountLabel = new JLabel("張數");
        JComboBox<String> ticketsCount = new JComboBox<>(numbers20);
        ticketCountLabel.setBounds(335, 60, 50, 25);
        ticketsCount.setBounds(325, 85, 70, 25);
        panel.add(ticketCountLabel);
        panel.add(ticketsCount);


        // 創建下拉式選單
        String[] numbers200 = new String[200];
        for (int i = 0; i < 200; i++) numbers200[i] = Integer.toString(i + 1);

        JLabel repeatLabel = new JLabel("重複次數");
        JComboBox<String> repeatCount = new JComboBox<>(numbers200);
        repeatLabel.setBounds(410, 60, 80, 25);
        repeatCount.setBounds(400, 85, 80, 25);
        panel.add(repeatLabel);
        panel.add(repeatCount);


        // 創建可選打勾框
        JCheckBox infinityCheckBox = new JCheckBox("重複無限次");
        infinityCheckBox.setBounds(300, 130, 110, 25);
        panel.add(infinityCheckBox);


        // 創建按鈕
        JButton button = new JButton("開始");
        button.setBounds(100, 130, 185, 25);
        panel.add(button);


        JTextArea outputData = new JTextArea();
        outputData.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(outputData);
        scrollPane.setBounds(20, 170, 450, 180);
        panel.add(scrollPane);


        // 添加按鈕的事件監聽器
        button.addActionListener(e -> {
            button.setEnabled(false);
            int indexInt = Integer.parseInt((String) index.getSelectedItem());
            int ticketCountInt = Integer.parseInt((String) ticketsCount.getSelectedItem());
            int repeatCountInt = infinityCheckBox.isSelected() ?
                    Integer.MAX_VALUE : Integer.parseInt((String) repeatCount.getSelectedItem());
            String dateStr = dateField.getText();
            String travel_ID;
            String urlText = urlField.getText();

            if (urlText.endsWith("/")) urlText = urlText.substring(0, urlText.length() - 1);
            if (urlText.startsWith("https://")) urlText = urlText.substring(8);
            if (urlText.startsWith("http://")) urlText = urlText.substring(7);
            String[] default_split = urlText.split("/");

            if (default_split.length == 5 && dateStr.isEmpty()) dateStr = default_split[4];
            travel_ID = default_split[3];

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                executor.submit(new TicketGetter(
                        outputData,
                        travel_ID,
                        dateStr,
                        indexInt,
                        ticketCountInt,
                        repeatCountInt
                ));
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
