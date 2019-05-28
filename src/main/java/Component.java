import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class Component {
    private String compName;
    private List<Boolean> changeSequence;
    private List<List<Integer>> metrics;

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
        Utils.replace(changeSequence, commitIndex, Boolean.TRUE);
    }

    public void setMetrics(List<List<Integer>> metrics){
        this.metrics = metrics;
    }

    public List<Boolean> getChangeSequence(){
        return changeSequence;
    }

    @Override
    public String toString(){
        String component = compName + " : ";
        for(int index = 0; index < changeSequence.size(); index++){
            component += changeSequence.get(index).equals(Boolean.TRUE) ? "1" : "0";
        }
        return component;
    }

    public String metricsToString() {
        String metricsPrint = compName + "\n";
        int snapshotNumber = 0;

        for(List<Integer> metricsOfSnapshot : metrics){
            metricsPrint += "SNAPSHOT: " + snapshotNumber + " | ";

            for(Integer metric : metricsOfSnapshot){
                metricsPrint += metric + "\t";
            }
            metricsPrint += "\n";
            snapshotNumber++;
        }

        return metricsPrint;
    }
}