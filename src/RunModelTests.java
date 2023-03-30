//import com.grivera.generator.sensors.SensorNode;
import com.grivera.solver.Model;
import com.grivera.solver.PMPGreedyModel;

public class RunModelTests {
    public static void main(String[] args) {
//        SensorNode.setBitsPerPacket(512 * 8);
        Model model = new PMPGreedyModel("figure_3_sensor_network.sn");
        model.run();
        System.out.printf("Cost: %d \u00b5J\n", model.getTotalCost());
        System.out.printf("Profit: %d \u00b5J\n", model.getTotalProfit());
    }
}
