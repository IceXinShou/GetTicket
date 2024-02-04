package tw.xserver.Object;

import java.time.LocalDateTime;

public class Config {
    public String begin_time;
    public long send_delay;
    public Guide guide;
    public Data[] data;

    public LocalDateTime getDateTime() {
        return LocalDateTime.parse(begin_time);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder
                .append("主要聯絡人資訊：")
                .append("\n  姓名：").append(guide.contact_name)
                .append("\n  國籍：").append(guide.nationality.equals("Taiwan") ? "本國國籍" : "非本國國籍")
                .append("\n  身分證字號：").append(guide.contact_id)
                .append("\n  聯絡電話：").append(guide.contact_tel)
                .append("\n  出生日期：").append(guide.contact_birth_y).append('/').append(guide.contact_birth_m).append('/').append(guide.contact_birth_d)
                .append("\n  電子信箱：").append(guide.contact_email)

                .append("\n\n退款帳號資訊：")
                .append("\n  銀行名稱：").append(guide.bank)
                .append("\n  分行名稱：").append(guide.sub_bank)
                .append("\n  銀行帳號：").append(guide.bank_account)
                .append("\n  銀行戶名：").append(guide.bank_name);

        builder.append("\n");
        for (Data i : data) {
            builder.append("\n\n日期：").append(i.date);

            if (i.getMembersAry() == null) {
                builder.append("\n成員數：").append(i.members.length);
            } else {
                for (int j = 0; j < i.members.length; j++) {
                    Member member = i.getMembersAry()[j];

                    builder
                            .append("\n  成員 ").append(String.format("%02d", j + 1)).append("：")
                            .append("\n    姓名：").append(member.name)
                            .append("\n    國籍：").append(member.nationality.equals("Taiwan") ? "本國國籍" : "非本國國籍")
                            .append("\n    身分證字號：").append(member.id)
                            .append("\n    聯絡電話：").append(guide.contact_tel)
                            .append("\n    葷素食：").append(member.food)
                            .append("\n    出生日期：").append(member.birth_y).append('/').append(member.birth_m).append('/').append(member.birth_d)
                            .append("\n    緊急聯絡人姓名：").append(guide.contact_name)
                            .append("\n    緊急聯絡人連絡電話：").append(guide.contact_tel)
                            .append('\n');
                }
            }
        }

        return builder.toString();
    }
}
