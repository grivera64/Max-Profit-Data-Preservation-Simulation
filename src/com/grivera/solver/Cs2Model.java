package com.grivera.solver;

import com.grivera.generator.Network;
import com.grivera.generator.sensors.SensorNode;
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
    private int totalProfit;
    private List<Tuple<SensorNode, SensorNode, Integer>> flows;

    public Cs2Model(Network network) {
        this(network, ".");
    }
    public Cs2Model(Network network, String cs2Location) {
        super(network);
        this.cs2Location = cs2Location;
    }

    public Cs2Model(String fileName) {
        this(fileName, ".");
    }

    /* Note, this isn't thread safe */
    public Cs2Model(String fileName, String cs2Location) {
        super(fileName);
        this.cs2Location = cs2Location;
    }

    public Cs2Model(String fileName, String cs2Location, int overflowPackets, int storageCapacity) {
        super(fileName, overflowPackets, storageCapacity);
        this.cs2Location = cs2Location;
    }

    public void run() {
        super.run();
        File currDir = new File(this.cs2Location);
        File[] files = currDir.listFiles(f -> f.getName().startsWith("cs2."));
        assert files != null;
        if (files.length < 1) {
            throw new RuntimeException(String.format("Couldn't find CS2 program in current directory [%s]", currDir.getAbsoluteFile()));
        }

        String flowNetworkName = String.format("cs2_reader_tmp_%s", this.getDateString());

        Network network = this.getNetwork();
        network.saveAsCsInp(String.format("%s.inp", flowNetworkName));

        Path tmpFile = null;
        try {
            tmpFile = Files.createTempFile(Path.of("."), flowNetworkName, ".txt");
            new ProcessBuilder(
                    List.of("cmd", "/C",
                            String.format("(\"%s/cs2.exe\" < \"%s.inp\") > \"%s\"", this.cs2Location, flowNetworkName, tmpFile.toString())
                    )
            )
                    .directory(new File("."))
                    .start()
                    .waitFor();
            System.out.printf("Saved Cs2 Output to \"%s\"!\n", tmpFile);
        } catch (IOException | InterruptedException e) {
            System.out.println("Terminal didn't run successfully");
        }

        assert tmpFile != null;
        this.parseCs2(tmpFile.toFile());
        new File(flowNetworkName + ".inp").deleteOnExit();
        tmpFile.toFile().deleteOnExit();
    }

    private void parseCs2(File file) {
        if (!file.exists()) {
            throw new RuntimeException("Running CS2 Failed!");
        }

        String[] lineSplit;
        SensorNode tmpSrc;
        SensorNode tmpDst;
        int srcId;
        int dstId;
        int tmpFlow;

        Network network = this.getNetwork();

        this.flows = new ArrayList<>();
        try (Scanner fileScanner = new Scanner(file)) {
            while (fileScanner.hasNext()) {
                lineSplit = fileScanner.nextLine().split("\\s+");

                switch (lineSplit[0].charAt(0)) {
                    case 's':
                        this.totalProfit = -Integer.parseInt(lineSplit[1]);
                        break;
                    case 'f':
                        srcId = Integer.parseInt(lineSplit[1]);
                        dstId = Integer.parseInt(lineSplit[2]);

                        if (srcId < 1 || dstId >= network.getSensorNodes().size()) {
                            break;
                        }

                        tmpSrc = network.getSensorNodeByUuid(srcId);
                        tmpDst = network.getSensorNodeByUuid(dstId);

                        tmpFlow = Integer.parseInt(lineSplit[3]);
                        this.flows.add(Tuple.of(tmpSrc, tmpDst, tmpFlow));
                        break;
                    case 'c':
                        break;
                    default:
                        throw new RuntimeException("Invalid starting char!");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot open file!");
        }
    }

    @Override
    public int getTotalProfit() {
        return this.totalProfit;
    }

    @Override
    public int getTotalCost() {
        int totalCost = 0;

        Network network = this.getNetwork();
        for (Tuple<SensorNode, SensorNode, Integer> tuple : this.flows) {
            totalCost += network.calculateMinCost(tuple.first(), tuple.second()) * tuple.third();
        }
        return totalCost;
    }

    private String getDateString() {
        String pattern = "yyyyMMddHHmmss";
        DateFormat df = new SimpleDateFormat(pattern);
        Date today = Calendar.getInstance().getTime();
        return df.format(today);
    }
}
