package com.grivera.solver;

import com.grivera.generator.Network;
import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.sensors.StorageNode;
import com.grivera.util.Tuple;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class Cs2Model extends AbstractModel {

    private final String cs2Location;
    private int totalCost;
    private List<Tuple<DataNode, StorageNode, Integer>> flows;

    public Cs2Model(Network network) {
        this(network, ".");
    }
    public Cs2Model(Network network, String cs2Location) {
        super(network);
        this.cs2Location = cs2Location;
        this.verifyCs2();
    }

    public Cs2Model(String fileName) {
        this(fileName, ".");
    }

    /* Note, this isn't thread safe */
    public Cs2Model(String fileName, String cs2Location) {
        super(fileName);
        this.cs2Location = cs2Location;
        this.verifyCs2();
    }

    public Cs2Model(String fileName, String cs2Location, int overflowPackets, int storageCapacity) {
        super(fileName, overflowPackets, storageCapacity);
        this.cs2Location = cs2Location;
        this.verifyCs2();
    }

    private void verifyCs2() {
        File currDir = new File(this.cs2Location);
        File[] files = currDir.listFiles(f -> f.getName().matches("^cs2(.exe)?$"));
        if (files == null || files.length < 1) {
            throw new IllegalArgumentException(
                    String.format("Couldn't find CS2 program [Searched Dir: \"%s\"]", currDir.getAbsoluteFile())
            );
        }
    }

    public void run(int episodes) {
        System.out.println("Warning: Ignoring episodes count; defaulting to 1...");
        this.run();
    }

    public void run() {
        super.run();

        String baseFileName = String.format("cs2_tmp_%s", this.getDateString());
        String tmpInpName = String.format("%s.inp", baseFileName);

        Network network = this.getNetwork();
        network.saveAsCsInp(tmpInpName);

        String cs2FullPath = new File(this.cs2Location).getAbsolutePath();
        Path tmpTxt;
        String tmpTxtName;
        try {
            tmpTxt = Files.createTempFile(Path.of("."), baseFileName, ".txt");
            tmpTxtName = tmpTxt.toString();
            String osName = System.getProperty("os.name");
            String mainCommand = String.format("(\"%s/cs2\" < \"%s\") > \"%s\"", cs2FullPath, tmpInpName, tmpTxtName);

            List<String> osCommand;
            if (osName.startsWith("Windows")) {
                osCommand = List.of("cmd", "/C", mainCommand.replace("/", "\\"));
            } else if (osName.startsWith("Mac OS")) {
                osCommand = List.of("/bin/zsh", "-c", mainCommand);
            } else {
                osCommand = List.of("/bin/bash", "-c", mainCommand);
            }

            new ProcessBuilder(osCommand)
                    .directory(new File("."))
                    .start()
                    .waitFor();

            this.parseCs2(tmpTxt.toFile());

            /* Clear the .inp and .txt files after no longer needed */
            tmpTxt.toFile().delete();
        } catch (IOException | InterruptedException e) {
            System.err.printf("ERROR: Terminal not supported for '%s'!\n", System.getProperty("os.name"));
        }
        new File(tmpInpName).delete();
    }

    private void parseCs2(File file) {
        if (!file.exists()) {
            throw new RuntimeException("Running CS2 Failed!");
        }

        String[] lineSplit;
        DataNode tmpSrc;
        StorageNode tmpDst;
        int srcId;
        int dstId;
        int tmpFlow;

        Network network = this.getNetwork();

        this.flows = new ArrayList<>();
        try (Scanner fileScanner = new Scanner(file)) {
            if (!fileScanner.hasNext()) {
                System.err.println("WARNING: EMPTY FILE!");
            }
            while (fileScanner.hasNext()) {
                lineSplit = fileScanner.nextLine().split("\\s+");

                switch (lineSplit[0].charAt(0)) {
                    case 's':
                        this.totalCost = Integer.parseInt(lineSplit[1]);
                        break;
                    case 'f':
                        srcId = Integer.parseInt(lineSplit[1]);
                        dstId = Integer.parseInt(lineSplit[2]);

                        if (srcId < 1 || dstId > network.getDataNodeCount() + network.getStorageNodeCount()) {
                            break;
                        }

                        tmpSrc = network.getDataNodeById(srcId);
                        tmpDst = network.getStorageNodeById(dstId - network.getDataNodeCount());

                        tmpFlow = Integer.parseInt(lineSplit[3]);
                        this.flows.add(Tuple.of(tmpSrc, tmpDst, tmpFlow));
                        break;
                    case 'c':
                        break;
                    default:
                        System.err.printf("WARNING: Invalid command '%s' found! Skipping...\n", lineSplit[0]);
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot open file!");
        }
    }

    @Override
    public int getTotalProfit() {
        int totalProfit = 0;

        Network network = this.getNetwork();
        for (Tuple<DataNode, StorageNode, Integer> tuple : this.flows) {
            totalProfit += network.calculateProfitOf(tuple.first(), tuple.second()) * tuple.third();
        }
        return totalProfit;
    }

    @Override
    public void printRoute() {
        StringJoiner str;
        Network network = this.getNetwork();
        for (Tuple<DataNode, StorageNode, Integer> tuple : this.flows) {
            if (tuple.third() > 0) {
                System.out.printf("%s -> %s (flow = %d)\n", tuple.first().getName(), tuple.second().getName(), tuple.third());

                str = new StringJoiner(" -> ", "[", "]");
                for (SensorNode n : network.getMinCostPath(tuple.first(), tuple.second())) {
                    str.add(n.getName());
                }
                System.out.printf("\t%s\n", str);
            }
        }
    }

    @Override
    public int getTotalCost() {
        return this.totalCost;
    }

    private String getDateString() {
        String pattern = "yyyyMMddHHmmss";
        DateFormat df = new SimpleDateFormat(pattern);
        Date today = Calendar.getInstance().getTime();
        return df.format(today);
    }
}
