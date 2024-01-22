package tw.xserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TicketGetter implements Runnable {

    private static final ConcurrentLinkedQueue<String> dataQueue = new ConcurrentLinkedQueue<>();
    private static CountDownLatch countDown;
    private static JTextArea area;
    private static String formattedURL;

    public TicketGetter(
            JTextArea area,
            String travel_ID,
            String date,
            int type_index,
            int person,
            int repeat
    ) throws InterruptedException {
        date = date
                .replace("/", "")
                .replace("-", "")
                .replace(".", "")
                .replace(" ", "");

        formattedURL = String.format("https://travel.wutai.gov.tw/Signup/CreateOrder/%s%s%02d/%d",
                travel_ID, date, type_index, person
        );

        countDown = new CountDownLatch(repeat);

        System.out.println("Example URL: " + formattedURL + "\n");

        TicketGetter.area = area;
    }

    @Override
    public void run() {
        ExecutorService areaUpdator = Executors.newSingleThreadExecutor();
        areaUpdator.submit(new AreaUpdator(area));

        ExecutorService updator = Executors.newSingleThreadExecutor();
        updator.submit(new Connector(formattedURL));

        try {
            countDown.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        updator.shutdownNow();
        areaUpdator.shutdownNow();
    }

    static class AreaUpdator implements Runnable {
        private final JTextArea area;

        AreaUpdator(JTextArea area) {
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
        private final String formattedURL;

        public Connector(String formattedURL) {
            this.formattedURL = formattedURL;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                try {
                    Connection.Response response = Jsoup.connect(formattedURL)
                            .execute();

                    JsonObject data = JsonParser.parseString(response.body()).getAsJsonObject();
                    switch (data.get("Status").getAsString()) {
                        case "OK": {
                            System.out.printf("https://travel.wutai.gov.tw/Signup/Users/%d/%s%n%n", data.get("oid").getAsInt(), data.get("verify").getAsString());
                            dataQueue.add(String.format("[%s] https://travel.wutai.gov.tw/Signup/Users/%d/%s%n",
                                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
                                    data.get("oid").getAsInt(),
                                    data.get("verify").getAsString()
                            ));
                            countDown.countDown();
                            break;
                        }

                        case "false": {
//                        System.err.println("Error");
                            break;
                        }

                        case "noQuota": {
                            countDown = new CountDownLatch(0);
//                        System.err.println("No More Tickets");
                            break;
                        }
                    }
                } catch (Exception e) {
                    countDown.countDown();
                }
            }
        }
    }
}