package tw.xserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import tw.xserver.Exceptions.BadUserIdFormat;
import tw.xserver.Exceptions.SameIdOccur;
import tw.xserver.Object.Config;
import tw.xserver.Object.Data;
import tw.xserver.Object.Member;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

import static java.lang.System.exit;
import static tw.xserver.GUI.ROOT_PATH;

public class FileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileManager.class);

    public Config config;

    public FileManager() {
        File file = new File(ROOT_PATH + "./data/config.yml");

        if (!file.exists()) {
            file = exportResource();
            LOGGER.error("you have to edit config.yml first before using the program!");
            exit(401);
        }

        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            config = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader(), new LoaderOptions()))
                    .loadAs(inputStream, Config.class);
            new XlsxManager().load(config);

        } catch (IOException e) {
            LOGGER.error("error on loading config file: {}", e.getMessage());
            e.printStackTrace();
            exit(403);
        } catch (YAMLException e) {
            e.printStackTrace();
            LOGGER.error("error on loading config content: {}", e.getMessage());
            exit(402);
        }
    }

    private static final String CHECK_HEAD = "ABCDEFGHJKLMNPQRSTUVXYWZIO"; // 字母代號對照表

    public static void checkUserId(String inp) throws BadUserIdFormat {
        if (inp.length() != 10) {
            throw new BadUserIdFormat("wrong length");
        }

        if (Character.isLowerCase(inp.charAt(0))) {
            throw new BadUserIdFormat("wrong alphabet case");
        }

        if (inp.charAt(1) != '1' && inp.charAt(1) != '2') {
            throw new BadUserIdFormat("wrong sex id");
        }

        int firstNum = CHECK_HEAD.indexOf(inp.charAt(0)) + 10;
        int sum = firstNum / 10 + (firstNum % 10) * 9;

        for (int i = 1; i < 9; i++) {
            sum += (inp.charAt(i) - '0') * (9 - i);
        }

        if ((10 - (sum % 10)) % 10 != (inp.charAt(9) - '0')) {
            throw new BadUserIdFormat("wrong id");
        }
    }

    public void verify() throws Exception {
        if (config.custom_data != null) {
            LOGGER.info("verifying custom_data");
            for (Data i : config.custom_data) {
                if (i.custom_count == null) {
                    LOGGER.error("unknown count");
                    continue;
                }

                if (i.date == null) {
                    LOGGER.error("unknown date");
                    continue;
                }

                LOGGER.info("SKIP: " + i.date + ", member count: " + i.custom_count);
            }
        } else {
            LOGGER.info("there is no setting about custom_data, verify skipped");
        }

        for (Data i : config.data) {
            Set<String> ids = new HashSet<>();
            for (Member member : i.getMembers()) {
                // 1. 檢查身分證號合法性
                checkUserId(member.id);

                // 2. 檢查身分證號重複性
                if (ids.contains(member.id)) {
                    LOGGER.error("same id occur, date: {}, id: {}", i.date, member.id);
                    throw new SameIdOccur();
                } else {
                    ids.add(member.id);
                }
            }
        }
    }

    private File exportResource() {
        try (InputStream fileInJar = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            if (fileInJar == null) {
                LOGGER.error("can not find resource: config.yml");
                return null;
            }

            Files.copy(fileInJar, Paths.get(ROOT_PATH + "config.yml"), StandardCopyOption.REPLACE_EXISTING);
            return new File(ROOT_PATH + "config.yml");
        } catch (IOException e) {
            LOGGER.error("read resource failed: {}", e.getMessage());
        }
        return null;
    }
}
