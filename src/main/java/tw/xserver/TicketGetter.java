package tw.xserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import tw.xserver.Object.Config;
import tw.xserver.Object.Roll;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

import static tw.xserver.GUI.*;

public class TicketGetter implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketGetter.class);
    private static final Random random = new Random();
    private static ConcurrentLinkedDeque<Roll> inputQueue;
    private static ConcurrentLinkedQueue<String> outputQueue;
    private static ConcurrentLinkedQueue<Roll> sendReq;
    private static ExecutorService connector;
    private static ExecutorService areaUpdater;
    private static ScheduledExecutorService awaitSender;
    private static CountDownLatch countDown;
    private static CopyOnWriteArraySet<String> noQuotaDates;
    private static int get_total;
    private static int post_await;
    private static int post_success;
    private static int post_fail;
    private static Config config;
    public static ConcurrentLinkedQueue<String> sentFailedQueue;


    public TicketGetter(Config config) {
        TicketGetter.config = config;
        countDown = new CountDownLatch(1);
        inputQueue = new ConcurrentLinkedDeque<>();
        outputQueue = new ConcurrentLinkedQueue<>();
        sentFailedQueue = new ConcurrentLinkedQueue<>();
        noQuotaDates = new CopyOnWriteArraySet<>();
        sendReq = new ConcurrentLinkedQueue<>();
        connector = Executors.newSingleThreadExecutor();
        awaitSender = Executors.newScheduledThreadPool(100);
        get_total = 0;
        post_await = 0;
        post_success = 0;
        post_fail = 0;

        if (config.gui)
            areaUpdater = Executors.newSingleThreadExecutor();

        if (config.hasCustom()) {
            for (Roll i : config.custom_data) {
                int member_count = i.custom_count;
                LOGGER.debug("date: {}, member: {}", i.date, member_count);

                for (int j = 0; j < member_count; j += 20) {
                    int newSize = Math.min(20, member_count - j);
                    Roll newD = new Roll().init(i.date, newSize);
                    LOGGER.debug("size: {}", newSize);
                    inputQueue.add(newD);
                }
            }
        }

        for (Roll i : config.roll) {
            int member_count = i.getMembers().size();
            LOGGER.debug("date: {}, member: {}", i.date, member_count);

            for (int j = 0; j < member_count; j += 20) {
                int newSizeEnd = j + Math.min(20, member_count - j);
                Roll newD = new Roll().init(config.guide, i.date, i.getMembers().subList(j, newSizeEnd));
                LOGGER.debug("range ({}, {}), size: {}, ctx: {}",
                        j, newSizeEnd,
                        newD.getMembers().size(), Arrays.toString(newD.getMembers().toArray()));
                inputQueue.add(newD);
            }
        }
    }

    @Override
    public void run() {
        areaUpdater.submit(new AreaUpdater());
        connector.submit(new Connector());

        try {
            countDown.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (!connector.isShutdown())
                connector.shutdown();
            if (!areaUpdater.isShutdown())
                areaUpdater.shutdown();
        }
    }

    private static void awaitSend(Roll roll, int delayInSeconds) {
        awaitSender.schedule(() -> {
            try {
                Connection.Response rsp = roll.getConn().execute();

                LOGGER.info("----------------------------------------");
                outputQueue.add("----------------------------------------");

                if (rsp.url().toString().contains("OrderComplete")) {
                    outputQueue.add(rsp.url().toString());
                    LOGGER.info("{}", rsp.url().toString());
                    ++post_success;
                } else {
                    outputQueue.add("失敗: " + rsp.url().toString());
                    LOGGER.warn("失敗: {}", rsp.url().toString());
                    LOGGER.warn(prettyPrintHTML(rsp.body()).replace("\n", ""));

                    ++post_fail;
                    sentFailedQueue.add(prettyPrintHTML(rsp.body()).replace("\n", ""));
                    sentFail_btn.setEnabled(true);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }, delayInSeconds, TimeUnit.SECONDS);
    }

    static class AreaUpdater implements Runnable {
        @Override
        public void run() {
            JTextArea area = GUI.outputArea;
            while (countDown.getCount() > 0)
                if (!outputQueue.isEmpty()) {
                    String data = outputQueue.poll();
                    if (area.getText().isEmpty()) {
                        area.setText(data);
                    } else {
                        area.setText(area.getText() + '\n' + data);
                    }
                }
            LOGGER.info("AreaUpdater shutdown");
        }
    }

    /**
     * @param date: ex-format "2024-04-01"
     * @return is ticket available
     */
    private static boolean checkTicketAvailable(String date) throws IOException {
        Connection.Response rsp = Jsoup
                .connect("https://travel.wutai.gov.tw/Travel/QuotaByDate/HYCDEMO/" + date)
                .referrer("https://travel.wutai.gov.tw/")
                .execute();
        return !JsonParser.parseString(rsp.body()).getAsJsonArray().isEmpty();
    }

    /**
     * @param date: ex-format "2024-04-01"
     * @return 0 if ticket is not gettable
     */
    private static int getRemainTicketCount(String date) throws IOException {
        Connection.Response rsp = Jsoup
                .connect("https://travel.wutai.gov.tw/Travel/QuotaByDate/HYCDEMO/" + date)
                .referrer("https://travel.wutai.gov.tw/")
                .execute();
        JsonArray ary = JsonParser.parseString(rsp.body()).getAsJsonArray();
        return (ary.isEmpty() ?
                0 : Math.max(0, 200 - ary.get(1).getAsJsonObject().get("used").getAsInt()));
    }


    static class Connector implements Runnable {
        @Override
        public void run() {
            if (inputQueue.isEmpty()) {
                LOGGER.error("input queue empty");
                LOGGER.info("Connector shutdown");

                outputQueue.add("完成");
                forceStop_btn.setEnabled(false);
                countDown.countDown();
                return;
            }

            while (countDown.getCount() > 0) {
                if (LocalDateTime.now().isAfter(config.getDateTime())) {
                    LOGGER.info("已過預計時間，開始驗證票");
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                while (!config.burst && countDown.getCount() > 0) {
                    assert inputQueue.peek() != null;

                    if (checkTicketAvailable(inputQueue.peek().date.replace('/', '-'))) {
                        LOGGER.info("取得票資訊，開始搶票");
                        break;
                    } else {
                        LOGGER.debug("check empty");
                    }
                    Thread.sleep(300);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            while (!forceStop.get() && !inputQueue.isEmpty()) {
                Roll roll = inputQueue.peek();
                if (noQuotaDates.contains(roll.date)) {
                    LOGGER.warn("no quote, skip requesting: {}", roll.date);
                    inputQueue.poll();
                    continue;
                }

                String formattedURL = String.format("https://travel.wutai.gov.tw/Signup/CreateOrder/HYCDEMO%s02/%d",
                        roll.date.replace("/", ""), roll.getSize()
                );

                LOGGER.info("----------------------------------------");
                LOGGER.info("REQ: {}", formattedURL);

                try {
                    Connection.Response rsp = Jsoup
                            .connect(formattedURL)
                            .referrer("https://travel.wutai.gov.tw/Travel/Detail/HYCDEMO/" + roll.date.replace("/", ""))
                            .execute();
                    Map<String, String> cookies = rsp.cookies();
                    JsonObject rspJson = JsonParser.parseString(rsp.body()).getAsJsonObject();

                    LOGGER.info(rspJson.toString());
                    switch (processRsp(rspJson)) {
                        case OK: {
                            int oid = rspJson.get("oid").getAsInt();
                            String verify = rspJson.get("verify").getAsString();
                            String url = String.format("https://travel.wutai.gov.tw/Signup/Users/%d/%s", oid, verify);
                            LOGGER.info("OK 成功: {} [{}]",
                                    roll.date.substring(5, 10), roll.getSize()
                            );
                            LOGGER.info("表單連結: {}", url);

                            outputQueue.add(String.format("%02d: (%s) [%02d] %s",
                                    ++get_total, roll.date.substring(5, 10), roll.getSize(), url
                            ));
                            inputQueue.poll();

                            if (roll.retry) noQuotaDates.add(roll.date);
                            if (roll.custom_count != null && roll.getMembers().isEmpty()) break;

                            if (roll.parsePostData(config.guide) != null) {
                                ++post_await;
                                roll.setConn(Jsoup.connect(url)
                                        .method(Connection.Method.POST)
                                        .referrer(url)
                                        .cookies(cookies)
                                        .data(roll.parsePostData(config.guide))
                                        .data("id", String.valueOf(rspJson.get("oid").getAsInt()))
                                        .data("Verify", rspJson.get("verify").getAsString()));
                                sendReq.add(roll);
                            }

                            break;
                        }

                        case FAILURE:
                            break;

                        case FALSE: {
                            inputQueue.add(inputQueue.poll());
                            break;
                        }

                        case NO_QUOTA: {
                            LOGGER.warn("沒票了!! : {} [{}]", roll.date.substring(5, 10), roll.getSize());
                            outputQueue.add(String.format("-> -> -> NO QUOTA 沒票了: %s [%d]", roll.date.substring(5, 10), roll.getSize()));

                            LOGGER.warn("嘗試取得餘票數...");
                            outputQueue.add("嘗試取得餘票數...");

                            int remain = getRemainTicketCount(roll.date);
                            if (remain > 0) {
                                LOGGER.warn("可用 {} 張", remain);
                                outputQueue.add("可用 " + remain + " 張");

                                roll.retry = true;
                                roll.custom_count = remain;
                                inputQueue.poll();
                                inputQueue.addFirst(roll);
                            } else {
                                noQuotaDates.add(roll.date);
                                LOGGER.warn("無");
                                outputQueue.add("無");
                            }

                            break;
                        }

                        case UNKNOWN: {
                            LOGGER.error("unknown response");
                            break;
                        }
                    }

                    Thread.sleep(config.send_delay);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            LOGGER.info("----------------------------------------");
            LOGGER.info("搶票完成，共搶了 {} 單", get_total);
            LOGGER.info("正在填寫資料...");


            outputQueue.add("----------------------------------------");
            outputQueue.add(String.format("搶票完成，共搶了 %d 單", get_total));
            outputQueue.add("正在填寫資料...");

            while (!sendReq.isEmpty()) {
                int awaitTime = random.nextInt(420) + 720;
                LOGGER.info("自動排程等待時間: {} 秒", awaitTime);
                awaitSend(sendReq.poll(), awaitTime); // 12 ~ 18 min
            }

            while ((post_success + post_fail) != post_await) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            LOGGER.info("----------------------------------------");
            LOGGER.info("資料填寫完成");
            LOGGER.info("提交 {} 單", post_success);
            LOGGER.info("失敗 {} 單", post_fail);

            outputQueue.add("----------------------------------------");
            outputQueue.add("資料填寫完成");
            outputQueue.add(String.format("提交 %d 單", post_success));
            outputQueue.add(String.format("失敗 %d 單", post_fail));

            LOGGER.info("Connector shutdown");
            outputQueue.add("完成");
            forceStop_btn.setEnabled(false);
            countDown.countDown();
        }

        ResponseType processRsp(JsonObject data) {
            return switch (data.get("Status").getAsString()) {
                case "OK" -> ResponseType.OK;
                case "false" -> ResponseType.FALSE;
                case "noQuota" -> ResponseType.NO_QUOTA;
                case "failure" -> ResponseType.FAILURE;
                default -> ResponseType.UNKNOWN;
            };
        }
    }

    private static String prettyPrintHTML(String rawHTML) {
        Tidy tidy = new Tidy();
        tidy.setInputEncoding("UTF-8");
        tidy.setOutputEncoding("UTF-8");
        tidy.setXHTML(true);
        tidy.setIndentContent(true);
        tidy.setPrintBodyOnly(true);
        tidy.setTidyMark(false);
        tidy.setShowErrors(0);
        tidy.setShowWarnings(false);
        tidy.setQuiet(true);

        Document htmlDOM = tidy.parseDOM(new ByteArrayInputStream(rawHTML.getBytes()), null);
        OutputStream out = new ByteArrayOutputStream();
        tidy.pprint(htmlDOM, out);

        return out.toString();
    }

    private enum ResponseType {
        OK, // success
        NO_QUOTA, // no more ticket
        FAILURE, // official limit
        FALSE, // unknown error
        UNKNOWN,
    }
}