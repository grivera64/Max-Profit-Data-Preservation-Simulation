//import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.SensorNetwork;
import com.grivera.solver.Model;
import com.grivera.solver.PMPGreedyModel;
import com.grivera.solver.PMPMarl;

public class RunModelTests {
    public static void main(String[] args) {
//        SensorNode.setBitsPerPacket(512 * 8);
        Model model = new PMPGreedyModel("figure_3_sensor_network.sn");
        model.run();
        System.out.printf("Cost: %d \u00b5J\n", model.getTotalCost());
        System.out.printf("Profit: %d \u00b5J\n", model.getTotalProfit());

        //entry point for MARL
//        PMPMarl model2 = new PMPMarl(SensorNetwork.of(100, 100, 20, 30, 5, 1, 5, 1, 0, 6000));
//        model2.run(1); // Infinite Loop found here!
    }
}
