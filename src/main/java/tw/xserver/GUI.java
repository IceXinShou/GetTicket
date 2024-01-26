package tw.xserver;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GUI {
    public static void main(String[] args) {
        JFrame frame = new JFrame("搶票大師 (Discord: xs._.b)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        JPanel panel = new JPanel();
        frame.add(panel);
        placeComponents(panel);

        frame.setVisible(true);
    }

    private static ExecutorService executor;

    private static void placeComponents(JPanel panel) {
        panel.setLayout(null);

        urlLabel = new JLabel("網址");
        urlField = new JTextField(20);
        urlLabel.setBounds(10, 20, 50, 25);
        urlField.setBounds(50, 20, 420, 25);
        panel.add(urlLabel);
        panel.add(urlField);


        dateLabel = new JLabel("日期 (2025/07/01)");
        dateField = new JTextField("", 10);
        dateLabel.setBounds(60, 60, 120, 25);
        dateField.setBounds(50, 85, 120, 25);
        panel.add(dateLabel);
        panel.add(dateField);


        String[] numbers10 = new String[10];
        for (int i = 0; i < 10; i++) numbers10[i] = Integer.toString(i + 1);
        indexLabel = new JLabel("排序");
        index = new JComboBox<>(numbers10);
        indexLabel.setBounds(260, 60, 50, 25);
        index.setBounds(250, 85, 70, 25);
        panel.add(indexLabel);
        panel.add(index);
        index.setSelectedIndex(1);


        String[] numbers20 = new String[20];
        for (int i = 0; i < 20; i++) numbers20[i] = Integer.toString(i + 1);
        ticketCountLabel = new JLabel("張數");
        ticketsCount = new JComboBox<>(numbers20);
        ticketCountLabel.setBounds(335, 60, 50, 25);
        ticketsCount.setBounds(325, 85, 70, 25);
        panel.add(ticketCountLabel);
        panel.add(ticketsCount);
        ticketsCount.setSelectedIndex(19);


        String[] numbers200 = new String[200];
        for (int i = 0; i < 200; i++) numbers200[i] = Integer.toString(i + 1);
        repeatLabel = new JLabel("重複次數");
        repeatCount = new JComboBox<>(numbers200);
        repeatLabel.setBounds(410, 60, 80, 25);
        repeatCount.setBounds(400, 85, 80, 25);
        panel.add(repeatLabel);
        panel.add(repeatCount);


        infinityCheckBox = new JCheckBox("重複無限次");
        infinityCheckBox.setBounds(350, 115, 110, 25);
        panel.add(infinityCheckBox);
        infinityCheckBox.setSelected(true);


        takeItAllCheckBox = new JCheckBox("無票自動調整");
        takeItAllCheckBox.setBounds(350, 145, 110, 25);
        panel.add(takeItAllCheckBox);
        takeItAllCheckBox.setSelected(true);


        button = new JButton("開始");
        button.setBounds(30, 130, 170, 25);
        panel.add(button);


        numberFormatter = new NumberFormatter(NumberFormat.getIntegerInstance());
        numberFormatter.setValueClass(Long.class);
        numberFormatter.setAllowsInvalid(false);
        numberFormatter.setMinimum(0L);
        delayLabel = new JLabel("延遲(ms)");
        delayField = new JFormattedTextField(numberFormatter);
        delayField.setText("100");
        delayLabel.setBounds(220, 130, 80, 25);
        delayField.setBounds(280, 130, 60, 25);
        panel.add(delayLabel);
        panel.add(delayField);


        outputArea = new JTextArea();
        outputArea.setEditable(false);
        scrollPane = new JScrollPane(outputArea);
        scrollPane.setBounds(20, 170, 450, 180);
        panel.add(scrollPane);


        // 添加按鈕的事件監聽器
        button.addActionListener(e -> {
            if (executor != null) {
                executor.shutdownNow();
            }

            executor = Executors.newSingleThreadExecutor();

            if (outputArea.getText().isEmpty()) {
                outputArea.setText("開始搶票，結束前請勿再次按下按鈕！\n");
            } else {
                outputArea.setText(outputArea.getText() + "\n\n開始搶票，結束前請勿再次按下按鈕！\n");
            }

            int delay_ms = Integer.parseInt(delayField.getText());
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
            try {
                executor.submit(new TicketGetter(
                        outputArea,
                        travel_ID,
                        dateStr,
                        indexInt,
                        ticketCountInt,
                        repeatCountInt,
                        delay_ms,
                        takeItAllCheckBox.isSelected()
                ));
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    // ------- GUI Component -------
    private static JLabel urlLabel;
    private static JTextField urlField;
    private static JLabel dateLabel;
    private static JTextField dateField;
    private static JLabel indexLabel;
    private static JComboBox<String> index;
    private static JLabel ticketCountLabel;
    private static JComboBox<String> ticketsCount;
    private static JLabel repeatLabel;
    private static JComboBox<String> repeatCount;
    private static JCheckBox infinityCheckBox;
    private static JCheckBox takeItAllCheckBox;
    private static JButton button;
    private static NumberFormatter numberFormatter;
    private static JTextArea outputArea;
    private static JLabel delayLabel;
    private static JFormattedTextField delayField;
    private static JScrollPane scrollPane;
}
