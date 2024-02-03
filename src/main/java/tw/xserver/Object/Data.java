package tw.xserver.Object;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Data {
    public String date;
    public String[] members;


    public Member[] getMembers() {
        Member[] members_ret = new Member[members.length];

        for (int i = 0; i < members.length; i++) {
            String[] cur = members[i].split(" ");
            Integer[] birth = Arrays.stream(cur[2].split("\\.")).map(Integer::parseInt).toArray(Integer[]::new);
            members_ret[i] = new Member(cur[1], cur[0], birth[0], birth[1], birth[2]);
        }

        return members_ret;
    }

    public Map<String, String> parsePostData(Guide guide) {
        if (members.length > 20)
            return null;

        Map<String, String> ret = new HashMap<>();

        ret.put("iAgress", String.valueOf(1));
        ret.put("qty", String.valueOf(members.length));
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

        Member[] memCache = getMembers();
        for (int i = 0; i < members.length; i++) {
            ret.put("Name_" + i, memCache[i].name);
            ret.put("nationality_UserId_" + i, memCache[i].nationality);
            ret.put("UserId_" + i, memCache[i].id);
            ret.put("Tel_" + i, guide.contact_tel);
            ret.put("Birthday_" + i + "_Y", String.valueOf(memCache[i].birth_y));
            ret.put("Birthday_" + i + "_M", String.valueOf(memCache[i].birth_m));
            ret.put("Birthday_" + i + "_D", String.valueOf(memCache[i].birth_d));
            ret.put("Food_" + i, memCache[i].food);
            ret.put("EmergencyName_" + i, guide.contact_name);
            ret.put("EmergencyTel_" + i, guide.contact_tel);
        }

        return ret;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append(date).append(": ");
        for (Member i : getMembers()) {
            builder.append(i).append('\n');
        }

        return builder.toString();
    }
}
