package tw.xserver.Object;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Data {
    public String date;
    public String[] members;
    public Integer member_count;
    public Member[] membersAry;

    public Member[] getMembersAry() {
        if (membersAry != null) return membersAry;
        if (members == null) return null;

        member_count = members.length;
        Member[] members_ret = new Member[member_count];
        for (int i = 0; i < member_count; i++) {
            String[] cur = members[i].split(" ");
            if (cur.length != 3) return null;

            Integer[] birth = Arrays.stream(cur[2].split("\\.")).map(Integer::parseInt).toArray(Integer[]::new);
            members_ret[i] = new Member(cur[1], cur[0], birth[0], birth[1], birth[2]);
        }

        membersAry = members_ret;
        return membersAry;
    }

    public Map<String, String> parsePostData(Guide guide) {
        if (member_count > 20 || getMembersAry() == null) {
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
            ret.put("Name_" + i, getMembersAry()[i].name);
            ret.put("nationality_UserId_" + i, getMembersAry()[i].nationality);
            ret.put("UserId_" + i, getMembersAry()[i].id);
            ret.put("Tel_" + i, guide.contact_tel);
            ret.put("Birthday_" + i + "_Y", String.valueOf(getMembersAry()[i].birth_y));
            ret.put("Birthday_" + i + "_M", String.valueOf(getMembersAry()[i].birth_m));
            ret.put("Birthday_" + i + "_D", String.valueOf(getMembersAry()[i].birth_d));
            ret.put("Food_" + i, getMembersAry()[i].food);
            ret.put("EmergencyName_" + i, guide.contact_name);
            ret.put("EmergencyTel_" + i, guide.contact_tel);
        }

        return ret;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append(date).append(": ");
        if (getMembersAry() == null)
            return String.valueOf(member_count);

        for (Member i : getMembersAry()) {
            builder.append(i).append('\n');
        }

        return builder.toString();
    }
}
