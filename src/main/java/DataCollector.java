import com.opencsv.CSVReader;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataCollector {
    final static int INDEX_COMMIT_INDEX = 0;
    final static int INDEX_COMMIT_ID = 1;
    final static int INDEX_CLASS_NAME = 2;
    final static int INDEX_CHANGED_LINES = 3;

    final static String FILE_RENAME_COMMIT_ID = "fc5c081e22a61bb5a6810af302be3f22f7966df4";

    static Map<Integer, Commit> commits;
    static Map<String, Component> components;

    public static void main(String args[]) {
        commits = new HashMap<Integer, Commit>();
        components = new HashMap<String, Component>();
        importCommitStats();

        printAllComponents();
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

                if(commitId.equals(FILE_RENAME_COMMIT_ID)){
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
                    int startIndex = className.indexOf("org/apache");
                    if (startIndex == -1) {
                        throw new RuntimeException("Unexpected ClassName. Class Name: " + className);
                    } else {
                        String uniqueClassName = className.substring(startIndex);
                        if (components.get(uniqueClassName) == null) {
                            components.put(uniqueClassName, new Component(uniqueClassName));
                        }

                        Commit commit = commits.get(commitIndex);
                        commit.insertChange(components.get(uniqueClassName), changedLines);
                    }
                } else {
                    //System.out.println("Ignoring an irrelevant class. ClassName: " + className);
                }

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

    private static void printAllComponents() {
        for (Component component : components.values()) {
            System.out.println(component.getName());
        }
    }
}
