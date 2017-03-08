package cz.agents.amodsim.graphbuilder;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import cz.agents.amodsim.graphbuilder.structurebuilders.RoadNetworkGraphSimplifier;
import cz.agents.amodsim.graphbuilder.structurebuilders.edge.RoadEdgeExtendedBuilder;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationEdge;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.amodsim.config.Config;
import cz.agents.amodsim.graphbuilder.structurebuilders.node.RoadNodeExtendedBuilder;
import cz.agents.basestructures.GPSLocation;
import cz.agents.basestructures.Graph;
import cz.agents.geotools.GPSLocationTools;
import cz.agents.geotools.StronglyConnectedComponentsFinder;
import cz.agents.geotools.Transformer;
import cz.agents.gtdgraphimporter.osm.DoubleExtractor;
import cz.agents.gtdgraphimporter.osm.InclExclTagEvaluator;
import cz.agents.gtdgraphimporter.osm.OneTagEvaluator;
import cz.agents.gtdgraphimporter.osm.OsmElementConsumer;
import cz.agents.gtdgraphimporter.osm.OsmGraphBuilder;
import cz.agents.gtdgraphimporter.osm.SpeedExtractor;
import cz.agents.gtdgraphimporter.osm.TagEvaluator;
import cz.agents.gtdgraphimporter.osm.TagExtractor;
import cz.agents.gtdgraphimporter.osm.WayTagExtractor;
import cz.agents.gtdgraphimporter.osm.element.OsmNode;
import cz.agents.gtdgraphimporter.osm.element.OsmRelation;
import cz.agents.gtdgraphimporter.osm.element.OsmWay;
import cz.agents.gtdgraphimporter.osm.handler.OsmHandler;
import cz.agents.gtdgraphimporter.structurebuilders.TmpGraphBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.edge.EdgeBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.node.NodeBuilder;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import static java.util.stream.Collectors.toSet;
import javax.inject.Named;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Instead of {@link cz.agents.gtdgraphimporter.GTDGraphBuilder}
 * Lighter version of it. Preparation for RoadEdgeExtended
 *
 * @author Zdenek Bousa
 */
public class SimulationGraphBuilder implements OsmElementConsumer{
    private static final Logger LOGGER = Logger.getLogger(SimulationGraphBuilder.class);
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    
    static {
        MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }
    
    
    
    
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
    
    private final Config config;
    
    private final Map<Long, OsmNode> osmNodes;
    
    /**
     * Predicate for each mode, allowed in the graph, that says if the mode is allowed on a particular way (edge)
     */
    private final Map<ModeOfTransport, TagEvaluator> modeEvaluators;

    /**
     * Predicate that says if nodes of a way are in opposite order than they really are. Important only for one-way
     * edges. Current implementation just reverse the order of the nodes therefore the one-way evaluators must
     * calculate
     * with the opposite order tags.
     */
    private final TagEvaluator oppositeDirectionEvaluator;
    
    /**
     * Factory for building graph nodes
     */
    private final Transformer projection;
    
    /**
     * Function extracting elevation from node tags
     */
    private final TagExtractor<Double> elevationExtractor;
    
    /**
     * Predicate for each mode, allowed in the graph, that says if a way (edge) is one-way for the mode
     */
    private final Map<ModeOfTransport, TagEvaluator> oneWayEvaluators;
    
    private int mergedEdges;
    
    /**
     * Function extracting max speed from way tags
     */
    private WayTagExtractor<Double> speedExtractor;
    
    /**
     * Function extracting lanes count tag.
     */
    private WayTagExtractor<Integer> lanesCountExtractor;
    
    /**
     * URL of the OSM to be parsed
     */
    private final File osmFile;
    
    protected final Set<ModeOfTransport> allowedModes;
    
    
    protected final TmpGraphBuilder<SimulationNode, SimulationEdge> builder;
    

    /**
     * Constructor
     *
     * @param config
     * @param projection      SRID
     * @param osmFile         file with OSM map
     * @param allowedOsmModes based on {@link RoadNetworkGraphBuilder#OSM_MODES}
     */
    @Inject
    public SimulationGraphBuilder(@Named("osm File") File osmFile, Set<ModeOfTransport> allowedOsmModes, 
            Config config, Transformer projection) {
        this.allowedOsmModes = allowedOsmModes;
        this.config = config;
        this.projection = projection;
        this.osmFile = osmFile;
        allowedModes = allowedOsmModes;
        osmNodes = new HashMap<>();
        modeEvaluators = new EnumMap<>(ModeOfTransport.class);
        oppositeDirectionEvaluator = new OneTagEvaluator("oneway", "-1");
        builder = new TmpGraphBuilder<>();
        elevationExtractor = new DoubleExtractor("height", 0);
        oneWayEvaluators = new EnumMap<>(ModeOfTransport.class);
        mergedEdges = 0;
    }
    
