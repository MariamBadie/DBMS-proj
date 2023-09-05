import java.io.File;
import java.util.Hashtable;

public class test {
    public static void main(String[] args) {
        String a = "aa";
        String b = "bb";
        File file = new File("Pages/aa1.class");
        System.out.println(file.delete());
    }
}
