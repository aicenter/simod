/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
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
package cz.cvut.fel.aic.simod.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import cz.cvut.fel.aic.simod.tripUtil.StartTargetNodePair;
import java.io.IOException;

/**
 *
 * @author fido
 */
public class StartTargetNodePairSerializer extends JsonSerializer<StartTargetNodePair>{

	@Override
	public void serialize(StartTargetNodePair startTargetNodePair, JsonGenerator jg, SerializerProvider sp) 
			throws IOException, JsonProcessingException {
		jg.writeFieldName(Integer.toString(startTargetNodePair.getStartNodeId()) + "-"
				+ Integer.toString(startTargetNodePair.getTargetNodeId()));
	}
	
}
