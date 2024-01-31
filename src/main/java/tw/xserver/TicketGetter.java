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
    public static final boolean DEBUG = true;
    private static final String rawData = "" +
            "03/01 20\n" +
            "03/01 20\n" +
            "03/02 20\n" +
            "03/02 20\n" +
            "03/02 20\n" +
            "03/02 20\n" +
            "03/03 20\n" +
            "03/03 20\n" +
            "03/03 20\n" +
            "03/03 20\n" +
            "03/05 20\n" +
            "03/05 20\n" +
            "03/07 20\n" +
            "03/07 16\n" +
            "03/08 20\n" +
            "03/08 20\n" +
            "03/09 20\n" +
            "03/09 20\n" +
            "03/09 20\n" +
            "03/09 20\n" +
            "03/09 20\n" +
            "03/09 20\n" +
            "03/09 2\n" +

            "03/10 20\n" +
            "03/10 20\n" +
            "03/15 20\n" +
            "03/15 20\n" +
            "03/16 20\n" +
            "03/16 20\n" +
            "03/16 20\n" +
            "03/16 20\n" +
            "03/17 6\n" +
            "03/17 20\n" +
            "03/17 20\n" +
            "03/17 20\n" +
            "03/22 20\n" +
            "03/22 20\n" +
            "03/23 20\n" +
            "03/23 20\n" +
            "03/23 20\n" +
            "03/23 20\n" +
            "03/24 20\n" +
            "03/24 20\n" +
            "03/24 20\n" +
            "03/24 20\n" +
            "03/24 20\n" +
            "03/24 20\n" +
            "03/24 2\n" +
            "03/25 20\n" +
            "03/25 20\n" +
            "03/29 20\n" +
            "03/29 20\n" +
            "03/30 20\n" +
            "03/30 20\n" +
            "03/31 20\n" +
            "03/31 20\n" ;

    private static ConcurrentLinkedQueue<Pair<String, Integer>> inputQueue;
    private static ExecutorService connector;
    private static ExecutorService areaUpdater;
    private static ConcurrentLinkedQueue<String> outputQueue;
    private static CountDownLatch countDown;
    private static String formattedURL;
    private static int counter = 1;
    private static int total = 0;
    private static JTextArea area;
    private static String travel_ID;
    private static String date;
    private static int type_index;
    private static int person;
    private static int delay_ms = 500;
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

        counter = 1;
        total = 0;
        countDown = new CountDownLatch(repeat);
        outputQueue = new ConcurrentLinkedQueue<>();
        areaUpdater = Executors.newSingleThreadExecutor();
        connector = Executors.newSingleThreadExecutor();
        inputQueue = new ConcurrentLinkedQueue<>();
        TicketGetter.area = area;

        if (DEBUG) {
            for (String i : rawData.split("\n")) {
                if (i.isEmpty()) continue;

                String rawDate = i.split(" ")[0];
                int rawCount = Integer.parseInt(i.split(" ")[1]);
                inputQueue.add(new Pair<>(
                        "2024" + rawDate.replace("/", "") + "02",
                        rawCount
                ));
            }
        } else {
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
    }

    @Override
    public void run() {
        areaUpdater.submit(new AreaUpdater(area));
        connector.submit(new Connector(formattedURL));

        try {
            countDown.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            connector.shutdownNow();
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
                if (!outputQueue.isEmpty()) {
                    String data = outputQueue.poll();
                    if (area.getText().isEmpty()) {
                        area.setText(data);
                    } else {
                        area.setText(area.getText() + "\n" + data);
                    }
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
            if (DEBUG) {
                while (!inputQueue.isEmpty()) {
                    Pair<String, Integer> rawData = inputQueue.peek();

                    formattedURL = String.format("https://travel.wutai.gov.tw/Signup/CreateOrder/HYCDEMO%s/%d",
                            rawData.getFirst(), rawData.getSecond()
                    );

                    System.out.println(getTime() + ' ' + formattedURL);

                    try {
                        JsonObject data = JsonParser.parseString(Jsoup.connect(formattedURL).execute().body()).getAsJsonObject();
                        int rsp = processRsp(data);

                        System.out.println(getTime() + " " + data);
                        if (rsp == 0) {
                            System.out.printf(getTime() + " OK 成功: %s %d\n", rawData.getFirst(), rawData.getSecond());
                            System.out.printf(getTime() + " https://travel.wutai.gov.tw/Signup/Users/%d/%s%n%n\n\n", data.get("oid").getAsInt(), data.get("verify").getAsString());
                            outputQueue.add(String.format("%02d: %s [%s] [%02d 人]\n https://travel.wutai.gov.tw/Signup/Users/%d/%s%n",
                                    counter++,
                                    getTime(),
                                    rawData.getFirst(),
                                    rawData.getSecond(),
                                    data.get("oid").getAsInt(),
                                    data.get("verify").getAsString()
                            ));

                            inputQueue.poll();
                            ++total;
                        } else if (rsp == 2) {
                            System.out.printf(getTime() + " FAIL 失敗: %s %d\n\n", rawData.getFirst(), rawData.getSecond());
                            outputQueue.add(getTime() + String.format(" FAIL 失敗: %s %d", rawData.getFirst(), rawData.getSecond()));
                            inputQueue.poll();
                        } else if (rsp == 1) {
                            inputQueue.add(inputQueue.poll());
                        }

                        Thread.sleep(delay_ms);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                System.out.println(getTime() + " 已結束，搶票完成：共搶了 " + total + " 單");
                outputQueue.add(String.format(getTime() + " 搶票完成：共搶了 %d 單", total));

            } else {
                while (true) {
                    try {
                        JsonObject data = JsonParser.parseString(Jsoup.connect(formattedURL).execute().body()).getAsJsonObject();
                        int rsp = processRsp(data);

                        System.out.println(getTime() + " " + data);
                        if (rsp == 0) {
                            System.out.printf("https://travel.wutai.gov.tw/Signup/Users/%d/%s%n%n", data.get("oid").getAsInt(), data.get("verify").getAsString());
                            outputQueue.add(String.format("%02d: %s [%s] [%02d 人]\n https://travel.wutai.gov.tw/Signup/Users/%d/%s%n",
                                    counter++,
                                    getTime(),
                                    date,
                                    person,
                                    data.get("oid").getAsInt(),
                                    data.get("verify").getAsString()
                            ));
                            total += person;
                            countDown.countDown();

                        } else if (rsp == 2) {
                            if (person == 1) {
                                System.out.println("已結束，無餘票");
                                outputQueue.add(String.format("搶票完成：共搶了 %d 張", total));
                                countDown = new CountDownLatch(0);
                                return;
                            }

                            if (!take_it_all) {
                                System.out.println("已結束，可能有餘票");
                                outputQueue.add(String.format("搶票完成：共搶了 %d 張", total));
                                countDown = new CountDownLatch(0);
                                return;
                            }

                            formattedURL = String.format("https://travel.wutai.gov.tw/Signup/CreateOrder/%s%s%02d/%d",
                                    travel_ID, date, type_index, 1
                            );
                            person = 1; // 開始處理餘票
                        }

                        Thread.sleep(delay_ms);
                    } catch (IOException e) {
                        countDown.countDown();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        int processRsp(JsonObject data) {
            switch (data.get("Status").getAsString()) {
                case "OK": {
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

        String getTime() {
            return String.format("[%-12s]", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)).replace(' ', '0');
        }
    }
}