package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import java.util.List;

/**
 *
 * @author F.I.D.O.
 */
public class Ordering implements Comparable<Ordering> {
	final int edgeCountReduction;
	final int contractionDepth;
	final long idHash; // If everything else is equal we don't care about the order - but it's useful for it to be stable between runs.

	public Ordering(CHNode node, int edgeCountReduction) {
		this.edgeCountReduction = edgeCountReduction;
		contractionDepth = Math.max(getMaxContractionDepth(node.outEdges), getMaxContractionDepth(node.inEdges));
		idHash = hashNodeId(node.node.id);
	}

	private long hashNodeId(long nodeId) {
		// If nodes happen to go 1,2,3,4,5,6... we'd rather not contract
		// them in that order.
		return 6364136223846793005L * nodeId + 1442695040888963407L;
	}

	public int compareTo(Ordering o) {
		if (o == null) {
			return -1;
		} else if (this.edgeCountReduction>=0 && o.edgeCountReduction<0) {
			return -1;
		} else if (this.edgeCountReduction<0 && o.edgeCountReduction>=0) {
			return 1;
		} else if (this.contractionDepth != o.contractionDepth) {
			return Integer.compare(this.contractionDepth,o.contractionDepth);
		} else if (this.edgeCountReduction != o.edgeCountReduction) {
			return Integer.compare(o.edgeCountReduction,this.edgeCountReduction);
		} else if (this.idHash != o.idHash) {
			return Long.compare(this.idHash,o.idHash);
		} else {
			return 0;
		}
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 17 * hash + this.edgeCountReduction;
		hash = 17 * hash + this.contractionDepth;
		hash = 17 * hash + (int)(this.idHash ^ (this.idHash >>> 32));;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final Ordering other = (Ordering) obj;
		return (this.edgeCountReduction == other.edgeCountReduction
				&& this.contractionDepth == other.contractionDepth
				&& this.idHash == other.idHash);
	}

	@Override
	public String toString() {
		return "(edge reduction=" + edgeCountReduction + ", depth=" + contractionDepth + ", idHash=" + idHash + ')';
	}
	
	private static int getMaxContractionDepth(List<CHEdge> edges) {
        int maxContractionDepth = 0;
        for (CHEdge edge : edges) {
            maxContractionDepth = Math.max(maxContractionDepth, edge.contractionDepth);
        }
        return maxContractionDepth;
    }
}
