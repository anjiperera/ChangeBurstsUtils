import java.util.List;

public class Component {
    private String compName;
    private List<Boolean> changeSequence;

    public Component(String compName) {
        this.compName = compName;
    }

    public String getName() {
        return compName;
    }
}