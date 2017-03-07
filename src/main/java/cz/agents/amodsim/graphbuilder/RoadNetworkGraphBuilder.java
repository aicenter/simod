package cz.agents.amodsim.graphbuilder;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import cz.agents.amodsim.graphbuilder.structurebuilders.RoadNetworkGraphSimplifier;
import cz.agents.amodsim.graphbuilder.structurebuilders.edge.RoadEdgeExtendedBuilder;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationEdge;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.RoadNodeExtended;
import cz.agents.amodsim.config.Config;
import cz.agents.basestructures.Graph;
import cz.agents.geotools.StronglyConnectedComponentsFinder;
import cz.agents.geotools.Transformer;
import cz.agents.gtdgraphimporter.osm.OsmElementConsumer;
import cz.agents.gtdgraphimporter.osm.TagEvaluator;
import cz.agents.gtdgraphimporter.osm.element.OsmNode;
import cz.agents.gtdgraphimporter.osm.element.OsmRelation;
import cz.agents.gtdgraphimporter.osm.element.OsmWay;
import cz.agents.gtdgraphimporter.structurebuilders.TmpGraphBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.edge.EdgeBuilder;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import javax.inject.Named;

/**
 * Instead of {@link cz.agents.gtdgraphimporter.GTDGraphBuilder}
 * Lighter version of it. Preparation for RoadEdgeExtended
 *
 * @author Zdenek Bousa
 */
public class RoadNetworkGraphBuilder implements OsmElementConsumer{
    private static final Logger LOGGER = Logger.getLogger(RoadNetworkGraphBuilder.class);
    
    /**
     * Set of all modes that can be loaded from OSM without any additional information required.
     */
    public static final Set<ModeOfTransport> OSM_MODES = Sets.immutableEnumSet(
            ModeOfTransport.WALK,
            ModeOfTransport.TAXI,
            ModeOfTransport.CAR,
            ModeOfTransport.MOTORCYCLE,
            ModeOfTransport.BIKE);
    
    
    

    /**
     * Modes to be loaded from OSM.
     */
    private final Set<ModeOfTransport> allowedOsmModes;

    /**
     * Builder and parser OSM for RoadExtended
     */
    private final OsmGraphBuilderExtended.Builder osmBuilderBuilderExtended;
    
    private final Config config;
    
    private final Map<Long, OsmNode> osmNodes;
    
    /**
     * Predicate for each mode, allowed in the graph, that says if the mode is allowed on a particular way (edge)
     */
    private final Map<ModeOfTransport, TagEvaluator> modeEvaluators;

    

    /**
     * Constructor
     *
     * @param config
     * @param projection      SRID
     * @param osmFile         file with OSM map
     * @param allowedOsmModes based on {@link RoadNetworkGraphBuilder#OSM_MODES}
     */
    @Inject
    public RoadNetworkGraphBuilder(@Named("osm File") File osmFile, Set<ModeOfTransport> allowedOsmModes, 
            Config config, Transformer projection) {
        this.allowedOsmModes = allowedOsmModes;
        this.config = config;
        this.osmBuilderBuilderExtended = new OsmGraphBuilderExtended.Builder(osmFile, projection, allowedOsmModes);
        osmNodes = new HashMap<>();
        modeEvaluators = new EnumMap<>(ModeOfTransport.class);
    }
    
     @Override
    public void accept(OsmNode node) {
        osmNodes.put(node.id, node);
    }

    @Override
    public void accept(OsmWay way) {
        way.removeMissingNodes(osmNodes.keySet());

        Set<ModeOfTransport> ModeOfTransports = getModeOfTransports(way);

        if (!ModeOfTransports.isEmpty()) {
            createEdges(way, ModeOfTransports);
        }
    }

    @Override
    public void accept(OsmRelation relation) {
    }

    /**
     * Construct road graph
     *
     * @return Graph that has one main strong component and might have been simplified (impact on visio - more sharp curves)
     */
    public Graph<RoadNodeExtended, SimulationEdge> build() {
        TmpGraphBuilder<RoadNodeExtended, SimulationEdge> osmGraph = buildOsmGraphExtended();
        //TODO: Simplifier - make switch for Visio and for Simulation.
        //TODO: Properly handle RoadEdgeExtended - find opposite way and uniqueWayId
        LOGGER.debug("Graph [#nodes=" + osmGraph.getNodeCount() + ", #edges=" + osmGraph.getEdgeCount() + "] simplification");
        if(config.agentpolis.simplifyGraph){
            RoadNetworkGraphSimplifier.simplify(osmGraph, Collections.emptySet()); //not working for RoadExtended
        }
        return osmGraph.createGraph();
    }
    
