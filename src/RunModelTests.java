//import com.grivera.generator.sensors.SensorNode;
import com.grivera.solver.Cs2Model;
import com.grivera.generator.Network;
import com.grivera.generator.SensorNetwork;
import com.grivera.solver.Cs2Model;
import com.grivera.solver.Model;
import com.grivera.solver.PMPGreedyModel;

import java.util.Scanner;

public class RunModelTests {

    private static final Scanner keyboard = new Scanner(System.in);
    public static void main(String[] args) {
        System.out.println("Welcome to the Max Profit Data Preservation Simulator!");
        System.out.println("===========================================");

        System.out.print("Please enter an option: (G)enerate/(F)ile/(Q)uit:\n> ");
        int option = keyboard.nextLine().charAt(0);

        Network network = null;
        switch (option) {
            case 'F', 'f' ->  network = readNetwork();
            case 'G', 'g' -> {
                network = generateNetwork();
                network.save("network.sn");
            }
            default -> {
                System.out.println("Thank you for using Max-Profit-Data-Preservation-Simulator!");
                System.exit(0);
            }
        }
        System.out.println();

        System.out.println("Running models...");
        System.out.println("=================");

        Model model = new PMPGreedyModel(network);
        model.run();
        System.out.println("Greedy:");
        System.out.printf("Cost: %d \u00b5J\n", model.getTotalCost());
        System.out.printf("Profit: %d \u00b5J\n", model.getTotalProfit());
//        Cs2Model model2 = new Cs2Model("figure_3_sensor_network.sn");
//        System.out.println(model2.getTotalCost());
        System.out.println();

        model = new Cs2Model(network);
        model.run();
        System.out.println("CS2 (Optimal):");
        System.out.printf("Cost: %d \u00b5J\n", model.getTotalCost());
        System.out.printf("Profit: %d \u00b5J\n", model.getTotalProfit());
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
