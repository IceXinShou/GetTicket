package tw.xserver;

import tw.xserver.utils.VerifyException;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.lang.System.exit;


public class GUI {
    public static final String ROOT_PATH = new File(System.getProperty("user.dir")).toString() + '/';
    private static FileManager manager;

    public static void main(String[] args) {
        frame = new JFrame("搶票大師 (Discord: xs._.b)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        panel = new JPanel();
        frame.add(panel);
        placeComponents(panel);
        addListener();

        frame.setVisible(true);
    }

    private static ExecutorService executor;

    private static void placeComponents(JPanel panel) {
        panel.setLayout(null);

        read_btn = new JButton("讀取設定");
        read_btn.setBounds(15, 15, 100, 25);
        panel.add(read_btn);

        preview_btn = new JButton("預覽設定");
        preview_btn.setBounds(15, 45, 100, 25);
        panel.add(preview_btn);

        start_btn = new JButton("開始執行");
        start_btn.setBounds(15, 75, 100, 25);
        panel.add(start_btn);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setBounds(130, 15, 350, 350);
        panel.add(outputScrollPane);
    }

    public static void addListener() {
        read_btn.addActionListener(event -> {
            managerInit();
        });

        preview_btn.addActionListener(event -> {
            if (manager == null) {
                managerInit();
            }

            JTextArea textArea = new JTextArea(manager.config.toString());
            textArea.setEditable(false);

            JScrollPane scrollPane = new JScrollPane(textArea);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            scrollPane.setPreferredSize(new Dimension(500, 500));
            JOptionPane.showMessageDialog(frame, scrollPane);
        });

        start_btn.addActionListener(event -> {
            if (manager == null) {
                managerInit();
            }

            if (executor != null) {
                executor.shutdownNow();
            }

            if (outputArea.getText().isEmpty()) {
                outputArea.setText("開始搶票，結束前請勿再次按下按鈕！\n");
            } else {
                outputArea.setText(outputArea.getText() + "\n\n開始搶票，結束前請勿再次按下按鈕！\n");
            }

            executor = Executors.newSingleThreadExecutor();
            executor.submit(new TicketGetter(manager.config, outputArea));
        });
    }

    private static void managerInit() {
        manager = new FileManager();
        try {
            manager.verify();
        } catch (VerifyException e) {
            JOptionPane.showMessageDialog(null,
                    e.getClass().getName() + ": " + e.getMessage() + '\n' +
                            "\tat " + Arrays.stream(e.getStackTrace())
                            .filter(i -> !i.getClassName().startsWith("tw.xserver"))
                            .map(StackTraceElement::toString)
                            .collect(Collectors.joining("\n\tat "))
            );
            exit(400);
        }
    }

    // ------- GUI Component -------
    private static JTextArea outputArea;
    private static JScrollPane outputScrollPane;
    private static JFrame frame;
    private static JPanel panel;
    private static JButton read_btn;
    private static JButton preview_btn;
    private static JButton start_btn;
}