     @Override
    public void accept(OsmNode node) {
        osmNodes.put(node.id, node);
    }

    @Override
    public void accept(OsmWay way) {
        way.removeMissingNodes(osmNodes.keySet());

        Set<ModeOfTransport> modesOfTransport = getModesOfTransport(way);

        if (!modesOfTransport.isEmpty()) {
            createEdges(way, modesOfTransport);
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
    public Graph<SimulationNode, SimulationEdge> build() {
        loadMissingSettings();
        TmpGraphBuilder<SimulationNode, SimulationEdge> osmGraph = buildOsmGraphExtended();
        //TODO: Simplifier - make switch for Visio and for Simulation.
        //TODO: Properly handle RoadEdgeExtended - find opposite way and uniqueWayId
        LOGGER.debug("Graph [#nodes=" + osmGraph.getNodeCount() + ", #edges=" + osmGraph.getEdgeCount() + "] simplification");
        if(config.agentpolis.simplifyGraph){
            RoadNetworkGraphSimplifier.simplify(osmGraph, Collections.emptySet()); //not working for RoadExtended
        }
        return osmGraph.createGraph();
    }
    
    /**
     * Create nodes & edges section
     */
    private void createEdges(OsmWay way, Set<ModeOfTransport> modeOfTransports) {
        List<Long> nodes = way.getNodes();

        //reverse nodes if way is the opposite direction. Have to cooperate with one-way evaluators.
        if (oppositeDirectionEvaluator.test(way.getTags())) {
            nodes = Lists.reverse(nodes);
        }
        nodes.forEach(this::createAndAddNode);

        Set<ModeOfTransport> bidirectionalModes = getBidirectionalModes(way, modeOfTransports);

        //the EdgeType parameters doesn't take into account the possibility of reversed direction - possible fix in
        // the future
        //
        // bidirectionalStatus is used for (int) uniqueWayId and (int) oppositeWayId. If 0, then edge is one-way.
        //If the number is 1, it is a bidirectional edge (in FORWARD) and if the number is 2, then it is the opposite
        // direction of the edge (BACKWARD)
        if (bidirectionalModes.isEmpty()) {
            createAndAddOrMergeEdges(nodes, modeOfTransports, way, EdgeType.FORWARD, 0);
        } else {
            way.addTag("[OsmParser]::bidirectional", "1"); // TODO: do it properly inside WayTagExtractor

            createAndAddOrMergeEdges(nodes, modeOfTransports, way, EdgeType.FORWARD, 1);
            createAndAddOrMergeEdges(Lists.reverse(nodes), bidirectionalModes, way, EdgeType.BACKWARD, 2);
        }
    }
    
    /**
     * Create edges for each node in OsmWay
     *
     * @param bidirectionalStatus is used for (int) uniqueWayId and (int) oppositeWayId. If 0, then edge is one-way.
     *                            If the number is 1, it is a bidirectional edge (in FORWARD) and if the number is 2,
     *                            then it is the opposite direction of the edge (BACKWARD)
     */
    private void createAndAddOrMergeEdges(List<Long> nodes, Set<ModeOfTransport> modeOfTransports, OsmWay way,
                                          EdgeType edgeType, int bidirectionalStatus) {
        for (int i = 1; i < nodes.size(); i++) {
            createAndAddOrMergeEdge(nodes.get(i - 1), nodes.get(i), modeOfTransports, way, edgeType, bidirectionalStatus);
        }
    }
    
    protected void createAndAddOrMergeEdge(long fromSourceId, long toSourceId, Set<ModeOfTransport> modeOfTransports,
                                           OsmWay way, EdgeType edgeType, int bidirectionalStatus) {
        int tmpFromId = builder.getIntIdForSourceId(fromSourceId);
        int tmpToId = builder.getIntIdForSourceId(toSourceId);

        if (builder.containsEdge(tmpFromId, tmpToId)) {
            //edge already built, so add  another mode
            mergedEdges++;
            resolveConflictEdges(tmpFromId, tmpToId, modeOfTransports, way, edgeType);
        } else {
            // begin with new edge
            int uniqueId = builder.getEdgeCount();

            // decide on opposite way
            int oppositeWayUniqueId;
            if (bidirectionalStatus == 1) {
                oppositeWayUniqueId = uniqueId + 1; // opposite direction will follow in construction
            } else if (bidirectionalStatus == 2) {
                oppositeWayUniqueId = uniqueId - 1;
            } else {
                oppositeWayUniqueId = -1;
            }

            // create temporary edge
            RoadEdgeExtendedBuilder roadEdge = new RoadEdgeExtendedBuilder(tmpFromId, tmpToId, way.getId(), uniqueId, 
                    oppositeWayUniqueId, (int) calculateLength(tmpFromId, tmpToId), modeOfTransports, 
                    extractSpeed(way, edgeType), extractLanesCount(way, edgeType));

            // add edge to TmpGraphBuilder
            builder.addEdge(roadEdge);
        }
    }
    
    protected double calculateLength(int fromId, int toId) {
        NodeBuilder<? extends SimulationNode> n1 = builder.getNode(fromId);
        NodeBuilder<? extends SimulationNode> n2 = builder.getNode(toId);
        return GPSLocationTools.computeDistance(n1.location, n2.location);
    }
    
    private Integer extractLanesCount(OsmWay way, EdgeType edgeType) {
        if (EdgeType.BACKWARD == edgeType) {
            return lanesCountExtractor.getBackwardValue(way.getTags());
        } else {
            return lanesCountExtractor.getForwardValue(way.getTags());
        }
    }
    
    protected float extractSpeed(OsmWay way, EdgeType edgeType) {
        return edgeType.apply(speedExtractor, way.getTags()).floatValue();
    }

    
    protected void resolveConflictEdges(int tmpFromId, int tmpToId, Set<ModeOfTransport> newModeOfTransports,
                                        OsmWay way, EdgeType edgeType) {
        RoadEdgeExtendedBuilder edgeBuilder = (RoadEdgeExtendedBuilder) builder.getEdge(tmpFromId, tmpToId);
        edgeBuilder.addModeOfTransports(newModeOfTransports);
    }
    
    /**
     * Return subset of {@code ModeOfTransports} for which the way is bidirectional (isn't one-way).
     */
    private Set<ModeOfTransport> getBidirectionalModes(OsmWay way, Set<ModeOfTransport> ModeOfTransports) {
        return ModeOfTransports.stream().filter(mode -> isBidirectional(way, mode)).collect(toSet());
    }
    
    private boolean isBidirectional(OsmWay way, ModeOfTransport mode) {
        return !oneWayEvaluators.get(mode).test(way.getTags());
    }
    
    /**
     * Create node and give it an int number based on builder.getNodeCount() - number of already added nodes
     * in TmpGraphBuilder
     *
     * @param nodeId - source id in OsmNode
     */
    protected void createAndAddNode(long nodeId) {
        if (!builder.containsNode(nodeId)) {
            OsmNode osmNode = osmNodes.get(nodeId);
            RoadNodeExtendedBuilder roadNodeExtendedBuilder = new RoadNodeExtendedBuilder(builder.getNodeCount(),
                    nodeId, getProjectedGPS(osmNode));
            builder.addNode(roadNodeExtendedBuilder);
        }
    }
    
    private GPSLocation getProjectedGPS(double lat, double lon, double elevation) {
        return GPSLocationTools.createGPSLocation(lat, lon, (int) Math.round(elevation), projection);
    }

    private GPSLocation getProjectedGPS(OsmNode osmNode) {
        return getProjectedGPS(osmNode.lat, osmNode.lon, elevationExtractor.apply(osmNode.getTags()));
    }
    
    /**
     * OSM way modes
     */
    private Set<ModeOfTransport> getModesOfTransport(OsmWay way) {
        Set<ModeOfTransport> ModesOfTransport = EnumSet.noneOf(ModeOfTransport.class);

        for (Map.Entry<ModeOfTransport, TagEvaluator> entry : modeEvaluators.entrySet()) {
            ModeOfTransport mode = entry.getKey();
            if (entry.getValue().test(way.getTags())) {
                ModesOfTransport.add(mode);
            }
        }
        return ModesOfTransport;
    }

    /**
     * Build temporary graph and apply minor components reduction.
     *
     * @return Full road graph with only one strong component.
     */
    private TmpGraphBuilder<SimulationNode, SimulationEdge> buildOsmGraphExtended() {
        TmpGraphBuilder<SimulationNode, SimulationEdge> osmGraph = readOsmAndGetGraphBuilder();
        return removeMinorComponents(osmGraph);
    }
    
    private TmpGraphBuilder<SimulationNode, SimulationEdge> readOsmAndGetGraphBuilder() {
        parseOSM();
        return builder;
    }
    
    /**
     * parser
     */
    private void parseOSM() {
        LOGGER.info("Parsing of OSM started...");

        long t1 = System.currentTimeMillis();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            XMLReader xmlreader = parser.getXMLReader();
            xmlreader.setContentHandler(new OsmHandler(this));
            xmlreader.parse(new InputSource(osmFile.toURI().toURL().openStream()));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("OSM can't be parsed.", e);
        }

        LOGGER.info(getStatistic());
        long t2 = System.currentTimeMillis();
        LOGGER.info("Parsing of OSM finished in " + (t2 - t1) + "ms");
        osmNodes.clear();
    }

    /**
     * Stats
     * @return 
     */
    public String getStatistic() {
        return "Merged edges=" + mergedEdges;
    }
    
    /**
     * Removes from the {@code osmGraph} all nodes and edges that are not in the main component for any mode.
     *
     * @param osmGraph osm graph with multiple strong components
     */
    private TmpGraphBuilder<SimulationNode, SimulationEdge> removeMinorComponents(TmpGraphBuilder<SimulationNode, SimulationEdge> osmGraph) {
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
    private Set<Integer> getMainComponent(TmpGraphBuilder<SimulationNode, SimulationEdge> graph, ModeOfTransport mode) {
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
                '}';
    }
    
    /**
    * Check for setting
    */
   protected void loadMissingSettings() {
       loadSpeedExtractorIfNeeded();
       loadModeEvaluatorsIfNeeded();
       loadOneWayEvaluatorsIfNeeded();
       loadLaneCountExtractorIfNeeded();
   }

   /**
    * Add missing tag evaluators
    */
   private void loadSpeedExtractorIfNeeded() {
       if (speedExtractor == null) {
           try {
               speedExtractor = MAPPER.readValue(
                       SpeedExtractor.class.getResourceAsStream("default_speed_mapping.json"),
                       SpeedExtractor.class);
           } catch (IOException e) {
               throw new IllegalStateException("Default speed extractor can't be created.", e);
           }
       }
   }

   private void loadLaneCountExtractorIfNeeded() {
       if (lanesCountExtractor == null) {
           lanesCountExtractor = new LanesCountExtractor();
       } else {
           throw new IllegalStateException("Default lanes count extractor can't be created.");
       }
   }

   private void loadModeEvaluatorsIfNeeded() {
       Set<ModeOfTransport> missingModes = Sets.difference(allowedModes, modeEvaluators.keySet());
       for (ModeOfTransport mode : missingModes) {
           InputStream stream = OsmGraphBuilder.class.getResourceAsStream("mode/" + mode.name().toLowerCase() +
                   ".json");
           if (stream == null) {
               throw new IllegalStateException("Default mode evaluator for " + mode + " isn't defined. You " +
                       "have to define it.");
           }
           try {
               modeEvaluators.put(mode, MAPPER.readValue(stream, InclExclTagEvaluator.class));
           } catch (IOException e) {
               throw new IllegalStateException("Default mode evaluator for mode " + mode + " can't be created.",
                       e);
           }
       }
   }

   private void loadOneWayEvaluatorsIfNeeded() {
       Set<ModeOfTransport> missingModes = Sets.difference(allowedModes, oneWayEvaluators.keySet());

       //default evaluator for all modes.
       TagEvaluator defaultEval = TagEvaluator.ALWAYS_FALSE;
       for (ModeOfTransport mode : missingModes) {
           InputStream stream = OsmGraphBuilder.class.getResourceAsStream("oneway/" + mode.name().toLowerCase() +
                   ".json");
           if (stream == null) {
               oneWayEvaluators.put(mode, defaultEval);
           } else {
               try {
                   oneWayEvaluators.put(mode, MAPPER.readValue(stream, InclExclTagEvaluator.class));
               } catch (IOException e) {
                   LOGGER.warn("Default mode evaluator for mode " + mode + " can't be created. Used default " +
                           "evaluator for all modes.");
                   oneWayEvaluators.put(mode, defaultEval);
               }
           }
       }
   }
   
   protected enum EdgeType {
        FORWARD {
            @Override
            protected <T> TagExtractor<T> getExtractor(WayTagExtractor<T> extractor) {
                return extractor::getForwardValue;
            }
        },
        BACKWARD {
            @Override
            protected <T> TagExtractor<T> getExtractor(WayTagExtractor<T> extractor) {
                return extractor::getBackwardValue;
            }
        };

        /**
         * Get corresponding tag extractor function.
         *
         * @param extractor
         * @param <T>
         * @return
         */
        protected abstract <T> TagExtractor<T> getExtractor(WayTagExtractor<T> extractor);

        /**
         * Applies corresponding method of the {@code extractor} on the {@code tags}.
         *
         * @param extractor
         * @param tags
         * @param <T>
         * @return
         */
        public <T> T apply(WayTagExtractor<T> extractor, Map<String, String> tags) {
            return getExtractor(extractor).apply(tags);
        }
    }
}
