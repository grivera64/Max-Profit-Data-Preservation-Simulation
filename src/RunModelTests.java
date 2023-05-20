import com.grivera.solver.Cs2Model;
import com.grivera.generator.Network;
import com.grivera.generator.SensorNetwork;
import com.grivera.solver.Model;
import com.grivera.solver.PMPGreedyModel;
import com.grivera.solver.PMPMarl;

import java.util.Scanner;

public class RunModelTests {

    private static final Scanner keyboard = new Scanner(System.in);
    public static void main(String[] args) {
        System.out.println("Welcome to the Max Profit Data Preservation Simulator!");
        System.out.println("===========================================");
        System.out.println();

        System.out.print("Please enter an option: (G)enerate/(F)ile/(Q)uit:\n> ");
        String option = keyboard.nextLine();

        if (option.isBlank() || option.isEmpty()) {
            option = "Q";
        }

        Network network = null;
        switch (option.charAt(0)) {
            case 'F', 'f' ->  network = readNetwork();
            case 'G', 'g' -> {
                network = generateNetwork();
                System.out.print("Please name the network:\n(\"network\".sn) > ");
                String networkName = keyboard.nextLine();
                if (!networkName.matches("^[A-za-z0-9._-][A-za-z0-9._-]{0,255}$")) {
                    networkName = "network";
                }
                network.save(String.format("%s.sn", networkName));
            }
            default -> {
                System.out.println("Thank you for using Max-Profit-Data-Preservation-Simulator!");
                System.exit(0);
            }
        }
        System.out.println();

        System.out.print("Where is your installation of cs2.exe located?\n(\".\") > ");
        String cs2Location = keyboard.nextLine();
        if (cs2Location.isEmpty()) {
            cs2Location = ".";
        } else if (cs2Location.matches("\\$[A-Za-z_][A-Za-z0-9_]*")) {
            cs2Location = System.getenv(cs2Location.substring(1));
        } else if (cs2Location.matches("%[A-Za-z_][A-Za-z0-9_]*%")) {
            cs2Location = System.getenv(cs2Location.substring(1, cs2Location.length() - 1));
        }
        System.out.println();

        System.out.print("How many episodes should MARL run?\n(100) > ");
        String episodesString = keyboard.nextLine();
        int episodes = 100;
        if (episodesString.matches("\\d+")) {
            episodes = Integer.parseInt(episodesString);
        }
        System.out.println();

        System.out.println("Running models...");
        System.out.println("=================");

        Model model = new PMPGreedyModel(network);
        model.run();
        System.out.println("Greedy:");
        System.out.printf("Cost: %d \u00b5J\n", model.getTotalCost());
        System.out.printf("Profit: %d \u00b5J\n", model.getTotalProfit());
        System.out.println();
        model.printRoute();
        System.out.println();

        try {
            System.out.println("CS2 (Optimal):");
            model = new Cs2Model(network, cs2Location);
            model.run();
            System.out.printf("Cost: %d \u00b5J\n", model.getTotalCost());
            System.out.printf("Profit: %d \u00b5J\n", model.getTotalProfit());
            System.out.println();
            model.printRoute();
        } catch (IllegalArgumentException e) {
            System.out.printf("WARNING: %s\n", e.getMessage());
            System.out.println("Skipping Cs2Model...");
        } finally {
            System.out.println();
        }

        System.out.printf("MARL (%d episodes):\n", episodes);
        model = new PMPMarl(network);
        model.run(episodes);
        System.out.printf("Cost: %d \u00b5J\n", model.getTotalCost());
        System.out.printf("Profit: %d \u00b5J\n", model.getTotalProfit());
        System.out.println();
        model.printRoute();
        System.out.println();
    }

    public static Network readNetwork() {
        System.out.print("Please enter the file name:\nF > ");
        String fileName = keyboard.nextLine().trim();
        return SensorNetwork.from(fileName);
    }

    public static Network generateNetwork() {
        System.out.println("Please enter the width (x) of the sensor network:");
        System.out.print("x = ");
        double width = keyboard.nextDouble();
        keyboard.nextLine();

        System.out.println("Please enter the height (y) of the sensor network: ");
        System.out.print("y = ");
        double height = keyboard.nextDouble();
        keyboard.nextLine();

        System.out.println("Please enter the number of sensor nodes (N) to generate in the sensor network:");
        System.out.print("N = ");
        int nodeCount = keyboard.nextInt();
        keyboard.nextLine();

        System.out.println("Please enter the number the transmission range (Tr) in meters:");
        System.out.print("Tr = ");
        double transmissionRange = keyboard.nextDouble();
        keyboard.nextLine();

        System.out.println("Please enter the number of Data Nodes (p) to generate:");
        System.out.print("p = ");
        int gNodeCount = keyboard.nextInt();
        keyboard.nextLine();

        System.out.println("Please enter the number of data packets (q) each Data Node has:");
        System.out.print("q = ");
        int packetsCount = keyboard.nextInt();
        keyboard.nextLine();

        System.out.println("Please enter the number of Storage Nodes (s) to generate:");
        System.out.print("s = ");
        int sNodeCount = keyboard.nextInt();
        keyboard.nextLine();

        System.out.println("Please enter the amount of packets (m) each Storage Node has:");
        System.out.print("m = ");
        int storageCount = keyboard.nextInt();
        keyboard.nextLine();

        System.out.println("Please enter the min value (Vl) that any data packet can have:");
        System.out.print("Vl = ");
        int lowestValue = keyboard.nextInt();
        keyboard.nextLine();

        System.out.println("Please enter the max value (Vh) that any data packet can have:");
        System.out.print("Vh = ");
        int highestValue = keyboard.nextInt();
        keyboard.nextLine();

        return SensorNetwork.of(
                width, height, nodeCount, transmissionRange, gNodeCount, packetsCount, sNodeCount, storageCount,
                lowestValue, highestValue
        );
    }
}
