package tw.xserver.Object;

import java.util.Arrays;

public class Member {
    public String name;
    public String nationality;
    public String id;
    public int birth_y;
    public int birth_m;
    public int birth_d;
    public String food;

    public Member(String name, String id, int birthday_y, int birthday_m, int birthday_d) {
        this.name = name;
        this.id = id;
        this.birth_y = birthday_y;
        this.birth_m = birthday_m;
        this.birth_d = birthday_d;

        this.nationality = "Taiwan";
        this.food = "葷食";
    }

    public Member(String name, String id, String birthday) {
        this(name, id, 0, 0, 0);

        Integer[] birth = Arrays.stream(birthday.split("\\."))
                .map(Integer::parseInt)
                .toArray(Integer[]::new);
        this.birth_y = birth[0];
        this.birth_m = birth[1];
        this.birth_d = birth[2];
    }

    @Override
    public String toString() {
        return "Member{" +
                "name='" + name + '\'' +
                ", year='" + birth_y + '\'' +
                ", month='" + birth_m + '\'' +
                ", day='" + birth_d + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
