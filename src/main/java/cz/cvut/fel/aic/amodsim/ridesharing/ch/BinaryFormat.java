package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import java.io.*;
import java.util.*;


public class BinaryFormat {
    private static final long MAX_FILE_VERSION_SUPPORTED = 6;
    private static final long MIN_FILE_VERSION_SUPPORTED = 5;
    private static final long FILE_VERSION_WRITTEN = MAX_FILE_VERSION_SUPPORTED;
	
	private static DataInputStream inStream(InputStream inStream) throws FileNotFoundException {
        return new DataInputStream(new BufferedInputStream(inStream));
    }
    
    private static DataOutputStream outStream(String filename) throws FileNotFoundException {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
    }
	
	
	
    
    public Map<Long,CHNode> read(String nodeFile, String wayFile, StatusMonitor monitor) throws IOException {
        try (FileInputStream nodesIn = new FileInputStream(nodeFile);
              FileInputStream waysIn = new FileInputStream(wayFile)) {
            return read(nodesIn, waysIn, monitor);
        }
    }
    
//    public MapData read(String nodeFile, String wayFile, String turnRestrictionFile, StatusMonitor monitor) throws IOException {
//        try ( FileInputStream nodesIn = new FileInputStream(nodeFile);
//              FileInputStream waysIn = new FileInputStream(wayFile);
//              FileInputStream restrictionsIn = (turnRestrictionFile==null?null:new FileInputStream(turnRestrictionFile))) {
//            return read(nodesIn, waysIn, restrictionsIn, monitor);
//        }
//    }
    
    public Map<Long,CHNode> read(InputStream nodesIn, InputStream waysIn, StatusMonitor monitor) throws IOException {       
        HashMap<Long, CHNode> nodesById;
        try (DataInputStream dis = inStream(nodesIn)) {
            nodesById = readNodes(dis, monitor);
        }
        
        try (DataInputStream dis = inStream(waysIn)) {
            loadEdgesGivenNodes(nodesById, dis, monitor);
        }
        
//        MapData md = new MapData(nodesById, turnRestrictions, monitor);
//        md.validate(monitor);
		
        return nodesById;
    }
    
    public void write(List<CHNode> toWrite, String nodeFile, String wayFile) throws IOException {
        try (DataOutputStream waysOut = outStream(wayFile);
                DataOutputStream nodesOut = outStream(nodeFile);) {
            write(toWrite, nodesOut, waysOut, null);
        }
    }
    
    /*Not broken or anything - just currently unused.
    public void write(MapData toWrite, String nodeFile, String wayFile, String restrictionFile) throws IOException {
        try (DataOutputStream waysOut = outStream(wayFile);
                DataOutputStream nodesOut = outStream(nodeFile);
                DataOutputStream restrictionsOut = (restrictionFile==null?null:outStream(restrictionFile))) {
            write(toWrite, nodesOut, waysOut, restrictionsOut);
        }
    }*/
    
    public void write(List<CHNode> toWrite, DataOutputStream nodesOut, DataOutputStream waysOut, 
			DataOutputStream restrictionsOut) throws IOException {
        writeEdges(toWrite, waysOut);
        writeNodesWithoutEdges(toWrite,nodesOut);
    }
    

    
    private HashMap<Long,CHNode> readNodes(DataInputStream source, StatusMonitor monitor) throws IOException {
        Preconditions.checkNoneNull(monitor);
        long fileFormatVersion = source.readLong();
        checkFileFormatVersion(fileFormatVersion);
        
        long totalNodeCount = (fileFormatVersion >= 6 ? source.readLong() : -1);
        long nodesLoadedSoFar = 0;
        HashMap<Long,CHNode> nodesById = new HashMap(Math.max(1000, (int)totalNodeCount));
        
        try {
            monitor.updateStatus(MonitoredProcess.LOAD_NODES, nodesLoadedSoFar, totalNodeCount);
            
            while(true) {
                long nodeId = source.readLong();
                long contractionOrder = source.readLong();
                int properties = source.readByte();
                double lat = source.readDouble();
                double lon = source.readDouble();
                
                CHNode node = new CHNode((int) nodeId, (float)lat, (float)lon);
                if (contractionOrder == Long.MAX_VALUE)
                    contractionOrder = CHNode.UNCONTRACTED;
                node.contractionOrder=(int) contractionOrder;
                
                nodesById.put(nodeId, node);
                nodesLoadedSoFar++;
                if (nodesLoadedSoFar % 10000 == 0)
                    monitor.updateStatus(MonitoredProcess.LOAD_NODES, nodesLoadedSoFar, totalNodeCount);
            }
            
        } catch (EOFException e) { }
        
        monitor.updateStatus(MonitoredProcess.LOAD_NODES, nodesLoadedSoFar, totalNodeCount);
        return nodesById;
    }
    
