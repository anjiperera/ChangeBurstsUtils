import java.util.List;

public class Utils {
    public static void replace(List<Integer> list, int index, int value){
        list.remove(index);
        list.add(index, value);
    }
    public static void replace(List<Boolean> list, int index, Boolean object){
        list.remove(index);
        list.add(index, object);
    }
}
