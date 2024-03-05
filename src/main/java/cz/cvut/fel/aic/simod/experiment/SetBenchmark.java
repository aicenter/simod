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
package cz.cvut.fel.aic.simod.experiment;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
import cz.cvut.fel.aic.simod.entity.agent.DemandAgent;
import cz.cvut.fel.aic.simod.entity.vehicle.SlotType;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.simod.PlanComputationRequest;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author david
 */
public class SetBenchmark {
	
	private final int setSize;
	
	private final int callCount;
	
	private final int innerSetSize;
	
	private final Random rand;

	public SetBenchmark(int setSize, int callCount, int innerSetSize) {
		this.setSize = setSize;
		this.callCount = callCount;
		this.innerSetSize = innerSetSize;
		rand = new Random();
	}
	
	
	
	public static void main(String[] args) {
		new SetBenchmark(100_000, 10_000_000, 5).run();
	}
	
	private void run(){
		
//		Set<Integer> set = Benchmark.measureTime(() -> this.createIntegerSet());
		Benchmark benchmark = new Benchmark();
		Set<Set<Integer>> set = benchmark.measureTime(() -> this.createIntegerMultiset());
//		Set<GroupData> set = Benchmark.measureTime(() -> this.createGroupDataSet());
//		Set<BigInteger> set = Benchmark.measureTime(() -> this.createBigIntegerSet());
		
		int creationTime = benchmark.getDurationMsInt();
		
//		Benchmark.measureTime(() -> this.integerSetTest(set));
		benchmark = new Benchmark();
		benchmark.measureTime(() -> this.createIntegerMultiset());
//		Benchmark.measureTime(() -> this.groupDataTest(set));
//		Benchmark.measureTime(() -> this.bigIntegerSetTest(set));
		
		int time = benchmark.getDurationMsInt();
		System.out.println(String.format("Creation time in miliseconds: %s", creationTime));
		System.out.println(String.format("Total time in miliseconds: %s", time));
		System.out.println(String.format("Ops. per second: %s", callCount / time * 1000));
	}
	
	private Set<Integer> createIntegerSet(){
		Set<Integer> set = new HashSet<>();
		for (int i = 0; i < setSize; i++) {
			set.add(rand.nextInt());
		}
		
		return set;
	}
	
	private void integerSetTest(Set<Integer> set){
		for (int i = 0; i < callCount; i++) {
			int  integer = rand.nextInt();
			boolean cont = set.contains(integer);
		}
	}
	
	private Set<BigInteger> createBigIntegerSet(){
		Set<BigInteger> set = new HashSet<>();
		for (int i = 0; i < setSize; i++) {
			String str = "";
			for (int j = 0; j < innerSetSize; j++) {
				str += rand.nextInt(1_200_000);
			}
			set.add(new BigInteger(str));
		}
		
		return set;
	}
	
	private void bigIntegerSetTest(Set<BigInteger> set){
		for (int i = 0; i < callCount; i++) {
			String str = "";
			for (int j = 0; j < innerSetSize; j++) {
				str += rand.nextInt(1_200_000);
			}
			BigInteger bigint = new BigInteger(str);
	
			boolean cont = set.contains(bigint);
		}
	}
	
	private Set<Set<Integer>> createIntegerMultiset(){
		Set<Set<Integer>> set = new HashSet<>();
		for (int i = 0; i < setSize; i++) {
			Set<Integer> innerSet = new HashSet<>();
			
			for (int j = 0; j < innerSetSize; j++) {
				innerSet.add(rand.nextInt(1_200_000));
			}
			
			set.add(innerSet);
		}
		
		return set;
	}
	
	private void integerMultisetTest(Set<Set<Integer>> set){
		for (int i = 0; i < callCount; i++) {
			Set<Integer> innerSet = new HashSet<>();
			
			for (int j = 0; j < innerSetSize; j++) {
				innerSet.add(rand.nextInt(1_200_000));
			}
			
			boolean cont = set.contains(innerSet);
		}
	}
	
	private Set<GroupData> createGroupDataSet(){
		Set<GroupData> set = new HashSet<>();
		
		for (int i = 0; i < setSize; i++) {
			Set<PlanComputationRequest> innerSet = new HashSet<>();
			
			for (int j = 0; j < innerSetSize; j++) {
				innerSet.add(new TestRequest(rand.nextInt(1_200_000)));
			}
			
			set.add(new GroupData(innerSet, null));
		}
		
		return set;
	}
	
	private void groupDataTest(Set<GroupData> set){
		for (int i = 0; i < callCount; i++) {
			Set<PlanComputationRequest> innerSet = new HashSet<>();
			
			for (int j = 0; j < innerSetSize; j++) {
				innerSet.add(new TestRequest(rand.nextInt(1_200_000)));
			}
			
			boolean cont = set.contains(new GroupData(innerSet, null));
		}
	}
	
	private class GroupData {
		private final Set<PlanComputationRequest> requests;
		
		private final Set<PlanComputationRequest> onboardRequestLock;
		
		private int hash;

		private GroupData(Set<PlanComputationRequest> requests) {
			this(requests, null);
		}
		
		private GroupData(Set<PlanComputationRequest> requests, 
				Set<PlanComputationRequest> onboardRequestLock) {
			this.requests = requests;
			this.onboardRequestLock = onboardRequestLock;
			hash = 0;
		}

		@Override
		public int hashCode() {
			if(hash == 0){
				hash = this.requests.hashCode() % 1_200_000;
			}
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
			final GroupData other = (GroupData) obj;
			if (!Objects.equals(this.requests, other.requests)) {
				return false;
			}
			return true;
		}
	}
	
	class TestRequest implements PlanComputationRequest{
		
		private int hash;
		
		private final int id;

		public TestRequest(int id) {
			this.id = id;
			this.hash = 0;
		}
		
		
		
		@Override
		public boolean equals(Object obj) {
			return this == obj;
		}

	//	@Override
	//	public int hashCode() {
	//		return demandAgent.getSimpleId();
	//	}
		@Override
		public int hashCode() {
			if(hash == 0){
				int p = 1_200_007;
				Random rand = new Random();
				int a = rand.nextInt(p) + 1;
				int b = rand.nextInt(p);
				hash = (int) (((long) a * id + b) % p) % 1_200_000 ;
			}
			return hash;
		}

		@Override
		public int getMaxPickupTime() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public int getMaxDropoffTime() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public int getMinTime() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public int getMinTravelTime() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public SimulationNode getFrom() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public SimulationNode getTo() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public boolean isOnboard() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public PlanActionPickup getPickUpAction() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public PlanActionDropoff getDropOffAction() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public DemandAgent getDemandAgent() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public void setDemandAgent(DemandAgent demandAgent) {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public int getId() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public void setOnboard(boolean onboard) {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public SlotType getRequiredSlotType() {
			return SlotType.STANDARD_SEAT;
		}

	}
}