    private void loadEdgesGivenNodes(HashMap<Long, CHNode> nodesById, DataInputStream source, StatusMonitor monitor) 
			throws IOException {
        Preconditions.checkNoneNull(monitor);
        long fileFormatVersion = source.readLong();
        checkFileFormatVersion(fileFormatVersion);
        
        long totalEdgeCount = (fileFormatVersion >= 6 ? source.readLong() : -1);
        long edgesLoadedSoFar = 0;
        HashMap<Long,CHEdge> edgesById = new HashMap(Math.max(1000, (int)totalEdgeCount));
        
        try {
            monitor.updateStatus(MonitoredProcess.LOAD_WAYS, edgesLoadedSoFar, totalEdgeCount);
            
            while(true) {
                long edgeId = source.readLong();
                long fromNodeId = source.readLong();
                long toNodeId = source.readLong();
                int cost = source.readInt();
                byte properties = source.readByte();
                boolean isShortcut = (properties&0x01)==0x01;
                long firstEdgeId = source.readLong();
                long secondEdgeId = source.readLong();
                
                CHNode fromNode = nodesById.get(fromNodeId);
                CHNode toNode = nodesById.get(toNodeId);
                if (fromNode==null || toNode==null) {
                    String problem = "Tried to load nodes " + fromNodeId + 
                            " and " + toNodeId + " for edge " + edgeId + 
                            " but got " + fromNode + " and " + toNode;
                    throw new RuntimeException(problem);
                }
                Preconditions.checkNoneNull(fromNode,toNode);
                
                CHEdge edge;
                if (isShortcut) {
                    CHEdge firstEdge = edgesById.get(firstEdgeId);
                    CHEdge secondEdge = edgesById.get(secondEdgeId);
                    Preconditions.checkNoneNull(firstEdge,secondEdge);
                    edge = new ShortcutEdge(firstEdge, secondEdge);
                } else {
                    edge = new WraperEdge(edgeId, fromNode, toNode, cost);
                }
                
                fromNode.outEdges.add(edge);
                toNode.inEdges.add(edge);
                edgesById.put(edgeId, edge);
                
                edgesLoadedSoFar++;
                if (edgesLoadedSoFar % 10000 == 0)
                    monitor.updateStatus(MonitoredProcess.LOAD_WAYS, edgesLoadedSoFar, totalEdgeCount);
            }
            
        } catch (EOFException e) { }
        
        monitor.updateStatus(MonitoredProcess.LOAD_WAYS, edgesLoadedSoFar, totalEdgeCount);
        CHNode.sortNeighborListsAll(nodesById.values());
    }
    
    
    private void writeNodesWithoutEdges(Collection<CHNode> toWrite, DataOutputStream dest) throws IOException {
        dest.writeLong(FILE_VERSION_WRITTEN);
        dest.writeLong(toWrite.size());
        
        for (CHNode node : toWrite) {
            dest.writeLong(node.node.id);
            if (node.isContracted()) { // REVISIT next time we bump the file version, maybe write it as an int?
                dest.writeLong(node.contractionOrder);
            } else {
                dest.writeLong(Long.MAX_VALUE);
            }
            int properties = 0x00;
            dest.writeByte(properties);
            dest.writeDouble(node.node.getLatitude());
            dest.writeDouble(node.node.getLongitude());
        }
    }
    
    private void writeEdges(Collection<CHNode> toWrite, DataOutputStream dos) throws IOException {
        dos.writeLong(FILE_VERSION_WRITTEN);
        dos.writeLong(calculateTotalEdgeCount(toWrite));
        
        HashSet<Long> writtenEdges = new HashSet();
        for (CHNode node : toWrite) {
            for (CHEdge edge : node.outEdges) {
                writeEdgeRecursively(edge, writtenEdges, dos);
            }
        }
    }
    
