package tw.xserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TicketGetter implements Runnable {
    public static final ExecutorService updater = Executors.newSingleThreadExecutor();
    private static final ConcurrentLinkedQueue<String> dataQueue = new ConcurrentLinkedQueue<>();
    private static final ExecutorService areaUpdater = Executors.newSingleThreadExecutor();
    private static CountDownLatch countDown;
    private static String formattedURL;
    private static int counter = 1;
    private static JTextArea area;
    private static String travel_ID;
    private static String date;
    private static int type_index;
    private static int person;
    private static int delay_ms;
    private static boolean take_it_all;


    public TicketGetter(
            JTextArea area,
            String travel_ID,
            String date,
            int type_index,
            int person,
            int repeat,
            int delay_ms,
            boolean take_it_all) throws InterruptedException {
        countDown = new CountDownLatch(repeat);

        date = date.replace("/", "")
                .replace("-", "")
                .replace(".", "")
                .replace(" ", "");
        formattedURL = String.format("https://travel.wutai.gov.tw/Signup/CreateOrder/%s%s%02d/%d",
                travel_ID, date, type_index, person
        );
        System.out.println("Example URL: " + formattedURL + "\n");


        TicketGetter.area = area;
        TicketGetter.travel_ID = travel_ID;
        TicketGetter.date = date;
        TicketGetter.type_index = type_index;
        TicketGetter.person = person;
        TicketGetter.delay_ms = delay_ms;
        TicketGetter.take_it_all = take_it_all;
    }

    @Override
    public void run() {
        areaUpdater.submit(new AreaUpdater(area));
        updater.submit(new Connector(formattedURL));

        try {
            countDown.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            updater.shutdownNow();
            areaUpdater.shutdownNow();
        }
    }

    static class AreaUpdater implements Runnable {
        private final JTextArea area;

        AreaUpdater(JTextArea area) {
            this.area = area;
        }

        @Override
        public void run() {
            while (true)
                if (!dataQueue.isEmpty()) {
                    String data = dataQueue.poll();
                    area.setText(area.getText() + "\n" + data);
                }
        }
    }

    static class Connector implements Runnable {
        private String formattedURL;

        public Connector(String formattedURL) {
            this.formattedURL = formattedURL;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(delay_ms);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                try {
                    int rsp = processRsp(JsonParser.parseString(Jsoup.connect(formattedURL).execute().body()).getAsJsonObject());
                    if (rsp == 2) {
                        if (person == 1) {
                            System.out.println("已結束，無餘票");
                            return;
                        }

                        if (!take_it_all) {
                            countDown = new CountDownLatch(0);
                            System.out.println("已結束，可能有餘票");
                            return;
                        }

                        formattedURL = String.format("https://travel.wutai.gov.tw/Signup/CreateOrder/%s%s%02d/%d",
                                travel_ID, date, type_index, 1
                        );
                        person = 1; // 開始處理餘票
                    }

                } catch (IOException e) {
                    countDown.countDown();
                }
            }
        }

        int processRsp(JsonObject data) {
            System.out.print(String.format("[%-12s]", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)).replace(' ', '0'));
            System.out.println(" " + data);

            switch (data.get("Status").getAsString()) {
                case "OK": {
                    System.out.printf("https://travel.wutai.gov.tw/Signup/Users/%d/%s%n%n", data.get("oid").getAsInt(), data.get("verify").getAsString());
                    dataQueue.add(String.format("%02d: [%-12s] [%02d 人]\n https://travel.wutai.gov.tw/Signup/Users/%d/%s%n",
                            counter++,
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
                            person,
                            data.get("oid").getAsInt(),
                            data.get("verify").getAsString()
                    ));
                    countDown.countDown();
                    return 0;
                }

                case "false": {
                    // ticket not available
                    return 1;
                }

                case "noQuota": {
                    // no more ticket
                    return 2;
                }

                default: {
                    return -1;
                }
            }
        }
    }
}