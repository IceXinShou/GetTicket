package tw.xserver;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import tw.xserver.Object.Config;
import tw.xserver.Object.Data;
import tw.xserver.Object.Member;
import tw.xserver.utils.VerifyException;
import tw.xserver.utils.logger.Logger;

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
    private static final String CHECK_HEAD = "ABCDEFGHJKLMNPQRSTUVWXYZIO"; // 字母代號對照表
    public Config config;

    public FileManager() {
        File file = new File(ROOT_PATH + "./config.yml");

        if (!file.exists()) {
            file = exportResource();
            Logger.WARNln("You have to edit config.yml first before using the program!");
            exit(401);
        }


        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            config = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader(), new LoaderOptions()))
                    .loadAs(inputStream, Config.class);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.WARNln(e.getMessage());
            exit(403);
        } catch (YAMLException e) {
            e.printStackTrace();
            Logger.WARNln(e.getMessage());
            exit(402);
        }

        config.init();
    }

    private static void idCheck(String inp) throws VerifyException {
        if (inp.length() != 10) {
            throw new VerifyException("wrong length");
        }

        if (Character.isLowerCase(inp.charAt(0))) {
            throw new VerifyException("wrong alphabet case");
        }

        if (inp.charAt(1) != '1' && inp.charAt(1) != '2') {
            throw new VerifyException("wrong sex id");
        }

        int firstNum = CHECK_HEAD.indexOf(inp.charAt(0)) + 10;
        int sum = firstNum / 10 + (firstNum % 10) * 9;

        for (int i = 1; i < 10; i++) {
            int num = inp.charAt(i) - '0';
            if (i < 9) { // 前8位數字
                sum += num * (9 - i);
            } else { // 最後一位檢查
                if ((10 - sum % 10) % 10 != num) {
                    throw new VerifyException("wrong id");
                }
            }
        }
    }

    public void verify() throws VerifyException {
        for (Data i : config.data) {
            if (i.members == null) {
                if (i.member_count == null) {
                    Logger.WARNln("至少要填寫一個 member_count 或 members 參數！");
                    exit(404);
                }

                Logger.LOGln("SKIP: " + i.date + ", member count: " + i.member_count);
                continue;
            }

            Set<String> ids = new HashSet<>();
            for (Member member : i.getMembersAry()) {
                // 1. 檢查身分證號合法性
                idCheck(member.id);

                // 2. 檢查身分證號重複性
                if (ids.contains(member.id))
                    throw new VerifyException("same id occur");
                else
                    ids.add(member.id);
            }
        }
    }

    private File exportResource() {
        try (InputStream fileInJar = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            if (fileInJar == null) {
                Logger.WARNln("can not find resource: " + "config.yml");
                return null;
            }

            Files.copy(fileInJar, Paths.get(ROOT_PATH + "config.yml"), StandardCopyOption.REPLACE_EXISTING);
            return new File(ROOT_PATH + "config.yml");
        } catch (IOException e) {
            Logger.WARNln(e.getMessage());
            Logger.WARNln("read resource failed");
        }
        return null;
    }
}