    private long calculateTotalEdgeCount(Collection<CHNode> toWrite) {
        long totalEdgeCount = 0;
        for (CHNode n : toWrite) {
            totalEdgeCount += n.outEdges.size();
        }
        return totalEdgeCount;
    }
        
    private void writeEdgeRecursively(CHEdge edge, HashSet<Long> alreadyWritten, DataOutputStream dos) throws IOException {
        if (edge == null || alreadyWritten.contains(edge.id)) {
            return;
        }
        
		if(edge instanceof ShortcutEdge){
			ShortcutEdge shortcutEdge = (ShortcutEdge) edge;
			writeEdgeRecursively(shortcutEdge.shortcutFirstEdge,alreadyWritten,dos);
			writeEdgeRecursively(shortcutEdge.shortcutSecondEdge,alreadyWritten,dos);
		}
        
        dos.writeLong(edge.id);
        dos.writeLong(edge.from.node.id);
        dos.writeLong(edge.to.node.id);
        dos.writeInt(edge.cost);
        
        int properties = (edge  instanceof ShortcutEdge ? 0x01 : 0x00);
        dos.writeByte(properties);
        
        if (edge instanceof ShortcutEdge) {
			ShortcutEdge shortcutEdge = (ShortcutEdge) edge;
            dos.writeLong(shortcutEdge.shortcutFirstEdge.id);
            dos.writeLong(shortcutEdge.shortcutSecondEdge.id);
        } else {
            dos.writeLong(0);
            dos.writeLong(0);
        }
        
        alreadyWritten.add(edge.id);
    }
    
//    private HashSet<TurnRestriction> readTurnRestrictions(DataInputStream source) throws IOException {
//        long fileFormatVersion = source.readLong();
//        checkFileFormatVersion(fileFormatVersion);
//        
//        long totalRestrictionCount = (fileFormatVersion >= 6 ? source.readLong() : -1);
//        HashSet<TurnRestriction> result = new HashSet(Math.max(1000, (int)totalRestrictionCount));
//        
//        try {
//            
//            while (true) {
//                long turnRestrictionId = source.readLong();
//                boolean typeStartsWithNo = source.readBoolean();
//                int entryCount = source.readInt();
//                List<Long> edgeIds = new ArrayList(entryCount);
//                
//                for (int i=0 ; i<entryCount ; i++) {
//                    edgeIds.add(source.readLong());
//                }
//                
//                TurnRestriction.TurnRestrictionType trt = (typeStartsWithNo?TurnRestriction.TurnRestrictionType.NOT_ALLOWED:TurnRestriction.TurnRestrictionType.ONLY_ALLOWED);
//                result.add(new TurnRestriction(turnRestrictionId, trt, edgeIds));
//            }
//            
//        } catch (EOFException e) { }
//        
//        return result;
//    }
//    
//    private void writeTurnRestrictions(Collection<TurnRestriction> toWrite, DataOutputStream dos) throws IOException {
//        dos.writeLong(FILE_VERSION_WRITTEN);
//        dos.writeLong(toWrite.size());
//        
//        for (TurnRestriction tr : toWrite) {
//            
//            dos.writeLong(tr.getTurnRestrictionId());
//            dos.writeBoolean(tr.getType()==TurnRestriction.TurnRestrictionType.NOT_ALLOWED);
//            dos.writeInt(tr.getDirectedEdgeIds().size());
//            for (Long edgeId : tr.getDirectedEdgeIds()) {
//                dos.writeLong(edgeId);
//            }
//            
//        }
//    }
    
    private void checkFileFormatVersion(long fileFormatVersion) throws IOException{
        if (fileFormatVersion < MIN_FILE_VERSION_SUPPORTED) {
            throw new IOException("File format version, " + fileFormatVersion + ", is below lowest version supported, " + MIN_FILE_VERSION_SUPPORTED);
        } else if (fileFormatVersion > MAX_FILE_VERSION_SUPPORTED) {
            throw new IOException("File format version, " + fileFormatVersion + ", is above greatest version supported, " + MAX_FILE_VERSION_SUPPORTED);
        }
    }

}
