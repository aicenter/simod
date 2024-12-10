package cz.cvut.fel.aic.simod;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.DateTimeParser;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultPlanComputationRequestTest {

	void testMaxDropOffTimeComputation(
		int maxPickupDelay,
		String desiredPickupTimeStr,
		long minTravelTimeMs,
		long maxProlongationMs,
		String expectedMaxDropOffTimeStr
	) {
		SimodConfig config = new SimodConfig();
		config.maxPickupDelay = maxPickupDelay;
		ZonedDateTime desiredPickupTime = DateTimeParser.parseDateTime(desiredPickupTimeStr);

		ZonedDateTime maxDropOffTime = DefaultPlanComputationRequest.getMaxDropOffTime(
			config,
			desiredPickupTime,
			minTravelTimeMs,
			maxProlongationMs
		);

		ZonedDateTime expectedMaxDropOffTime = DateTimeParser.parseDateTime(expectedMaxDropOffTimeStr);

		assertEquals(expectedMaxDropOffTime, maxDropOffTime);
	}

	@Test
	void testMaxDropOffTimeComputation() {
		testMaxDropOffTimeComputation(900, "2023-08-17 17:30:00", 1_131_229, 1_131_229, "2023-08-17 18:22:43");
	}

	@Test
	void testMaxDropOffTimeComputation2() {
		testMaxDropOffTimeComputation(900, "2023-08-17 11:30:00", 941_899, 941_899, "2023-08-17 12:16:24");
	}

}
