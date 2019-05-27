import javafx.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Commit {
    private int commitIndex;
    private String commitId;
    private Map<Component, Integer> changedComponents;
    private String author;

    public Commit(int commitIndex, String commitId) {
        changedComponents = new HashMap<Component, Integer>();
        this.commitIndex = commitIndex;
        this.commitId = commitId;
    }

    public void insertChange(Component component, int changedLines) {
        if (changedComponents.containsKey(component)) {
            throw new RuntimeException("Same Component changed twice in a single commit: Something wrong with the data" +
                    " extraction. Commit Index: " + commitIndex + ", Commit ID: " + commitId + ", Component: " +
                    component.getName());
        }
        changedComponents.put(component, changedLines);
    }
}
