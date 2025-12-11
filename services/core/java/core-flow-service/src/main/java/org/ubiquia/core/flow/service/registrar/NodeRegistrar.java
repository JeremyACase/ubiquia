package org.ubiquia.core.flow.service.registrar;


import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.embeddable.NodeSettings;
import org.ubiquia.common.model.ubiquia.embeddable.CommunicationServiceSettings;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.entity.NodeEntity;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.core.flow.repository.NodeRepository;
import org.ubiquia.core.flow.service.logic.node.NodeTypeLogic;
import org.ubiquia.common.library.implementation.service.mapper.OverrideSettingsMapper;

/**
 * This is a service that can be used to register adapters with Ubiquia.
 */
@Service
public class NodeRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(NodeRegistrar.class);

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private NodeTypeLogic nodeTypeLogic;

    @Autowired
    private OverrideSettingsMapper overrideSettingsMapper;

    /**
     * Attempt to register adapters for a provided graph.
     *
     * @param graphEntity       The database entity representing the graph.
     * @param graphRegistration The registration JSON of the graph.
     * @return A list of newly-registered adapters.
     */
    public List<NodeEntity> registerNodesFor(
        GraphEntity graphEntity,
        final Graph graphRegistration)
        throws JsonProcessingException {

        var nodeEntities = new ArrayList<NodeEntity>();
        for (var node : graphRegistration.getNodes()) {
            var nodeEntity = this.tryGetNodeEntityFrom(graphEntity, node);
            nodeEntities.add(nodeEntity);
        }

        this.tryConnectNodes(nodeEntities, graphRegistration);
        var persistedNodeEntities = this.persistNodes(nodeEntities);
        graphEntity.getNodes().addAll(persistedNodeEntities);

        return persistedNodeEntities;
    }

    /**
     * A helper method to set an adapter's settings provided a registration.
     *
     * @param nodeEntity       The adapter to clean.
     * @param nodeRegistration The JSON representing the adapter's registration data.
     */
    private void setNodeSettings(
        NodeEntity nodeEntity,
        final Node nodeRegistration) {

        var entitySettings = nodeEntity.getNodeSettings();
        var registrationSettings = nodeRegistration.getNodeSettings();

        if (Objects.isNull(entitySettings)) {
            logger.info("...adapter has null settings; setting to default...");
            nodeEntity.setNodeSettings(new NodeSettings());
        } else {

            if (Objects.nonNull(registrationSettings.getInputStampKeychains())) {
                entitySettings.getInputStampKeychains().addAll(
                    registrationSettings.getInputStampKeychains());
            }

            if (Objects.nonNull(registrationSettings.getOutputStampKeychains())) {
                entitySettings.getOutputStampKeychains().addAll(
                    registrationSettings.getOutputStampKeychains());
            }

            if (Objects.nonNull(registrationSettings.getBackpressurePollFrequencyMilliseconds())) {
                entitySettings.setBackpressurePollFrequencyMilliseconds(
                    registrationSettings.getBackpressurePollFrequencyMilliseconds());
            }

            if (Objects.nonNull(registrationSettings.getInboxPollFrequencyMilliseconds())) {
                entitySettings.setInboxPollFrequencyMilliseconds(
                    registrationSettings.getInboxPollFrequencyMilliseconds());
            }

            if (Objects.nonNull(registrationSettings.getPersistOutputPayload())) {
                entitySettings.setPersistOutputPayload(registrationSettings
                    .getPersistOutputPayload());
            }

            if (Objects.nonNull(registrationSettings.getPersistInputPayload())) {
                entitySettings.setPersistInputPayload(registrationSettings
                    .getPersistInputPayload());
            }

            if (Objects.nonNull(registrationSettings.getValidateInputPayload())) {
                entitySettings.setValidateInputPayload(registrationSettings
                    .getValidateInputPayload());
            }

            if (Objects.nonNull(registrationSettings.getValidateOutputPayload())) {
                entitySettings.setValidateOutputPayload(registrationSettings
                    .getValidateOutputPayload());
            }
        }

        if (this.nodeTypeLogic.nodeTypeRequiresEgressSettings(nodeEntity.getNodeType())) {
            if (Objects.isNull(nodeEntity.getEgressSettings())) {
                logger.info("...node egress settings are null but are required for {} node;"
                        + " setting default egress settings...",
                    nodeEntity.getNodeType());
                nodeEntity.setEgressSettings(new EgressSettings());
            }
        }
    }

    @Transactional
    private List<NodeEntity> persistNodes(List<NodeEntity> nodeEntities) {
        var persistedEntities = this.nodeRepository.saveAll(nodeEntities);
        return persistedEntities;
    }

    /**
     * Provided a graph, attempt to "connect" adapters by setting their upstream/downstream
     * counterparts.
     *
     * @param graphRegistration The graph to connect adapters for.
     */
    private void tryConnectNodes(List<NodeEntity> nodes, final Graph graphRegistration) {

        for (var edge : graphRegistration.getEdges()) {

            var leftNodeRecord = nodes
                .stream()
                .filter(x -> x.getName().equals(edge.getLeftNodeName()))
                .findFirst();

            if (leftNodeRecord.isEmpty()) {
                throw new IllegalArgumentException("ERROR: Could not find left node "
                    + "named: "
                    + edge.getLeftNodeName());
            }

            for (var rightNodeName : edge.getRightNodeNames()) {

                var rightNodeRecord = nodes
                    .stream()
                    .filter(x -> x.getName().equals(rightNodeName))
                    .findFirst();

                if (rightNodeRecord.isEmpty()) {
                    throw new IllegalArgumentException("ERROR: Could not find right node "
                        + "named: "
                        + rightNodeName);
                }
                var leftNode = leftNodeRecord.get();
                var rightNode = rightNodeRecord.get();

                leftNode.getDownstreamNodes().add(rightNode);
                rightNode.getUpstreamNodes().add(leftNode);
            }
        }
    }

    /**
     * Provided a graph and an associated adapter registration, attempt to get an adapter entity.
     *
     * @param graphEntity         The graph entity to get an adapter for.
     * @param nodeRegistration The registration object representing the adapter.
     * @return An adapter entity.
     */
    @Transactional
    private NodeEntity tryGetNodeEntityFrom(
        final GraphEntity graphEntity,
        final Node nodeRegistration) throws JsonProcessingException {

        var nodeEntity = this.tryGetNodeEntityHelper(graphEntity, nodeRegistration);
        this.setNodeSettings(nodeEntity, nodeRegistration);
        return nodeEntity;
    }

    /**
     * A helper method that can build a new adapter entity.
     *
     * @param graphEntity         The graph entity we're building an adapter for.
     * @param node The object representing a new adapter.
     * @return A new adapter entity.
     */
    private NodeEntity tryGetNodeEntityHelper(
        final GraphEntity graphEntity,
        final Node node)
        throws JsonProcessingException {

        var nodeEntity = new NodeEntity();
        nodeEntity.setGraph(graphEntity);
        nodeEntity.setCommunicationServiceSettings(node.getCommunicationServiceSettings());
        nodeEntity.setNodeType(node.getNodeType());
        nodeEntity.setName(node.getName());
        nodeEntity.setNodeSettings(node.getNodeSettings());
        nodeEntity.setBrokerSettings(node.getBrokerSettings());
        nodeEntity.setDescription(node.getDescription());
        nodeEntity.setEgressSettings(node.getEgressSettings());
        nodeEntity.setEndpoint(node.getEndpoint());
        nodeEntity.setPollSettings(node.getPollSettings());
        nodeEntity.setUpstreamNodes(new ArrayList<>());
        nodeEntity.setDownstreamNodes(new ArrayList<>());
        nodeEntity.setOutboxFlowMessages(new ArrayList<>());

        nodeEntity.setOverrideSettings(new HashSet<>());
        if (Objects.nonNull(node.getOverrideSettings())) {
            var converted = this.overrideSettingsMapper.mapToStringified(
                node.getOverrideSettings());
            nodeEntity.getOverrideSettings().addAll(converted);
        }

        nodeEntity.setInputSubSchemas(new HashSet<>());
        if (Objects.nonNull(node.getInputSubSchemas())) {
            for (var schema : node.getInputSubSchemas()) {
                nodeEntity.getInputSubSchemas().add(schema);
            }
        }

        if (Objects.isNull(nodeEntity.getCommunicationServiceSettings())) {
            nodeEntity.setCommunicationServiceSettings(new CommunicationServiceSettings());
        }

        nodeEntity.setOutputSubSchema(node.getOutputSubSchema());

        return nodeEntity;
    }
}
