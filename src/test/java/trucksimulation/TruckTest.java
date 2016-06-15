package trucksimulation;

import java.io.File;

import org.junit.Test;

import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.trucks.DestinationArrivedException;
import trucksimulation.trucks.Truck;

public class TruckTest {

	@Test
	public void testMovement() throws InterruptedException {
		// use different graphhopper cache directory to avoid conflicts
		String userHome = System.getProperty("user.home");
		String ghCacheLocation = new File(userHome, ".graphhopper-test").getAbsolutePath();
		
		Truck t1 = Truck.buildTruck();
		Route r = Route.getRoute(new File("osm", "denmark-latest.osm.pbf").toString(), ghCacheLocation,
				new Position(54.939615, 8.864417), new Position(55.495973, 9.473052));
		t1.setRoute(r);
		
		long journeyTime = 0;
		
		while(true) {
			try {
				t1.move();
				System.out.println("new pos: " + t1.getPos().toString());
				Thread.sleep(1);
				journeyTime++;
			} catch (DestinationArrivedException ex) {
				break;
			}
		}
		System.out.println("Journey took " + journeyTime + " seconds.");
		System.out.println("estimated journey time was " + t1.getRoute().getTimeMs()/1000);
	}

}
