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
        File[] files = currDir.listFiles(f -> f.getName().startsWith("cs2."));
        if (files == null || files.length < 1) {
            throw new IllegalArgumentException(
                    String.format("Couldn't find CS2 program [Searched Dir: \"%s\"]", currDir.getAbsoluteFile())
            );
        }
    }

    public void run() {
        super.run();

        String baseFileName = String.format("cs2_model_%s", this.getDateString());
        String tmpInpName = String.format("%s.inp", baseFileName);

        Network network = this.getNetwork();
        network.saveAsCsInp(tmpInpName);

        String cs2FullPath = new File(this.cs2Location).getAbsolutePath();
        Path tmpTxt = null;
        String tmpTxtName;
        try {
            tmpTxt = Files.createTempFile(Path.of("."), baseFileName, ".txt");
            tmpTxtName = tmpTxt.toString();
            String osName = System.getProperty("os.name");
            String mainCommand = String.format("(\"%s/cs2.exe\" < \"%s\") > \"%s\"", cs2FullPath, tmpInpName, tmpTxtName);

            List<String> osCommand;
            if (osName.startsWith("Windows")) {
                osCommand = List.of("cmd", "/C", mainCommand);
            } else if (osName.startsWith("Mac OS")) {
                osCommand = List.of("/bin/zsh", mainCommand);
            } else {
                osCommand = List.of("sh", mainCommand);
            }

            new ProcessBuilder(osCommand)
                    .directory(new File("."))
                    .start()
                    .waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Terminal didn't run successfully");
        }

        assert tmpTxt != null;
        this.parseCs2(tmpTxt.toFile());

        /* Clear the .inp and .txt files after no longer needed */
        new File(tmpInpName).delete();
        tmpTxt.toFile().delete();
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
