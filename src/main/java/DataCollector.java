import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataCollector {
    final static int TOTAL_COMMITS = 5447;
    final static int INDEX_COMMIT_INDEX = 0;
    final static int INDEX_COMMIT_ID = 1;
    final static int INDEX_CLASS_NAME = 2;
    final static int INDEX_CHANGED_LINES = 3;
    final static int INDEX_AUTHOR = 2;

    final static String FILE_RENAME_COMMIT_ID_1 = "fc5c081e22a61bb5a6810af302be3f22f7966df4";
    final static String FILE_RENAME_COMMIT_ID_2 = "debc02c6d9b94c717b4182ae4dd7a97d47293a52";

    static Map<Integer, Commit> commits;
    static Map<String, Component> components;

    public static void main(String args[]) {
        commits = new HashMap<Integer, Commit>();
        components = new HashMap<String, Component>();
        importCommitStats();
        importCommitAuthors();
        fillChangeSequences();

        for(Component component : components.values()){
            MetricsExtractor.extractMetrics(commits, component);
        }

        //printFirstAndLastChangedIndices("math/NumberUtils");
        //printFirstAndLastChangedIndices("NumberUtils");
        //printFirstAndLastChangedIndices("org/apache/commons/lang/math/NumberUtils");

        //System.out.println(getAllTheChangedComponents(4).size());
        //writeMetrics("C:\\Research\\Defects4J_Projects\\Utils\\lang\\metrics_snapshot", getAllTheChangedComponents(4), 4);
        //printMetricsForComponent("org/apache/commons/lang3/math/NumberUtils");
        //printAllComponents();
        //printAllCommits();
    }

    private static void printFirstAndLastChangedIndices(String componentName) {
        String output = componentName + " : First Change Index: " + components.get(componentName).getFirstChangedIndex()
                + ", Last Change Index: " + components.get(componentName).getLastChangedIndex();
        System.out.println(output);
    }

    private static List<Component> getAllTheChangedComponents(int snapshot) {
        List<Component> changedComponents = new ArrayList<>();
        for(Component component : components.values()){
            if(component.getMetrics(snapshot).get(MetricsExtractor.INDEX_NOC) > 0){
                changedComponents.add(component);
            }
        }

        return changedComponents;
    }

    private static void writeMetrics(String filePath, List<Component> components, int snapshot){
        filePath = filePath + "_" + snapshot + ".csv";
        File file = new File(filePath);
        try {
            FileWriter outputFile = new FileWriter(file);

            CSVWriter writer = new CSVWriter(outputFile);

            String[] header = { "Component", "NOC", "NOCC", "NOCB", "TBS", "MCB", "NOCE", "NOCCE", "NOCBE", "TBSE", "MCBE",
            "NOCL", "NOCCL", "NOCBL", "TBSL", "MCBL", "TFB", "TLB", "TMB", "PT", "TPIB", "MPIB", "CT", "TCIB", "MCIB"};
            writer.writeNext(header);

            String[] row = new String[25];

            for(Component component : components){
                row[0] = component.getName();
                int metricIndex = 1;
                for(Integer metric : component.getMetrics(snapshot)){
                    row[metricIndex] = metric.toString();
                    metricIndex++;
                }
                writer.writeNext(row);
            }

            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printMetricsForComponent(String compName) {
        if(!components.containsKey(compName)){
            throw new RuntimeException("Component Name is not found. Name: " + compName);
        }
        System.out.println(components.get(compName).metricsToString());
    }

    private static void importCommitStats() {
        try {
            FileReader fileReader = new FileReader("C:\\Research\\Defects4J_Projects\\Utils\\lang\\change_details.csv");
            CSVReader csvReader = new CSVReader(fileReader);
            String[] nextRecord;

            while ((nextRecord = csvReader.readNext()) != null) {
                int commitIndex = Integer.parseInt(nextRecord[INDEX_COMMIT_INDEX]);
                String commitId = nextRecord[INDEX_COMMIT_ID];
                commitId = commitId.split("\n")[0];
                String className = nextRecord[INDEX_CLASS_NAME];
                int changedLines = Integer.parseInt(nextRecord[INDEX_CHANGED_LINES]);

                if(commitId.equals(FILE_RENAME_COMMIT_ID_1) || commitId.equals(FILE_RENAME_COMMIT_ID_2)){
                    //System.out.println("Skipping file rename commit. Empty commit will be created. Commit ID: " + commitId);
                    if (commits.get(commitIndex) == null) {
                        commits.put(commitIndex, new Commit(commitIndex, commitId));
                    }
                    continue;
                }

                if (commits.get(commitIndex) == null) {
                    commits.put(commitIndex, new Commit(commitIndex, commitId));
                }

                boolean testClass = checkTestClasses(className);
                boolean undefinedPackage = checkForUndefinedPackages(className);

                if (!testClass && !undefinedPackage && !className.equals("commit_not_found") &&
                        !className.equals("no_classes_changed")) {
                    String uniqueClassName = null;
                    if(className.contains("org/apache/commons/lang/")){
                        uniqueClassName = className.split("org/apache/commons/lang/")[1];
                    } else if(className.contains("org/apache/commons/lang3/")){
                        uniqueClassName = className.split("org/apache/commons/lang3/")[1];
                    }

                    assert uniqueClassName != null;

                    if (components.get(uniqueClassName) == null) {
                        components.put(uniqueClassName, new Component(uniqueClassName));
                    }

                    Commit commit = commits.get(commitIndex);
                    commit.insertChange(components.get(uniqueClassName), changedLines);
                    /*int startIndex = className.indexOf("org/apache");
                    if (startIndex == -1) {
                        throw new RuntimeException("Unexpected ClassName. Class Name: " + className);
                    } else {
                        String uniqueClassName = className.substring(startIndex);
                        if (components.get(uniqueClassName) == null) {
                            components.put(uniqueClassName, new Component(uniqueClassName));
                        }

                        Commit commit = commits.get(commitIndex);
                        commit.insertChange(components.get(uniqueClassName), changedLines);
                    }*/
                } else {
                    //System.out.println("Ignoring an irrelevant class. ClassName: " + className);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //This method should be called after calling importCommitStats()
    private static void importCommitAuthors(){
        try {
            FileReader fileReader = new FileReader("C:\\Research\\Defects4J_Projects\\Utils\\lang\\commit_authors.csv");
            CSVReader csvReader = new CSVReader(fileReader);
            String[] nextRecord;

            while ((nextRecord = csvReader.readNext()) != null) {
                int commitIndex = Integer.parseInt(nextRecord[INDEX_COMMIT_INDEX]);
                String commitId = nextRecord[INDEX_COMMIT_ID];
                commitId = commitId.split("\n")[0];
                String author = nextRecord[INDEX_AUTHOR];

                if(!commits.containsKey(commitIndex)){
                    throw new RuntimeException("Unknown Commit. Commit Index: " + commitIndex + " Commit ID: " + commitId);
                }

                Commit commit = commits.get(commitIndex);
                if(!commitId.equals(commit.getCommitId())){
                    throw new RuntimeException("Commit Index does not match with Commit ID. Commit Index: " +
                            commitIndex + " Commit ID: " + commitId);
                }

                commit.setAuthor(author);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean checkForUndefinedPackages(String className) {
        if (className.contains("src/pending/")) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean checkTestClasses(String className) {
        if (className.contains("src/test/org") || className.contains("src/test/java/org/")) {
            return true;
        } else {
            return false;
        }
    }

    private static void  fillChangeSequences(){
        for(Commit commit : commits.values()){
            for(Component component : commit.getChangedComponents()){
                component.changedAt(commit.getCommitIndex());
            }
        }
    }

    private static void printAllComponents() {
        for (Component component : components.values()) {
            System.out.println(component.toString());
        }
        System.out.println("Total Components: " + components.size());
    }

    private static void printAllCommits(){
        System.out.println("Total Commits: " + commits.size());
        for(Commit commit: commits.values()){
            System.out.println(commit.getCommitIndex() + " : " + commit.getCommitId() + " : " + commit.getAuthor());
        }
    }
}