    /**
     * OSM way modes
     */
    private Set<ModeOfTransport> getModeOfTransports(OsmWay way) {
        Set<ModeOfTransport> ModeOfTransports = EnumSet.noneOf(ModeOfTransport.class);

        for (Map.Entry<ModeOfTransport, TagEvaluator> entry : modeEvaluators.entrySet()) {
            ModeOfTransport mode = entry.getKey();
            if (entry.getValue().test(way.getTags())) {
                ModeOfTransports.add(mode);
            }
        }
        return ModeOfTransports;
    }

    /**
     * Build temporary graph and apply minor components reduction.
     *
     * @return Full road graph with only one strong component.
     */
    private TmpGraphBuilder<RoadNodeExtended, SimulationEdge> buildOsmGraphExtended() {
        TmpGraphBuilder<RoadNodeExtended, SimulationEdge> osmGraph = osmBuilderBuilderExtended.build().readOsmAndGetGraphBuilder();
        return removeMinorComponents(osmGraph);
    }

    /**
     * Removes from the {@code osmGraph} all nodes and edges that are not in the main component for any mode.
     *
     * @param osmGraph osm graph with multiple strong components
     */
    private TmpGraphBuilder<RoadNodeExtended, SimulationEdge> removeMinorComponents(TmpGraphBuilder<RoadNodeExtended, SimulationEdge> osmGraph) {
        LOGGER.debug("Calculating main components for all modes...");
        SetMultimap<Integer, ModeOfTransport> modesOnNodes = HashMultimap.create();
        for (ModeOfTransport mode : allowedOsmModes) {
            Set<Integer> mainComponent = getMainComponent(osmGraph, mode);
            mainComponent.forEach(i -> modesOnNodes.put(i, mode));
        }

        Predicate<EdgeBuilder<? extends SimulationEdge>> filter = edge -> {
            RoadEdgeExtendedBuilder roadEdgeExtendedBuilder = (RoadEdgeExtendedBuilder) edge;
            roadEdgeExtendedBuilder.intersectModeOfTransports(modesOnNodes.get(roadEdgeExtendedBuilder.getTmpFromId()));
            roadEdgeExtendedBuilder.intersectModeOfTransports(modesOnNodes.get(roadEdgeExtendedBuilder.getTmpToId()));
            return roadEdgeExtendedBuilder.getModeOfTransports().isEmpty();
        };
        int removedEdges = osmGraph.removeEdges(filter);
        LOGGER.debug("Removed " + removedEdges + " edges.");

        int removedNodes = osmGraph.removeIsolatedNodes();
        LOGGER.debug("Removed " + removedNodes + " nodes.");
        LOGGER.debug("Nodes by degree: ");
        osmGraph.getNodesByDegree().forEach((k, v) -> LOGGER.debug(k + "->" + v.size()));
        return osmGraph;
    }

    /**
     * Main strong component
     */
    private Set<Integer> getMainComponent(TmpGraphBuilder<RoadNodeExtended, SimulationEdge> graph, ModeOfTransport mode) {
        List<EdgeBuilder<? extends SimulationEdge>> feasibleEdges = graph.getFeasibleEdges(mode);
        return getMainComponent(feasibleEdges);
    }

    /**
     * Find strong component by size
     */
    private Set<Integer> getMainComponent(Collection<EdgeBuilder<? extends SimulationEdge>> edges) {
        Set<Integer> nodeIds = new HashSet<>();
        Map<Integer, Set<Integer>> edgeIds = new HashMap<>();
        for (EdgeBuilder<? extends SimulationEdge> edgeExtendedBuilder : edges) {
            int fromId = edgeExtendedBuilder.getTmpFromId();
            int toId = edgeExtendedBuilder.getTmpToId();
            nodeIds.add(fromId);
            nodeIds.add(toId);
            Set<Integer> outgoing = edgeIds.computeIfAbsent(fromId, k -> new HashSet<>());
            outgoing.add(toId);
        }
        return StronglyConnectedComponentsFinder.getStronglyConnectedComponentsSortedBySize(nodeIds, edgeIds).get(0);
    }

    @Override
    public String toString() {
        return "RoadNetworkGraphBuilder{" +
                "allowedOsmModes=" + allowedOsmModes +
                ", osmBuilderBuilderExtended=" + osmBuilderBuilderExtended +
                '}';
    }
}
