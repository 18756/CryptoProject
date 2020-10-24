import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.IntStream;

public class Main {
    private static OptimizedMT optMT;
    private static final Random random = new Random();
    private static final DecimalFormat df = new DecimalFormat("#.###");
    private static final String nextDataSeparator = "===========================================================";
    private static final SortedMap<Double, Double> plotDataComplete = new TreeMap<>();
    private static final SortedMap<Double, Double> plotDataCompleteBounded = new TreeMap<>();
    private static final SortedMap<distribution, SortedMap<Double, Double>> plotDataDistributions = new TreeMap<>();
    private static final SortedMap<distribution, Pair> witSizeAndCount = new TreeMap<>();
    private static final SortedMap<distribution, Double> accSumForDist = new TreeMap<>();
    private static final List<SortedMap<Double, Double>> plotDataPartitions = new ArrayList<>(3);
    private static final SortedMap<Double, Double> plotDataAccPartitions = new TreeMap<>();

    private enum distribution {
        BLOCK,
        SPLIT,
        RANDOM
    }

    private static class Pair {
        public double a, b;

        public Pair(double a, double b) {
            this.a = a;
            this.b = b;
        }
    }

    public static void main(String[] args) {
        try {
            optMT = new OptimizedMT();
        } catch (Exception e) {
            System.out.println("Fail");
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("test_results.txt", StandardCharsets.UTF_8))) {
            test(8, bw);
        } catch (IOException e) {
            System.out.println("Fail");
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("Graph.txt", StandardCharsets.UTF_8))) {
//          COMPLETE
            printPlot(bw, plotDataComplete);
//          DISTRIBUTION
            for (Map<Double, Double> m : plotDataDistributions.values()) {
                printPlot(bw, m);
            }
//          PARTITION
            for (Map<Double, Double> m : plotDataPartitions) {
                printPlot(bw, m);
            }
//          ACC FOR X
            printPlot(bw, plotDataAccPartitions);
//          BOUNDED COMPLETE
            printPlot(bw, plotDataCompleteBounded);

        } catch (IOException e) {
            System.out.println("Fail");
        }
    }

    public static void printPlot(BufferedWriter bw, Map<Double, Double> m) throws IOException {
        bw.write(m.size() + "\n");
        for (Double x : m.keySet()) {
            bw.write(df.format(x) + " ");
        }
        bw.newLine();
        for (Double y : m.values()) {
            bw.write(df.format(y) + " ");
        }
        bw.newLine();
    }

    public static List<String> generateData(int size) {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            data.add(gen(i));
        }
        return data;
    }

    public static List<String> formDist(List<String> data, int size, distribution d) {
        List<String> answer = new ArrayList<>();
        switch (d) {
            case BLOCK:
                int left = (int) (random.nextFloat() * (data.size() - 1));
                int right = Math.min(left + size, data.size());
                IntStream.range(left, right).forEach(i -> answer.add(data.get(i)));
                size -= (right - left);
                if (size > 0) {
                    IntStream.range(0, size).forEach(i -> answer.add(data.get(i)));
                }
                break;
            case SPLIT:
                int pos = (int) (random.nextFloat() * (data.size() - 1));
                int step = data.size() / size;
                while (size > 0) {
                    answer.add(data.get(pos));
                    pos = (pos + step) % data.size();
                    size -= 1;
                }
                break;
            case RANDOM:
                Collections.shuffle(data);
                for (int i = 0; i < size; ++i) {
                    answer.add(data.get(i));
                }
                break;
        }
        return answer;
    }

    public static void test(int numOfTests, BufferedWriter bw) throws IOException {
        for (distribution dist : distribution.values()) {
            plotDataDistributions.put(dist, new TreeMap<>());
        }

        for (int r = 0; r < 3; ++r) {
            plotDataPartitions.add(new TreeMap<>());
        }

        long startTime;
        int[] heights = {4, 8, 12};
        int[][] sizesOfX = new int[heights.length][];
        for (int i = 0; i < heights.length; ++i) {
            int size = (int) Math.pow(2, heights[i]);

            int k = 50;
            sizesOfX[i] = new int[k + 1];
            sizesOfX[i][0] = 1;
            for (int q = 0; q < k; ++q) {
                sizesOfX[i][1 + q] = Math.max(1, (int) (size / (double) k * (q + 1)));
            }
//            sizesOfX[i] = new int[20];
//            for (int t = 0; t < 20; ++t) {
//                sizesOfX[i][t] = Math.max(1, (int) Math.pow(t, 3));
//            }
        }


        boolean verdicts = true;
        for (int i = 0; i < heights.length; ++i) {
            System.out.println("testing " + (i + 1) + "-th of " + heights.length);

            int h = heights[i];
            int size = (int) Math.pow(2, h);
            boolean f = true;

            for (int n : sizesOfX[i]) {
                for (distribution dist : distribution.values()) {
                    witSizeAndCount.put(dist, new Pair(0., 0.));
                }
                for (distribution dist : distribution.values()) {
                    accSumForDist.put(dist, 0.);
                }

                System.out.println("For subset of " + n + " elements");
                n = Math.min(size, n);
                List<String> X;

                double setUpTime = 0.;
                Map<distribution, Double> times = new HashMap<>();
                for (distribution d : distribution.values()) {
                    times.put(d, 0.);
                }

                int witSize = 0;
                int witCount = 0;
                long accSize = 0;
                for (int k = 0; k < numOfTests; ++k) {
                    List<String> data = generateData(size);

                    //Time to setUp MT for data of given size
                    startTime = System.nanoTime();
                    {
                        optMT.setUp(new HashSet<>(data));
                    }
                    setUpTime += System.nanoTime() - startTime;

                    for (distribution dist : distribution.values()) {
                        int curWitSize = 0;
                        int curWitCount = 0;


                        X = formDist(data, n, dist);
                        //Time to generate Acc for n elements
                        startTime = System.nanoTime();
                        OptimizedMT.Accumulator accX;
                        {
                            accX = optMT.accumulate(new HashSet<>(X));
                            accSize += accX.mem.size() + accX.notMem.size();
                        }
                        times.put(dist, times.get(dist) + System.nanoTime() - startTime);

                        //Getting witnesses and verifying them
                        List<String> R = new ArrayList<>();
                        for (String st : data) {
                            if (!X.contains(st)) {
                                R.add(st);
                            }
                        }
                        List<String> nonMems;
                        List<String> mems;

                        Collections.shuffle(R);
                        Collections.shuffle(X);
                        nonMems = R;
                        mems = X;
                        curWitCount = nonMems.size() + mems.size();
                        witCount += curWitCount;

                        for (String s : nonMems) {
                            List<OptimizedMT.Sibling> wit = optMT.witGen(s, accX);
                            witSize += wit.size();
                            curWitSize += wit.size();
                            Boolean verdict = optMT.verify(s, wit, accX);
                            verdicts &= !((verdict == null) || (!verdict.equals(false)));
                        }
                        for (String s : mems) {
                            List<OptimizedMT.Sibling> wit = optMT.witGen(s, accX);
                            witSize += wit.size();
                            curWitSize += wit.size();
                            Boolean verdict = optMT.verify(s, wit, accX);
                            verdicts &= !((verdict == null) || (!verdict.equals(true)));
                        }
                        if (i == heights.length - 1) {
                            witSizeAndCount.get(dist).a += curWitSize;
                            witSizeAndCount.get(dist).b += curWitCount;
                            accSumForDist.put(dist, accSumForDist.get(dist) + (double) accX.mem.size() + accX.notMem.size());
                        }
                    }
                }
                if (i == heights.length - 1) {
                    for (distribution dist : distribution.values()) {
                        plotDataDistributions.get(dist).put((witSizeAndCount.get(dist).a / (double) witSizeAndCount.get(dist).b) / h, (double) accSumForDist.get(dist) / numOfTests);
                    }
                }
                if (f) {
                    bw.write("Time to SetUp MT for " + size + " elements : " + df.format(setUpTime / numOfTests / 1_000_000_000.) + " s\n");
                    f = false;
                }
                bw.write("******\n");
                for (distribution dist : distribution.values()) {
                    bw.write("Time to generate Acc for (" + n + ")"
                            + df.format((n * 100. / size)) + "% " + dist
                            + " elements : "
                            + df.format((times.get(dist) / numOfTests) / 1_000_000_000.)
                            + " s\n");
                }

                bw.write("Accumulator size : " + df.format(accSize / (double) (numOfTests * distribution.values().length)) + "\n");

                bw.write("\nAvg witness size via Subset Difference : " + df.format(witSize / (double) witCount) + "\n");
                bw.write("Default MT witness size for this case : " + h + "\n");


//                GRAPH
                plotDataPartitions.get(i).put(n / (double) size, witSize / (double) witCount / h);
                if (i == heights.length - 1) {
                    plotDataAccPartitions.put(n / (double) size, accSize / (double) (numOfTests * distribution.values().length));
                    plotDataComplete.put((witSize / (double) witCount) / h, accSize / (double) (numOfTests * distribution.values().length));
                    if (n / (double) size < 0.6) {
                        plotDataCompleteBounded.put((witSize / (double) witCount) / h, accSize / (double) (numOfTests * distribution.values().length));
                    }
                }
            }
            bw.write(nextDataSeparator + "\n");
        }
        bw.write("Correctness : " + (verdicts ? "OK" : "FAIL"));
    }

    public static void draw() {

    }

    public static String gen(Integer num) {
        int leftLimit = 0x0; // skip control symbols
        int rightLimit = 0xFF; // skip control symbols
        int targetStringLength = 16;

        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
//            if (randomLimitedInt >= 0x7F) {
//                randomLimitedInt++;
//            }
            buffer.append((byte) randomLimitedInt);
        }
        buffer.append(num.toString());
        return buffer.toString();
    }
}
