/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.init;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.statistics.StatisticEvent;
import cz.cvut.fel.aic.amodsim.statistics.Statistics;

/**
 * @author fido
 */
@Singleton
public class StatisticInitializer {

	private final Statistics statistics;

	private final EventProcessor eventProcessor;


	@Inject
	public StatisticInitializer(Statistics statistics, EventProcessor eventProcessor) {
		this.statistics = statistics;
		this.eventProcessor = eventProcessor;
	}


	public void initialize() {
		eventProcessor.addEvent(StatisticEvent.TICK, statistics, null, null);
	}
}
