package trucksimulation.trucks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.graphhopper.util.Instruction;
import com.graphhopper.util.PointList;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.routing.RouteSegment;
import trucksimulation.routing.TargetExceededException;
import trucksimulation.traffic.TrafficIncident;

public class Truck {
	
	private String id;
	private Route route;
	private String routeId;
	private TelemetryBox telemetryBox = new TelemetryBox();
	private TelemetryData data;
	private List<TrafficIncident> incidents = new ArrayList<>();
	/** points to the current traffic incident if the truck is affected by one. null otherwise. */
	private TrafficIncident curIncident = null;
	
	private double speed = 5.0;
	private Position pos;
	private Position targetPos;
	private int interval = 1;
	private long ts = 0;
	
	private int curRouteSegment = 0;
	private int curSegmentPoint = 0;
	
	private static List<Truck> trucks = new ArrayList<>();
	private static long nextTruckId = 100;
	
	public Truck(String id) {
		this.id = id;
	}
	
	public static Truck buildTruck() {
		Truck t = new Truck("truck " + nextTruckId++);
		trucks.add(t);
		return t;
	}
	
	public void move(double moveSpeed) {
		if(pos.equals(targetPos)) {
			throw new DestinationArrivedException("Already arrived at destination. Set a new destination / route.");
		}
		try {
			pos = pos.moveTowards(targetPos, moveSpeed * interval);
		} catch (TargetExceededException e) {
			pos = targetPos;
			proceedToNextPoint();
			move(e.getExceededBy());
		}
		ts += interval * 1000;
		data = telemetryBox.update(pos, ts);
		updateTrafficMode();
	}
	
	/**
	 * Moves the truck forward on its assigned route.
	 * 
	 * Throws a DestinationArrivedException if the truck is already at the last point of the route.
	 */
	public void move() {
		move(speed);	
	}
	
	public JsonObject asGeoJsonFeature() {
		return pos.asGeoJsonFeature();
	}
	
	private void proceedToNextPoint() {	
		RouteSegment currentSegment = route.getSegment(curRouteSegment);
		if(currentSegment.getSize() > curSegmentPoint + 1) {
			curSegmentPoint++;
			targetPos = currentSegment.getPoint(curSegmentPoint);
		} else {
			curRouteSegment++;
			curSegmentPoint = 0;
			if(curRouteSegment == route.getSegmentCount() -1) {
				currentSegment = route.getSegment(curRouteSegment);
				targetPos = currentSegment.getPoint(curSegmentPoint);
			} else if(curRouteSegment < route.getSegmentCount()) {
				currentSegment = route.getSegment(curRouteSegment);
				targetPos = currentSegment.getPoint(curSegmentPoint);
				double nextSpeed = currentSegment.getSpeed();
				if(nextSpeed > 0 && curIncident == null) {
					speed = nextSpeed;
				}
			} else {
				throw new DestinationArrivedException("Truck has reached its target. Please assign a new route before proceeding.");
			}
		}
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Route getRoute() {
		return route;
	}
	public void setRoute(Route route) {
		this.route = route;
		this.pos = route.getStart();
		this.proceedToNextPoint();
	}
	public TelemetryData getData() {
		return data;
	}
	public void setData(TelemetryData data) {
		this.data = data;
	}
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public Position getPos() {
		return pos;
	}

	public void setPos(Position pos) {
		this.pos = pos;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public static List<Truck> getTrucks() {
		return trucks;
	}

	public static void setTrucks(List<Truck> trucks) {
		Truck.trucks = trucks;
	}

	public static long getNextTruckId() {
		return nextTruckId;
	}

	public static void setNextTruckId(long nextTruckId) {
		Truck.nextTruckId = nextTruckId;
	}

	public JsonObject getJsonData() {
		JsonObject msg = new JsonObject();
		msg.put("position", data.getPosition().asGeoJsonFeature());
		msg.put("speed", data.getSpeed());
		msg.put("ts", data.getTimeStamp());
		msg.put("horizontalAccuracy", data.getHorizontalAccuracy());
		msg.put("truckId", id);
		msg.put("bearing", data.getBearing());
		return msg;
	}
	
	

	 /**
	 * Detects if the truck is entering or leaving a traffic incident.
	 * Uses a simple check for the current distance to the traffic incidents start and end point.
	 * It is assumed that only traffic incidents have been assigned to the truck that are actually
	 * on the trucks route. 
	  */
	private void updateTrafficMode() {
		Iterator<TrafficIncident> iter = incidents.iterator();
		while(iter.hasNext()) {
			TrafficIncident incident = iter.next();
			double distToStart = pos.getDistance(incident.getStart());
			double distToend = pos.getDistance(incident.getEnd());
			
			if(distToStart < speed * interval) {
				enterTraffic(incident);
			}
			if(distToend < speed * interval) {
				leaveTraffic(incident);
				iter.remove();
			}
		}
	}
	
	private void enterTraffic(TrafficIncident incident) {
		if(curIncident != null && !curIncident.equals(incident)) {
			throw new IllegalStateException("truck is already in a traffic incident. This is likely a bug in the updateTrafficMode method.");
		}
		this.speed = incident.getSpeed();
		curIncident = incident;
	}
	
	private void leaveTraffic(TrafficIncident incident) {
		this.speed = route.getSegment(curRouteSegment).getSpeed();
		curIncident = null;
	}

	public TrafficIncident getCurIncident() {
		return curIncident;
	}

	public void setCurIncident(TrafficIncident curIncident) {
		this.curIncident = curIncident;
	}

	public List<TrafficIncident> getTrafficIncidents() {
		return incidents;
	}
	
	public void addTrafficIncident(TrafficIncident incident) {
		if(incident == null) {
			throw new IllegalArgumentException("must not be null");
		}
		this.incidents.add(incident);
	}
	
	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public boolean hasArrived() {
		return this.pos.equals(route.getGoal());
	}
	
	
	

}
