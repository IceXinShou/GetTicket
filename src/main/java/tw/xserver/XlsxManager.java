package tw.xserver;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import tw.xserver.Exceptions.BadSheetNameFormat;
import tw.xserver.Exceptions.BadUserBirthFormat;
import tw.xserver.Exceptions.BadUserIdFormat;
import tw.xserver.Object.Config;
import tw.xserver.Object.Member;
import tw.xserver.Object.Roll;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static tw.xserver.FileManager.checkUserId;
import static tw.xserver.GUI.ROOT_PATH;

public class XlsxManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(XlsxManager.class);
    private static final File FOLDER = new File(ROOT_PATH + "./data/xlsx/");
    private static final Marker FATAL_MARKER = MarkerFactory.getMarker("FATAL");
    public static final boolean CHECK_AUTO_FIX = true;
    private static final ExampleId exampleId = new ExampleId();

    public XlsxManager() {
        if (FOLDER.mkdirs()) {
            LOGGER.debug("folder created: {}", FOLDER.getAbsolutePath());
        }
    }

    public void load(Config config) throws IOException {
        File[] files;
        if ((files = FOLDER.listFiles((d, s) ->
                (s.toLowerCase().endsWith("xlsx") ||
                        s.toLowerCase().endsWith("xls")) &&
                        !s.startsWith("~"))) == null) {
            LOGGER.info("no xlsx file exist");
            return;
        }

        Map<String, List<Member>> fileData = new HashMap<>();
        for (File file : files) {
            LOGGER.info("loading file: {}", file.getAbsolutePath());

            try (FileInputStream fileInputStream = new FileInputStream(file);
                 ReadableWorkbook wb = new ReadableWorkbook(fileInputStream)) {

                wb.getSheets().forEach(sheet -> {
                    try {
                        checkSheetName(sheet.getName());
                    } catch (BadSheetNameFormat e) {
                        LOGGER.error("bad sheet name format, sheet: {}", sheet.getName());
                        return;
                    }

                    Set<String> ids = new HashSet<>();
                    try (Stream<Row> rows = sheet.openStream()) {
                        rows.forEach(user -> {
                            Optional<String> userName = user.getCellAsString(0);
                            Optional<String> userId = user.getCellAsString(1);
                            Optional<String> userBirth = user.getCellAsString(2);

                            if (userName.isEmpty() || userId.isEmpty() || userBirth.isEmpty()) {
                                LOGGER.error("wrong user data, sheet: {}, line: {}", sheet.getName(), user.getRowNum());
                                return;
                            }

                            try {
                                checkUserId(userId.get());
                            } catch (BadUserIdFormat e) {
                                if (!CHECK_AUTO_FIX) {
                                    LOGGER.error("bad userId format, sheet: {}, line: {}", sheet.getName(), user.getRowNum());
                                    return;
                                }

                                LOGGER.warn("bad userId format, sheet: {}, line: {}", sheet.getName(), user.getRowNum());
                                LOGGER.info("try to auto fix...");
                                String newId;
                                switch (userId.get().charAt(1)) {
                                    case '1' -> newId = exampleId.getManId();
                                    case '2' -> newId = exampleId.getWomanId();
                                    default -> {
                                        LOGGER.error(FATAL_MARKER, "unknown sex, please fix it manually !!!");
                                        return;
                                    }
                                }
                                LOGGER.info("changed: {} -> {}", userId.get(), newId);
                                userId = Optional.of(newId);

                            }

                            try {
                                checkUserBirth(userBirth.get());
                            } catch (BadUserBirthFormat e) {
                                if (!CHECK_AUTO_FIX) {
                                    LOGGER.error("bad userBirth format, sheet: {}, line: {}", sheet.getName(), user.getRowNum());
                                    return;
                                }

                                LOGGER.warn("bad userBirth format, sheet: {}, line: {}", sheet.getName(), user.getRowNum());
                                LOGGER.info("try to auto fix...");
                                String newBirth;
                                newBirth = String.format("%2d.%02d.%02d",
                                        new Random().nextInt(16) + 50, // 50 ~ 65
                                        new Random().nextInt(12) + 1,      // 1 ~ 12
                                        new Random().nextInt(28) + 1       // 1 ~ 28
                                );
                                LOGGER.info("changed: {} -> {}", userBirth.get(), newBirth);
                                userBirth = Optional.of(newBirth);
                            }

                            if (ids.contains(userId.get())) {
                                String newId;
                                LOGGER.error("same id occur, try to auto fix...");
                                switch (userId.get().charAt(1)) {
                                    case '1' -> newId = exampleId.getManId();
                                    case '2' -> newId = exampleId.getWomanId();
                                    default -> {
                                        LOGGER.error(FATAL_MARKER, "unknown sex, please fix it manually !!!");
                                        return;
                                    }
                                }
                                userId = Optional.of(newId);
                                LOGGER.info("changed: {} -> {}", userId.get(), newId);
                            }

                            ids.add(userId.get());
                            Member member = new Member(userName.get(), userId.get(), userBirth.get());
                            fileData.computeIfAbsent(sheet.getName(), k -> new ArrayList<>()).add(member);
                        });
                    } catch (IOException e) {
                        LOGGER.error("cannot read file: {}", file.getAbsolutePath());
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        for (Map.Entry<String, List<Member>> i : fileData.entrySet()) {
            String date = String.format("2024/%s/%s", i.getKey().substring(0, 2), i.getKey().substring(2, 4));

            List<Member> members = i.getValue();
            config.roll.add(new Roll().init(config.guide, date, members));
            LOGGER.info("loaded member data: {}", date);
        }

        config.roll.sort(Comparator.comparing(Roll::getDate));
    }

    private static final Pattern DATE_PATTERN = Pattern.compile("^(" +
            "01(0[1-9]|[12][0-9]|3[01])|" +
            "02(0[1-9]|1[0-9]|2[0-9])|" +
            "03(0[1-9]|[12][0-9]|3[01])|" +
            "04(0[1-9]|[12][0-9]|30)|" +
            "05(0[1-9]|[12][0-9]|3[01])|" +
            "06(0[1-9]|[12][0-9]|30)|" +
            "07(0[1-9]|[12][0-9]|3[01])|" +
            "08(0[1-9]|[12][0-9]|3[01])|" +
            "09(0[1-9]|[12][0-9]|30)|" +
            "10(0[1-9]|[12][0-9]|3[01])|" +
            "11(0[1-9]|[12][0-9]|30)|" +
            "12(0[1-9]|[12][0-9]|3[01]))$");

    private static void checkSheetName(String name) throws BadSheetNameFormat {
        if (!DATE_PATTERN.matcher(name).matches()) {
            throw new BadSheetNameFormat();
        }
    }

    public static final Pattern BIRTH_PATTERN = Pattern.compile(
            "^\\d{2,4}[./-](0[1-9]|1[0-2])[./-](0[1-9]|[12][0-9]|3[01])$");

    private static void checkUserBirth(String name) throws BadUserBirthFormat {
        if (!BIRTH_PATTERN.matcher(name).matches()) {
            throw new BadUserBirthFormat();
        }
    }

    private static class ExampleId {
        private static final String[] ID_MAN_EXAMPLE_LIST = {
                "M110770789",
                "Y110318760",
                "S102824531",
                "B100567306",
                "B115467117",
                "T113104649",
                "J102058049",
                "H107677141",
                "V113632686",
                "F104061452",
                "T112432484",
                "R112561255",
                "X100641242",
                "O114512348",
                "J111886533",
                "T108136326",
                "P118830668",
                "G104110611",
                "D101018617",
                "I103727234",
                "I103468605",
                "I105224576",
                "P113200866",
                "P106145832",
                "K103420737",
                "M104783350",
                "N101544763",
                "K104276822",
                "Q106830033",
                "E100362840",
                "K107455487",
                "O118637613",
                "E105514451",
                "P114156849",
                "P118336736",
                "N103285463",
                "W114610211",
                "S100424275",
                "M103654841",
                "N117135467",
                "N111214807",
                "I116367206",
                "L102778225",
                "S102282079",
                "Q115241384",
                "K110536455",
                "Y108620531",
                "J101885426",
                "F118032674",
                "H115834552",
        };

        private static final String[] ID_WOMAN_EXAMPLE_LIST = {
                "N210310759",
                "C206727765",
                "Y212001555",
                "C206028089",
                "G206338842",
                "E218082219",
                "X218140185",
                "W210626064",
                "A211741837",
                "V212483172",
                "A214336454",
                "Q206136701",
                "G217464531",
                "S206264846",
                "R201003708",
                "M212251716",
                "M213724076",
                "M205583614",
                "Y208502101",
                "R204137630",
                "S204628039",
                "H206165420",
                "K210516357",
                "X200110351",
                "B203464760",
                "L200844737",
                "A216785571",
                "N211554040",
                "N211283162",
                "O215054623",
                "D216607486",
                "R207308462",
                "S203325422",
                "F205513411",
                "B205772747",
                "E202526880",
                "L202622837",
                "S218733347",
                "E207545367",
                "Y214445359",
                "W206083757",
                "D201265074",
                "B215871524",
                "H213360239",
                "J217640140",
                "K218046601",
                "J211582827",
                "D212204514",
                "S218775167",
                "X210173637",
        };

        private final Queue<String> manQueue = new ArrayDeque<>();
        private final Queue<String> womanQueue = new ArrayDeque<>();

        private ExampleId() {
            manQueue.addAll(Arrays.asList(ID_MAN_EXAMPLE_LIST));
            womanQueue.addAll(Arrays.asList(ID_WOMAN_EXAMPLE_LIST));
        }

        public String getManId() {
            return manQueue.poll();
        }

        public String getWomanId() {
            return womanQueue.poll();
        }
    }
}
