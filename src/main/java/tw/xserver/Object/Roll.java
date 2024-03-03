package tw.xserver.Object;

import org.jetbrains.annotations.Nullable;
import org.jsoup.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Roll {
    private static final Logger LOGGER = LoggerFactory.getLogger(Roll.class);
    public String date;
    public Integer custom_count;
    private List<Member> members = new ArrayList<>();
    private Guide guide;
    private Connection conn;

    public int getSize() {
        return Objects.requireNonNullElseGet(custom_count, () -> getMembers().size());
    }

    public Guide getGuide() {
        return guide;
    }

    public Connection getConn() {
        return conn;
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    public Roll init(String date, int customCount) {
        this.date = date;
        this.custom_count = customCount;
        return this;
    }

    public Roll init(@Nullable Guide guide, String date, List<Member> members) {
        this.guide = guide;
        this.date = date;
        this.members = members;
        return this;
    }

    public List<Member> getMembers() {
        return members;
    }

    public LocalDate getDate() {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    public Map<String, String> parsePostData(Guide guide) {
        int member_count = members.size();
        if (member_count > 20 || getMembers() == null) {
            return null;
        }

        Map<String, String> ret = new HashMap<>();

        ret.put("iAgress", String.valueOf(1));
        ret.put("qty", String.valueOf(member_count));
        ret.put("ContactName", guide.contact_name);
        ret.put("nationality", guide.nationality);
        ret.put("ContactUserId", guide.contact_id);
        ret.put("ConactTel", guide.contact_tel);
        ret.put("ContactBirthday_Y", String.valueOf(guide.contact_birth_y));
        ret.put("ContactBirthday_M", String.valueOf(guide.contact_birth_m));
        ret.put("ContactBirthday_D", String.valueOf(guide.contact_birth_d));
        ret.put("ContactEmail", guide.contact_email);
        ret.put("ContactEmail2", guide.contact_email);
        ret.put("Bank", guide.bank);
        ret.put("SubBank", guide.sub_bank);
        ret.put("BankAccount", guide.bank_account);
        ret.put("BankName", guide.bank_name);

        for (int i = 0; i < member_count; i++) {
            ret.put("Name_" + i, members.get(i).name);
            ret.put("nationality_UserId_" + i, members.get(i).nationality);
            ret.put("UserId_" + i, members.get(i).id);
            ret.put("Tel_" + i, guide.contact_tel);
            ret.put("Birthday_" + i + "_Y", String.valueOf(members.get(i).birth_y));
            ret.put("Birthday_" + i + "_M", String.valueOf(members.get(i).birth_m));
            ret.put("Birthday_" + i + "_D", String.valueOf(members.get(i).birth_d));
            ret.put("Food_" + i, members.get(i).food);
            ret.put("EmergencyName_" + i, guide.contact_name);
            ret.put("EmergencyTel_" + i, guide.contact_tel);
        }

        return ret;
    }

    @Override
    public String toString() {
        return String.format("%s [%d]", date.substring(5, 10), members.size());
    }

    public String toStringAll() {
        StringBuilder builder = new StringBuilder().append(date).append(": ");

        try {
            if (getMembers() == null) return String.valueOf(members.size());
            for (Member i : getMembers()) {
                builder.append(i).append('\n');
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return builder.toString();
    }
}
