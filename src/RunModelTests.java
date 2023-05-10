//import com.grivera.generator.sensors.SensorNode;
import com.grivera.solver.Model;
import com.grivera.solver.PMPGreedyModel;
import com.grivera.solver.PMPMarl;

public class RunModelTests {
    public static void main(String[] args) {
//        SensorNode.setBitsPerPacket(512 * 8);
        Model model = new PMPGreedyModel("figure_3_sensor_network.sn");
        //Model model = new PMPGreedyModel("og_network_01.sn");
        model.run();
        System.out.printf("Cost: %d \u00b5J\n", model.getTotalCost());
        System.out.printf("Profit: %d \u00b5J\n", model.getTotalProfit());

        //entry point for MARL

        PMPMarl marl = new PMPMarl("figure_3_sensor_network.sn");
        //PMPMarl marl = new PMPMarl("og_network_01.sn");
        marl.pmpMarl(100);
    }
}
