/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.init;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.statistics.StatisticEvent;
import cz.cvut.fel.aic.simod.statistics.Statistics;

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
