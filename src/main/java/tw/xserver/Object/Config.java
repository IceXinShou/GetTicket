package tw.xserver.Object;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Config {
    public String begin_time;
    public Long send_delay;
    public Boolean gui;
    public Boolean burst;
    public Guide guide;
    public Roll[] custom_data;
    public List<Roll> roll = new ArrayList<>();

    public boolean hasCustom() {
        return custom_data != null;
    }

    public LocalDateTime getDateTime() {
        return LocalDateTime.parse(begin_time);
    }

    @Override
    public String toString() {
        int total_member = 0;
        int total_days = roll.size() + ((custom_data == null) ? 0 : custom_data.length);
        int total_split_count = 0;

        StringBuilder builder = new StringBuilder();
        builder
                .append("開始時間: ").append(getDateTime().toString()).append('\n')
                .append("封包延遲: ").append(send_delay).append(" ms").append('\n')

                .append("\n\n主要聯絡人資訊: ")
                .append("\n  姓名: ").append(guide.contact_name)
                .append("\n  國籍: ").append(guide.nationality.equals("Taiwan") ? "本國國籍" : "非本國國籍")
                .append("\n  身分證字號: ").append(guide.contact_id)
                .append("\n  聯絡電話: ").append(guide.contact_tel)
                .append("\n  出生日期: ").append(guide.contact_birth_y).append('/').append(guide.contact_birth_m).append('/').append(guide.contact_birth_d)
                .append("\n  電子信箱: ").append(guide.contact_email)

                .append("\n\n退款帳號資訊: ")
                .append("\n  銀行名稱: ").append(guide.bank)
                .append("\n  分行名稱: ").append(guide.sub_bank)
                .append("\n  銀行帳號: ").append(guide.bank_account)
                .append("\n  銀行戶名: ").append(guide.bank_name);

        builder.append("\n");

        if (hasCustom())
            for (Roll i : custom_data) {
                builder.append("\n\n日期: ").append(i.date);
                total_member += i.custom_count;
                total_split_count += (i.custom_count / 20) + ((i.custom_count % 20 == 0) ? 0 : 1);
                builder
                        .append(" [僅取得報名連結]")
                        .append("\n成員數: ").append(i.custom_count).append('\n');
            }

        for (Roll i : roll) {
            int member_count = i.getMembers().size();

            builder.append("\n\n日期: ").append(i.date);
            total_member += member_count;
            total_split_count += (member_count / 20) + ((member_count % 20 == 0) ? 0 : 1);
            try {
                builder.append(" [自動填寫報名表]");
                for (int j = 0; j < i.getMembers().size(); j++) {
                    Member member = i.getMembers().get(j);

                    builder
                            .append("\n  成員 ").append(String.format("%02d", j + 1)).append(": ")
                            .append("\n    姓名: ").append(member.name)
                            .append("\n    國籍: ").append(member.nationality.equals("Taiwan") ? "本國國籍" : "非本國國籍")
                            .append("\n    身分證字號: ").append(member.id)
                            .append("\n    聯絡電話: ").append(guide.contact_tel)
                            .append("\n    葷素食: ").append(member.food)
                            .append("\n    出生日期: ").append(member.birth_y).append('/').append(member.birth_m).append('/').append(member.birth_d)
                            .append("\n    緊急聯絡人姓名: ").append(guide.contact_name)
                            .append("\n    緊急聯絡人連絡電話: ").append(guide.contact_tel)
                            .append('\n');
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        builder.insert(0,
                "成員總數: " + total_member + '\n' +
                        "天數: " + total_days + '\n' +
                        "單數: " + total_split_count + "\n\n"
        );

        return builder.toString();
    }
}
