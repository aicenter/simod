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
package cz.cvut.fel.aic.simod.tripUtil;

/**
 *
 * @author fido
 */
public class StartTargetNodePair {
	private final int startNodeId;
	
	private final int targetNodeId;

	
	
	
	public int getStartNodeId() {
		return startNodeId;
	}

	public int getTargetNodeId() {
		return targetNodeId;
	}
	
	
	
	
	public StartTargetNodePair(int startNodeId, int targetNodeId) {
		this.startNodeId = startNodeId;
		this.targetNodeId = targetNodeId;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 59 * hash + this.startNodeId;
		hash = 59 * hash + this.targetNodeId;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final StartTargetNodePair other = (StartTargetNodePair) obj;
		if (this.startNodeId != other.startNodeId) {
			return false;
		}
		if (this.targetNodeId != other.targetNodeId) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return startNodeId + "-" + targetNodeId;
	}
	
	
	
	
}
