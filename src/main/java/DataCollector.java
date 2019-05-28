import com.opencsv.CSVReader;

import java.io.FileReader;
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

    final static String FILE_RENAME_COMMIT_ID = "fc5c081e22a61bb5a6810af302be3f22f7966df4";

    static Map<Integer, Commit> commits;
    static Map<String, Component> components;

    public static void main(String args[]) {
        commits = new HashMap<Integer, Commit>();
        components = new HashMap<String, Component>();
        importCommitStats();
        importCommitAuthors();
        fillChangeSequences();

        printAllComponents();
        //printAllCommits();
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
    }

    private static void printAllCommits(){
        System.out.println("Total Commits: " + commits.size());
        for(Commit commit: commits.values()){
            System.out.println(commit.getCommitIndex() + " : " + commit.getCommitId() + " : " + commit.getAuthor());
        }
    }
}
