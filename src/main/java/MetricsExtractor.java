import java.util.*;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class MetricsExtractor {
    private static final Integer INDEX_NON_CHANGE = 0;
    private static final Integer INDEX_NORMAL_BURST = 1;
    private static final Integer INDEX_START_BURST = 2;
    private static final Integer INDEX_END_BURST = 3;
    private static final Integer INDEX_NON_BURST_CHANGE = 4;
    public static int GAP_SIZE = 1;
    public static int BURST_SIZE = 1;
    public static int SNAPSHOT_GAP = 450;
    public static int TOTAL_SNAPSHOTS = 9;

    final static int INDEX_NOC = 0;
    final static int INDEX_NOCC = 1;
    final static int INDEX_NOCB = 2;
    final static int INDEX_TBS = 3;
    final static int INDEX_MCB = 4;
    final static int INDEX_NOCE = 5;
    final static int INDEX_NOCCE = 6;
    final static int INDEX_NOCBE = 7;
    final static int INDEX_TBSE = 8;
    final static int INDEX_MCBE = 9;
    final static int INDEX_NOCL = 10;
    final static int INDEX_NOCCL = 11;
    final static int INDEX_NOCBL = 12;
    final static int INDEX_TBSL = 13;
    final static int INDEX_MCBL = 14;
    final static int INDEX_TFB = 15;
    final static int INDEX_TLB = 16;
    final static int INDEX_TMB = 17;
    final static int INDEX_PT = 18;
    final static int INDEX_TPIB = 19;
    final static int INDEX_MPIB = 20;
    final static int INDEX_CT = 21;
    final static int INDEX_TCIB = 22;
    final static int INDEX_MCIB = 23;

    enum ProjectLife {
        Full, Early, Late;
    }

    public static List<List<Integer>> extractMetrics(List<Boolean> changedSequence, Map<Integer, Commit> commits, Component component) {
        List<List<Integer>> metrics = new ArrayList<>();
        int[] changeBurstsValues;

        for (int snapshot = 0; snapshot < TOTAL_SNAPSHOTS; snapshot++) {
            List<Integer> metricsOfSnapshot = new ArrayList<>();
            Map<Integer, Commit> commitsOfSnapshot = new HashMap<>();
            for (int index = snapshot * SNAPSHOT_GAP; index <= snapshot * SNAPSHOT_GAP + SNAPSHOT_GAP - 1; index++) {
                commitsOfSnapshot.put(index - snapshot * SNAPSHOT_GAP, commits.get(index));
            }

            List<Integer> changeBursts = getChangeBursts(changedSequence, snapshot * SNAPSHOT_GAP,
                    snapshot * SNAPSHOT_GAP + SNAPSHOT_GAP - 1);
            calculateChangeMetrics(changeBursts, metricsOfSnapshot, ProjectLife.Full);
            calculateTimeMetrics(changeBursts, metricsOfSnapshot);
            calculatePeopleMetrics(changeBursts, metricsOfSnapshot, commitsOfSnapshot);
            calculateChurnMetrics(changeBursts, metricsOfSnapshot, commits, component);

            int originalBurstSize = BURST_SIZE;
            BURST_SIZE = 1;

            changeBursts = getChangeBursts(changedSequence, snapshot * SNAPSHOT_GAP,
                    snapshot * SNAPSHOT_GAP + SNAPSHOT_GAP - 1);
            changeBursts.remove(SNAPSHOT_GAP);  //this carries the Number of Changes

            calculateConsecutiveChanges(changeBursts, metricsOfSnapshot, ProjectLife.Full);

            BURST_SIZE = originalBurstSize;

            //80% Early
            changeBursts = getChangeBursts(changedSequence, snapshot * SNAPSHOT_GAP,
                    snapshot * SNAPSHOT_GAP + (SNAPSHOT_GAP * 80) / 100 - 1);
            calculateChangeMetrics(changeBursts, metricsOfSnapshot, ProjectLife.Early);

            BURST_SIZE = 1;

            changeBursts = getChangeBursts(changedSequence, snapshot * SNAPSHOT_GAP,
                    snapshot * SNAPSHOT_GAP + (SNAPSHOT_GAP * 80) / 100 - 1);
            changeBursts.remove(changeBursts.size() - 1);  //this carries the Number of Changes

            calculateConsecutiveChanges(changeBursts, metricsOfSnapshot, ProjectLife.Early);

            BURST_SIZE = originalBurstSize;

            //20% Late
            changeBursts = getChangeBursts(changedSequence, snapshot * SNAPSHOT_GAP + (SNAPSHOT_GAP * 80) / 100,
                    snapshot * SNAPSHOT_GAP + SNAPSHOT_GAP - 1);
            calculateChangeMetrics(changeBursts, metricsOfSnapshot, ProjectLife.Late);

            BURST_SIZE = 1;

            changeBursts = getChangeBursts(changedSequence, snapshot * SNAPSHOT_GAP + (SNAPSHOT_GAP * 80) / 100,
                    snapshot * SNAPSHOT_GAP + SNAPSHOT_GAP - 1);
            changeBursts.remove(changeBursts.size() - 1);  //this carries the Number of Changes

            calculateConsecutiveChanges(changeBursts, metricsOfSnapshot, ProjectLife.Late);


            metrics.add(snapshot, metricsOfSnapshot);
        }

        return metrics;
    }

    /*
        If the BURST_SIZE = 1 and then the smallest burst will be like {INDEX_END_BURST}
        changedBursts[endIndex - startIndex + 1] carries the numberOfChanges
    */
    private static List<Integer> getChangeBursts(List<Boolean> changedSequence, int startIndex, int endIndex) {
        int changeCount = 0;
        boolean isBurst = false;
        int lastChangedIndex = 0;
        int numberOfChanges = 0;

        List<Integer> changeBursts = new ArrayList<>();
        for (int index = 0; index <= endIndex - startIndex; index++) {
            changeBursts.add(index, INDEX_NON_CHANGE);
        }

        Stack<Integer> tempBurst = new Stack<>();
        for (int index = startIndex; index <= endIndex; index++) {

            if (changedSequence.get(index) == TRUE) {
                numberOfChanges++;
                changeBursts.add(index - startIndex, INDEX_NON_BURST_CHANGE);
                lastChangedIndex = index;

                if (!isBurst) {
                    tempBurst.push(index - startIndex);
                    changeCount++;
                    if (changeCount >= BURST_SIZE) {
                        isBurst = true;
                        updateChangeBursts(changeBursts, tempBurst);
                        assert tempBurst.empty();
                    }
                } else {
                    changeBursts.add(index - startIndex, INDEX_NORMAL_BURST);
                }

            } else {
                assert changedSequence.get(index) == FALSE;
                assert (index - lastChangedIndex) < GAP_SIZE || !isBurst;

                if ((index - lastChangedIndex) == GAP_SIZE) {
                    changeCount = 0;
                    if (isBurst) {
                        changeBursts.add(lastChangedIndex - startIndex, INDEX_END_BURST);
                    } else {
                        tempBurst.clear();
                    }
                    isBurst = false;
                }
            }

        }

        if (isBurst) {
            changeBursts.add(lastChangedIndex - startIndex, INDEX_END_BURST);
        }

        changeBursts.add(endIndex - startIndex + 1, numberOfChanges);
        return changeBursts;
    }

    private static void updateChangeBursts(List<Integer> changeBursts, Stack<Integer> tempBurst) {
        while (!tempBurst.empty()) {
            if (tempBurst.size() == 1) {
                changeBursts.add(tempBurst.pop(), INDEX_START_BURST);
            } else {
                changeBursts.add(tempBurst.pop(), INDEX_NORMAL_BURST);
            }
        }
    }

    private static void calculateChangeMetrics(List<Integer> changeBursts, List<Integer> metrics, Enum projectLife) {
        int totalPoints = changeBursts.size();
        int numberOfChanges = changeBursts.get(totalPoints - 1);
        changeBursts.remove(totalPoints - 1);

        int numberOfChangeBursts = 0;
        int totalBurstSize = 0;
        int maxChangeBurst = 0;
        int burstSize = 0;

        for (Integer point : changeBursts) {
            if (point == INDEX_END_BURST) {
                numberOfChangeBursts++;
                totalBurstSize++;
                burstSize++;
                if (burstSize > maxChangeBurst) {
                    maxChangeBurst = burstSize;
                }
                burstSize = 0;
            } else if (point == INDEX_START_BURST) {
                totalBurstSize++;
                burstSize++;
            } else if (point == INDEX_NORMAL_BURST) {
                totalBurstSize++;
                burstSize++;
            }
        }

        insertChangeMetrics(metrics, numberOfChanges, numberOfChangeBursts, totalBurstSize, maxChangeBurst, projectLife);
    }

    private static void insertChangeMetrics(List<Integer> metrics, int numberOfChanges, int numberOfChangeBursts,
                                            int totalBurstSize, int maxChangeBurst, Enum projectLife) {
        if (projectLife == ProjectLife.Full) {
            metrics.add(INDEX_NOC, numberOfChanges);
            metrics.add(INDEX_NOCB, numberOfChangeBursts);
            metrics.add(INDEX_TBS, totalBurstSize);
            metrics.add(INDEX_MCB, maxChangeBurst);
        } else if (projectLife == ProjectLife.Early) {
            metrics.add(INDEX_NOCE, numberOfChanges);
            metrics.add(INDEX_NOCBE, numberOfChangeBursts);
            metrics.add(INDEX_TBSE, totalBurstSize);
            metrics.add(INDEX_MCBE, maxChangeBurst);
        } else if (projectLife == ProjectLife.Late) {
            metrics.add(INDEX_NOCL, numberOfChanges);
            metrics.add(INDEX_NOCBL, numberOfChangeBursts);
            metrics.add(INDEX_TBSL, totalBurstSize);
            metrics.add(INDEX_MCBL, maxChangeBurst);
        }
    }

    private static void calculateTimeMetrics(List<Integer> changeBursts, List<Integer> metrics) {
        int timeFirstBurst = 0;
        for (int index = 0; index < changeBursts.size(); index++) {
            if (changeBursts.get(index) == INDEX_START_BURST || changeBursts.get(index) == INDEX_END_BURST) {
                timeFirstBurst = index;
                break;
            }
        }
        metrics.add(INDEX_TFB, timeFirstBurst);

        int timeLastBurst = 0;
        boolean lastEndBurstFound = false;
        for (int index = changeBursts.size() - 1; index >= 0; index--) {
            if (!lastEndBurstFound) {
                if (changeBursts.get(index) == INDEX_END_BURST) {
                    timeLastBurst = index;
                    lastEndBurstFound = true;
                }
            } else {
                if (changeBursts.get(index) == INDEX_END_BURST) {
                    break;
                } else if (changeBursts.get(index) == INDEX_START_BURST) {
                    timeLastBurst = index;
                    break;
                }
            }
        }
        metrics.add(INDEX_TLB, timeLastBurst);

        int timeMaxBurst = 0;
        int burstSize = 0;
        int maxBurstSize = 0;
        int firstStartBurst = 0;

        for (int index = 0; index < changeBursts.size(); index++) {
            if (changeBursts.get(index) == INDEX_END_BURST) {
                burstSize++;
                if (burstSize > maxBurstSize) {
                    maxBurstSize = burstSize;
                    timeMaxBurst = firstStartBurst;
                }
                burstSize = 0;
            } else if (changeBursts.get(index) == INDEX_START_BURST) {
                burstSize++;
                firstStartBurst = index;
            } else if (changeBursts.get(index) == INDEX_NORMAL_BURST) {
                burstSize++;
            }
        }
        metrics.add(INDEX_TMB);
    }

    private static void calculatePeopleMetrics(List<Integer> changeBursts, List<Integer> metrics, Map<Integer, Commit> commits) {
        Set<String> allAuthors = new HashSet<>();
        Set<String> allAuthorsInBursts = new HashSet<>();
        Set<String> authorsInBurst = new HashSet<>();
        int maxPeopleInBurst = 0;

        for (int index = 0; index < changeBursts.size(); index++) {
            int point = changeBursts.get(index);

            if (point == INDEX_START_BURST || point == INDEX_NORMAL_BURST || point == INDEX_END_BURST) {
                allAuthorsInBursts.add(commits.get(index).getAuthor());
                allAuthors.add(commits.get(index).getAuthor());
                authorsInBurst.add(commits.get(index).getAuthor());
                if (point == INDEX_END_BURST) {
                    if (authorsInBurst.size() > maxPeopleInBurst) {
                        maxPeopleInBurst = authorsInBurst.size();
                    }
                    authorsInBurst.clear();
                }
            }
            if (point == INDEX_NON_BURST_CHANGE) {
                allAuthors.add(commits.get(index).getAuthor());
            }
        }

        metrics.add(INDEX_PT, allAuthors.size());
        metrics.add(INDEX_TPIB, allAuthorsInBursts.size());
        metrics.add(INDEX_MPIB, maxPeopleInBurst);
    }

    private static void calculateChurnMetrics(List<Integer> changeBursts, List<Integer> metrics,
                                              Map<Integer, Commit> commits, Component component) {
        int totalChurn = 0;
        int totalChurnInBurst = 0;
        int churnInBurst = 0;
        int maxChurnInBurst = 0;

        for (int index = 0; index < changeBursts.size(); index++) {
            int point = changeBursts.get(index);

            if (point == INDEX_START_BURST || point == INDEX_NORMAL_BURST || point == INDEX_END_BURST) {
                totalChurnInBurst += commits.get(index).getNumberOfChangedLines(component);
                totalChurn += commits.get(index).getNumberOfChangedLines(component);
                churnInBurst += commits.get(index).getNumberOfChangedLines(component);
                if (point == INDEX_END_BURST) {
                    if (churnInBurst > maxChurnInBurst) {
                        maxChurnInBurst = churnInBurst;
                    }
                }
            }
            if (point == INDEX_NON_BURST_CHANGE) {
                totalChurn += commits.get(index).getNumberOfChangedLines(component);
            }
        }

        metrics.add(INDEX_CT, totalChurn);
        metrics.add(INDEX_TCIB, totalChurnInBurst);
        metrics.add(INDEX_MCIB, maxChurnInBurst);

    }

    private static void calculateConsecutiveChanges(List<Integer> changeBursts, List<Integer> metrics, ProjectLife projectLife) {
        int numberOfConsecutiveChanges = 0;
        for (Integer point : changeBursts) {
            if (point == INDEX_END_BURST) {
                numberOfConsecutiveChanges++;
            }
        }

        insertConsecutiveChangeMetrics(metrics, numberOfConsecutiveChanges, projectLife);
        metrics.add(INDEX_NOCC, numberOfConsecutiveChanges);
    }

    private static void insertConsecutiveChangeMetrics(List<Integer> metrics, int numberOfConsecutiveChanges,
                                                       ProjectLife projectLife) {
        if (projectLife == ProjectLife.Full) {
            metrics.add(INDEX_NOCC, numberOfConsecutiveChanges);
        } else if (projectLife == ProjectLife.Early) {
            metrics.add(INDEX_NOCCE, numberOfConsecutiveChanges);
        } else if (projectLife == ProjectLife.Late) {
            metrics.add(INDEX_NOCCL, numberOfConsecutiveChanges);
        }
    }

}
