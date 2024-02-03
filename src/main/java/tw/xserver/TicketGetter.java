package tw.xserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import tw.xserver.Object.Config;
import tw.xserver.Object.Data;
import tw.xserver.utils.logger.Logger;

import javax.swing.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TicketGetter implements Runnable {
    private static ConcurrentLinkedQueue<Data> inputQueue;
    private static ConcurrentLinkedQueue<String> outputQueue;
    private static ConcurrentLinkedQueue<Connection> sendReq;
    private static ExecutorService connector;
    private static ExecutorService areaUpdater;
    private static CountDownLatch countDown;
    private static int counter = 1;
    private static int total = 0;
    private static JTextArea area;
    private static Config config;


    public TicketGetter(Config config, JTextArea area) {
        countDown = new CountDownLatch(1);
        inputQueue = new ConcurrentLinkedQueue<>();
        outputQueue = new ConcurrentLinkedQueue<>();
        sendReq = new ConcurrentLinkedQueue<>();
        areaUpdater = Executors.newSingleThreadExecutor();
        connector = Executors.newSingleThreadExecutor();
        TicketGetter.config = config;
        TicketGetter.area = area;

        for (Data i : config.data) {
            for (int j = 0; j < i.members.length; j += 20) {
                Data d = new Data();
                d.date = i.date;
                d.members = Arrays.copyOfRange(i.members, j, Math.min(20, i.members.length - j));
                inputQueue.add(d);
            }
        }
    }

    @Override
    public void run() {
        areaUpdater.submit(new AreaUpdater(area));
        connector.submit(new Connector());

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
        @Override
        public void run() {
            while (true) {
                if (LocalDateTime.now().isAfter(config.getDateTime())) {
                    Logger.LOGln("RUN");
                    break;
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            while (!inputQueue.isEmpty()) {
                Data rawData = inputQueue.peek();
                String formattedURL = String.format("https://travel.wutai.gov.tw/Signup/CreateOrder/HYCDEMO%s02/%d",
                        rawData.date.replace("/", ""), rawData.members.length
                );


                Logger.LOGln("----------------------------------------");
                Logger.LOGln(formattedURL);

                try {
                    Connection.Response rsp = Jsoup.connect(formattedURL).execute();
                    Map<String, String> cookies = rsp.cookies();
                    System.out.println(cookies);

                    JsonObject rspJson = JsonParser.parseString(rsp.body()).getAsJsonObject();

                    int rspIndex = processRsp(rspJson);
                    Logger.LOGln(rspJson.toString());

                    if (rspIndex == 0) {
                        int oid = rspJson.get("oid").getAsInt();
                        String verify = rspJson.get("verify").getAsString();
                        String url = String.format("https://travel.wutai.gov.tw/Signup/Users/%d/%s", oid, verify);
                        Logger.LOGln("OK 成功: " + rawData.date + ' ' + rawData.members.length);
                        Logger.LOGln(url);


                        sendReq.add(Jsoup.connect(url)
                                .method(Connection.Method.POST)
                                .referrer(url)
                                .cookies(cookies)
                                .data(rawData.parsePostData(config.guide))
                                .data("id", String.valueOf(rspJson.get("oid").getAsInt()))
                                .data("Verify", String.valueOf(rspJson.get("verify").getAsString())));

                        outputQueue.add(String.format("%02d: [%s] [%02d] %s",
                                counter++,
                                rawData.date.substring(5, 10),
                                rawData.members.length,
                                url
                        ));

                        inputQueue.poll();
                        ++total;
                    } else if (rspIndex == 2) {
                        Logger.LOGln("FAIL 失敗: " + rawData.date.substring(4, 8) + ' ' + rawData.members.length);
                        outputQueue.add(String.format(" FAIL 失敗: %s %d", rawData.date.substring(4, 8), rawData.members.length));
                        inputQueue.poll();
                    } else if (rspIndex == 1) {
                        inputQueue.add(inputQueue.poll());
                    }

                    Thread.sleep(config.send_delay);
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }

            Logger.LOGln(String.format("搶票完成：共搶了 %d 單", total));
            Logger.LOGln("正在填寫資料...");
            outputQueue.add(String.format("搶票完成：共搶了 %d 單", total));
            outputQueue.add("正在填寫資料...");

            while (!sendReq.isEmpty()) {
                try {
                    Connection.Response rsp = sendReq.poll().execute();
                    outputQueue.add(String.valueOf(rsp.url().toString()));
                    Thread.sleep(100);
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            outputQueue.add(String.format("搶票完成：共搶了 %d 單", total));
            outputQueue.add("資料填寫完成");
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
    }
}