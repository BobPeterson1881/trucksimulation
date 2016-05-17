package trucksimulation;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import trucksimulation.routing.RouteCalculationVerticle;

public class StarterVerticle extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(StarterVerticle.class);

	@Override
	public void start() {

		DeploymentOptions routeMgrOptions = new DeploymentOptions().setWorker(true).setConfig(config());
		DeploymentOptions deplOptions = new DeploymentOptions().setConfig(config());

		vertx.deployVerticle(new RouteCalculationVerticle(), routeMgrOptions, w -> {
			if (w.failed()) {
				LOGGER.error("Deployment of RouteManager failed." + w.cause());
			}
			vertx.deployVerticle(new TruckControllerVerticle(), deplOptions, e -> {
				if (e.failed()) {
					LOGGER.error("Deployment of TruckCOntroller failed." + e.cause());
				}
			});
			vertx.deployVerticle(new BootstrapVerticle(), deplOptions, d -> {
				if (d.failed()) {
					LOGGER.error("Deployment of BootstrapVerticle failed." + d.cause());
				}
			});
		});

		vertx.deployVerticle(new Server(), deplOptions, e -> {
			if (e.failed()) {
				LOGGER.error("Deployment of server failed. " + e.cause());
			}
		});
	}
}
