import java.util.ArrayList;
import java.util.List;

public class Component {
    private String compName;
    private List<Boolean> changeSequence;

    public Component(String compName) {
        changeSequence = new ArrayList<>();
        for(int index = 0; index < DataCollector.TOTAL_COMMITS; index++){
            changeSequence.add(Boolean.FALSE);
        }
        this.compName = compName;
    }

    public String getName() {
        return compName;
    }

    public void changedAt(int commitIndex){
        changeSequence.add(commitIndex, Boolean.TRUE);
    }

    @Override
    public String toString(){
        String component = compName + " : ";
        for(int index = 0; index < changeSequence.size(); index++){
            component += changeSequence.get(index).equals(Boolean.TRUE) ? "1" : "0";
        }
        return component;
    }
}