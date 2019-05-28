import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class MetricsExtractor {
    private static final Integer INDEX_START_BURST = 2;
    private static final Integer INDEX_END_BURST = 3;
    public static int GAP_SIZE = 1;
    public static int BURST_SIZE = 1;
    public static int SNAPSHOT_GAP = 450;
    public static int INDEX_START_COMMIT = 0;
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

    public static List<List<Integer>> extractMetrics(List<Boolean> changedSequence){
        //List<Integer> metricsOf = new ArrayList<>();
        List<List<Integer>> metrics = new ArrayList<>();
        int[] changeBurstsValues;

        for(int snapshot = 0; snapshot < TOTAL_SNAPSHOTS; snapshot++){
            changeBurstsValues = calculateChangeBursts(changedSequence, snapshot*SNAPSHOT_GAP,
                    snapshot*SNAPSHOT_GAP + SNAPSHOT_GAP - 1);

            List<Integer> metricsOfSnapshot = new ArrayList<>();

            metricsOfSnapshot.add(INDEX_NOC, changeBurstsValues[0]);
            metricsOfSnapshot.add(INDEX_NOCB, changeBurstsValues[1]);
            metricsOfSnapshot.add(INDEX_TBS, changeBurstsValues[2]);
            metricsOfSnapshot.add(INDEX_MCB, changeBurstsValues[3]);

            int originalBurstSize = BURST_SIZE;

            BURST_SIZE = 1;

            changeBurstsValues = calculateChangeBursts(changedSequence, snapshot*SNAPSHOT_GAP,
                    snapshot*SNAPSHOT_GAP + SNAPSHOT_GAP - 1);

            BURST_SIZE = originalBurstSize;

            metricsOfSnapshot.add(INDEX_NOCC, changeBurstsValues[1]);

            //80% Early

            changeBurstsValues = calculateChangeBursts(changedSequence, snapshot*SNAPSHOT_GAP,
                    snapshot*SNAPSHOT_GAP + (SNAPSHOT_GAP*80)/100 - 1);

            metricsOfSnapshot.add(INDEX_NOCE, changeBurstsValues[0]);
            metricsOfSnapshot.add(INDEX_NOCBE, changeBurstsValues[1]);
            metricsOfSnapshot.add(INDEX_TBSE, changeBurstsValues[2]);
            metricsOfSnapshot.add(INDEX_MCBE, changeBurstsValues[3]);

            BURST_SIZE = 1;

            changeBurstsValues = calculateChangeBursts(changedSequence, snapshot*SNAPSHOT_GAP,
                    snapshot*SNAPSHOT_GAP + (SNAPSHOT_GAP*80)/100 - 1);

            BURST_SIZE = originalBurstSize;

            metricsOfSnapshot.add(INDEX_NOCCE, changeBurstsValues[1]);

            //20% Late

            changeBurstsValues = calculateChangeBursts(changedSequence, snapshot*SNAPSHOT_GAP + (SNAPSHOT_GAP*80)/100,
                    snapshot*SNAPSHOT_GAP + SNAPSHOT_GAP - 1);

            metricsOfSnapshot.add(INDEX_NOCL, changeBurstsValues[0]);
            metricsOfSnapshot.add(INDEX_NOCBL, changeBurstsValues[1]);
            metricsOfSnapshot.add(INDEX_TBSL, changeBurstsValues[2]);
            metricsOfSnapshot.add(INDEX_MCBL, changeBurstsValues[3]);

            BURST_SIZE = 1;

            changeBurstsValues = calculateChangeBursts(changedSequence, snapshot*SNAPSHOT_GAP + (SNAPSHOT_GAP*80)/100,
                    snapshot*SNAPSHOT_GAP + SNAPSHOT_GAP - 1);

            metricsOfSnapshot.add(INDEX_NOCCL, changeBurstsValues[1]);

            metrics.add(snapshot, metricsOfSnapshot);
        }

        return metrics;
    }

    private static int[] calculateChangeBursts(List<Boolean> changedSequence, int startIndex, int endIndex){
        int changeCount = 0;
        boolean isBurst = false;
        int numberOfChanges = 0;
        int totalBurstSize = 0;
        int maxChangeBurst = 0;
        int lastChangedIndex = 0;
        int burstSize = 0;
        int numberOfChangeBursts = 0;

        for(int index = startIndex; index <= endIndex; index++){

            if(changedSequence.get(index) == TRUE){
                numberOfChanges++;
                lastChangedIndex = index;

                if(!isBurst) {
                    changeCount++;
                    if (changeCount >= BURST_SIZE) {
                        isBurst = true;
                    }
                }

                burstSize++;
            } else {
                assert changedSequence.get(index) == FALSE;
                assert (index - lastChangedIndex) < GAP_SIZE || !isBurst;

                if((index - lastChangedIndex) == GAP_SIZE){
                    changeCount = 0;
                    if(isBurst){
                        numberOfChangeBursts++;
                        totalBurstSize += burstSize;
                        if(burstSize > maxChangeBurst){
                            maxChangeBurst = burstSize;
                        }
                    }
                    isBurst = false;
                    burstSize = 0;
                }
            }

        }

        int[] changeBurstsValues = new int[4];
        changeBurstsValues[0] = numberOfChanges;
        changeBurstsValues[1] = numberOfChangeBursts;
        changeBurstsValues[2] = totalBurstSize;
        changeBurstsValues[3] = maxChangeBurst;

        return changeBurstsValues;
    }


    /*
        If the BURST_SIZE = 1 and then the smallest burst will be like {INDEX_END_BURST}
    */
    private static List<Integer> getChangeBursts(List<Boolean> changedSequence, int startIndex, int endIndex){
        int changeCount = 0;
        boolean isBurst = false;
        int lastChangedIndex = 0;

        List<Integer> changeBursts = new ArrayList<>();
        for(int index = 0; index <= endIndex -startIndex; index++){
            changeBursts.add(index, 0);
        }

        Stack<Integer> tempBurst = new Stack<>();
        for(int index = startIndex; index <= endIndex; index++){

            if(changedSequence.get(index) == TRUE){
                lastChangedIndex = index;

                if(!isBurst) {
                    tempBurst.push(index - startIndex);
                    changeCount++;
                    if (changeCount >= BURST_SIZE) {
                        isBurst = true;
                        updateChangeBursts(changeBursts, tempBurst);
                        assert tempBurst.empty();
                    }
                } else {
                    changeBursts.add(index - startIndex, 1);
                }

            } else {
                assert changedSequence.get(index) == FALSE;
                assert (index - lastChangedIndex) < GAP_SIZE || !isBurst;

                if((index - lastChangedIndex) == GAP_SIZE){
                    changeCount = 0;
                    if(isBurst){
                        changeBursts.add(lastChangedIndex - startIndex, INDEX_END_BURST);
                    } else {
                        tempBurst.clear();
                    }
                    isBurst = false;
                }
            }

        }

        if(isBurst){
            changeBursts.add(lastChangedIndex - startIndex, INDEX_END_BURST);
        }

        return changeBursts;
    }

    private static void updateChangeBursts(List<Integer> changeBursts, Stack<Integer> tempBurst) {
        while(!tempBurst.empty()){
            if(tempBurst.size() == 1){
                changeBursts.add(tempBurst.pop(), INDEX_START_BURST);
            } else {
                changeBursts.add(tempBurst.pop(), 1);
            }
        }
    }

}
