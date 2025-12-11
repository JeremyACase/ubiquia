package org.ubiquia.core.flow.service.visitor.validator;


import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.embeddable.SubSchema;
import org.ubiquia.common.model.ubiquia.enums.EgressType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;

/**
 * A service dedicated to validating nodes.
 */
@Service
public class NodeValidator {

    private static final Logger logger = LoggerFactory.getLogger(NodeValidator.class);
    @Autowired
    private SubSchemaValidator subSchemaValidator;

    /**
     * Try to validate a list of nodes.
     *
     * @param nodes The nodes to validate.
     * @throws Exception Exceptions from attempting to validate any node.
     */
    public void tryValidate(final List<Node> nodes) throws Exception {
        logger.info("...validating nodes...");
        for (var node : nodes) {
            this.tryValidate(node);
        }
        logger.info("...validated nodes...");
    }

    /**
     * Attempt to validate an node, mostly depending on its type.
     *
     * @param node The node to validate.
     * @throws Exception Exceptions from validating the node.
     */
    public void tryValidate(final Node node) throws Exception {
        switch (node.getNodeType()) {
            case EGRESS: {
                this.tryValidateEgressNode(node);
            }
            break;

            case MERGE: {
                this.tryValidateMergeNode(node);
            }
            break;

            case POLL: {
                this.tryValidatePollNode(node);
            }
            break;

            case PUBLISH: {
                this.tryValidatePublishNode(node);
            }
            break;

            case PUSH: {
                this.tryValidatePushNode(node);
            }
            break;

            case QUEUE: {
                this.tryValidateQueueNode(node);
            }
            break;

            case HIDDEN: {
                this.tryValidateHiddenNode(node);
            }
            break;

            case SUBSCRIBE: {
                this.tryValidateSubscribeNode(node);
            }
            break;

            default: {
                throw new Exception("ERROR: Unrecognized node type: "
                    + node.getNodeType());
            }
        }
    }

    /**
     * Determine if two nodes having matching output and input schemas.
     *
     * @param outputNode The output node.
     * @param inputNode  The input node.
     * @return Whether or not the nodes have matching output/input schemas.
     */
    public Boolean haveMatchingInputOutput(final Node outputNode, final Node inputNode) {

        var hasMatchingInputOutput = false;

        if (Objects.nonNull(outputNode.getOutputSubSchema())) {

            var matchingInputSubSchema = this.tryGetMatchingInputSubSchema(
                outputNode,
                inputNode);

            if (matchingInputSubSchema.isEmpty()) {
                logger.info("Could not find a matching sub schema between output node {}"
                        + " and input  {}",
                    outputNode.getName(),
                    inputNode.getName());
            } else {
                hasMatchingInputOutput = this.subSchemaValidator.areEquivalent(
                    matchingInputSubSchema.get(),
                    outputNode.getOutputSubSchema());
            }
        }

        return hasMatchingInputOutput;
    }

    /**
     * Attempt to validate that an egress node has appropriate configurations.
     *
     * @param node The node to validate.
     */
    public void tryValidateEgressNode(final Node node) {
        this.tryValidateNodeSettings(node);
        this.validateNodeHasNoComponent(node);
        this.validateNodeHasNoDownstreamNodes(node);
        this.validateNodeHasASingleUpstreamNode(node);
        this.validateNodeHasEgressSettings(node);
        this.validateNodeIsNotPassthrough(node);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a merge adapter has appropriate configurations.
     *
     * @param node The adapter to validate.
     */
    public void tryValidateMergeNode(final Node node) {
        this.tryValidateNodeSettings(node);
        this.validateNodeHasEgressSettings(node);
        this.validateNodeIsNotPassthroughWithoutDownstreamNodes(node);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a poll node has appropriate configurations.
     *
     * @param node The node to validate.
     */
    public void tryValidatePollNode(final Node node) {
        this.tryValidateNodeSettings(node);
        this.validateNodeHasNoUpstreamNodes(node);
        this.validateNodeHasPollSettings(node);
        this.validateNodeHasASingleInputSubSchema(node);
        this.validateNodeIsNotPassthroughWithoutDownstreamNodes(node);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a publish node has appropriate configurations.
     *
     * @param node The node to validate.
     */
    public void tryValidatePublishNode(final Node node) {
        this.tryValidateNodeSettings(node);
        this.validateNodeHasNoComponent(node);
        this.validateNodeHasASingleUpstreamNode(node);
        this.validateNodeHasNoDownstreamNodes(node);
        this.validateNodeHasNoEgressSettings(node);
        this.validateNodeHasASingleInputSubSchema(node);
        this.validateNodeIsNotPassthrough(node);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a push node has appropriate configurations.
     *
     * @param node The node to validate.
     */
    public void tryValidatePushNode(final Node node) {
        this.tryValidateNodeSettings(node);
        this.validateNodeHasNoUpstreamNodes(node);
        this.validateNodeHasEgressSettings(node);
        this.validateNodeHasASingleInputSubSchema(node);
        this.validateNodeIsNotPassthroughWithoutDownstreamNodes(node);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a queue node has appropriate configurations.
     *
     * @param node The node to validate.
     */
    public void tryValidateQueueNode(final Node node) {
        this.tryValidateNodeSettings(node);
        this.validateNodeHasNoComponent(node);
        this.validateNodeHasASingleUpstreamNode(node);
        this.validateNodeHasNoDownstreamNodes(node);
        this.validateNodeHasNoEgressSettings(node);
        this.validateNodeHasASingleInputSubSchema(node);
        this.validateNodeIsNotPassthrough(node);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a hidden node has appropriate configurations.
     *
     * @param node The node to validate.
     */
    public void tryValidateHiddenNode(final Node node) {
        this.tryValidateNodeSettings(node);
        this.validateNodeHasASingleUpstreamNode(node);
        this.validateNodeHasEgressSettings(node);
        this.validateNodeHasASingleInputSubSchema(node);
        this.validateNodeIsNotPassthroughWithoutDownstreamNodes(node);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a subscribe node has appropriate configurations.
     *
     * @param node The node to validate.
     */
    public void tryValidateSubscribeNode(final Node node) {
        this.tryValidateNodeSettings(node);
        this.validateNodeHasEgressSettings(node);
        this.validateNodeHasNoUpstreamNodes(node);
        this.validateNodeHasASingleInputSubSchema(node);
        this.validateNodeIsNotPassthroughWithoutDownstreamNodes(node);
        logger.info("...validated...");
    }

    /**
     * Helper method that attempts to find a matching input sub schema for a pair of output/input
     * nodes.
     *
     * @param outputNode The output node.
     * @param inputNode  The input node.
     * @return A matching subschema, if applicable.
     */
    private Optional<SubSchema> tryGetMatchingInputSubSchema(
        final Node outputNode,
        final Node inputNode) {

        var outputSubSchema = outputNode.getOutputSubSchema();
        var match = inputNode.getInputSubSchemas()
            .stream()
            .filter(x -> x.getModelName().equals(outputSubSchema.getModelName()))
            .findFirst();
        return match;
    }

    /**
     * A helper method for verifying nodes.
     *
     * @param node The node to verify.
     */
    private void tryValidateNodeSettings(final Node node) {
        logger.info("Validating {} node named {}...",
            node.getNodeType(),
            node.getName());

        var nodeSettings = node.getNodeSettings();
        if (Objects.nonNull(nodeSettings.getBackpressurePollFrequencyMilliseconds())
            && nodeSettings.getBackpressurePollFrequencyMilliseconds()
            > TimeUnit.MINUTES.toMillis(1)) {
            throw new IllegalArgumentException("ERROR: Backpressure polling frequency must be"
                + " less than a minute...");
        }
    }

    /**
     * A helper method to validate whether an node has valid egress settings.
     *
     * @param node The node to verify.
     */
    private void validateNodeHasEgressSettings(final Node node) {

        logger.info("...validating node named {} has "
                + "valid egress settings...",
            node.getName());

        var egressSettings = node.getEgressSettings();
        if (Objects.isNull(egressSettings)) {
            throw new IllegalArgumentException("ERROR: "
                + node.getNodeType()
                + " type node is required to "
                + "have egress settings!");
        }

        if (egressSettings.getEgressType().equals(EgressType.NONE)) {
            throw new IllegalArgumentException("ERROR: "
                + node.getNodeType()
                + " type node is required to "
                + "have an egress output type set!");
        }

        if (egressSettings.getEgressType().equals(EgressType.SYNCHRONOUS)) {
            if (egressSettings.getEgressConcurrency() > 1) {
                throw new IllegalArgumentException("ERROR: "
                    + "Cannot have have egress concurrency higher than 1 when egress type "
                    + " is set to "
                    + EgressType.SYNCHRONOUS
                    + "!");
            }
        }

        if (egressSettings.getHttpOutputType().equals(HttpOutputType.NONE)) {
            throw new IllegalArgumentException("ERROR: "
                + node.getNodeType()
                + " type node is required to "
                + "have an http output type set!");
        }
    }

    /**
     * A helper method to validate whether an node has valid egress settings.
     *
     * @param node The node to verify.
     */
    private void validateNodeHasPollSettings(final Node node) {

        logger.info("...validating node named {} has valid egress settings...",
            node.getName());

        if (Objects.isNull(node.getPollSettings())) {
            throw new IllegalArgumentException("ERROR: "
                + node.getNodeType()
                + " type node is required to "
                + "have poll settings!");
        }

        if (Objects.isNull(node.getPollSettings().getPollEndpoint())) {
            throw new IllegalArgumentException("ERROR: "
                + node.getNodeType()
                + " type node is required to "
                + "have a poll endpoint!");
        }

        if (Objects.isNull(node.getPollSettings().getPollFrequencyInMilliseconds())) {
            throw new IllegalArgumentException("ERROR: "
                + node.getNodeType()
                + " type node is required to "
                + "have a poll frequency set!");
        }
    }

    /**
     * A helper method to verify that an node has no egress settings.
     *
     * @param node The node to verify.
     */
    private void validateNodeHasNoEgressSettings(final Node node) {

        logger.info("...validating node named {} has no egress settings...",
            node.getName());

        if (Objects.nonNull(node.getEgressSettings())) {
            throw new IllegalArgumentException("ERROR: "
                + node.getNodeType()
                + " type node is required to "
                + "have no egress settings!");
        }
    }

    /**
     * Validate that an node only has a single upstream node.
     *
     * @param node The node to validate.
     */
    private void validateNodeHasASingleUpstreamNode(final Node node) {

        logger.info("...validating node named {} has only a single upstream node...",
            node.getName());

        if (node.getUpstreamNodes().size() != 1) {
            throw new IllegalArgumentException("ERROR: "
                + node.getNodeType()
                + " type node is required to have only a single upstream node!");
        }
    }

    /**
     * Validate than an node has no downstream nodes.
     *
     * @param node The node to validate.
     */
    private void validateNodeHasNoDownstreamNodes(final Node node) {

        logger.info("...validating node named {} has no downstream nodes...",
            node.getName());

        if (!node.getDownstreamNodes().isEmpty()) {
            throw new IllegalArgumentException("ERROR: "
                + node.getNodeType()
                + " type node is not allowed to "
                + "have downstream nodes!");
        }
    }

    /**
     * Validate that an node has only a single input sub schema.
     *
     * @param node The node to valiate.
     */
    private void validateNodeHasASingleInputSubSchema(final Node node) {
        logger.info("...validating node named {} has only a single input sub schema...",
            node.getName());

        if (node.getInputSubSchemas().size() != 1) {
            throw new IllegalArgumentException("ERROR: "
                + node.getNodeType()
                + " type node is required to "
                + "have a single input subschemas!");
        }
    }

    /**
     * Validate than at node has no component.
     *
     * @param node The node to validate for.
     */
    private void validateNodeHasNoComponent(final Node node) {

        logger.info("...validating node named {} has "
                + "no component...",
            node.getName());

        if (Objects.nonNull(node.getComponent())) {
            throw new IllegalArgumentException("ERROR: "
                + node.getNodeType()
                + " type node is not allowed to "
                + "have a component!");
        }
    }

    /**
     * Validate that at node has no upstream nodes.
     *
     * @param node The node to validate.
     */
    private void validateNodeHasNoUpstreamNodes(final Node node) {

        logger.info("...validating node named {} has no upstream nodes...",
            node.getName());

        if (!node.getUpstreamNodes().isEmpty()) {
            throw new IllegalArgumentException("ERROR: "
                + node.getNodeType()
                + " type node is not allowed to "
                + "have upstream nodes!");
        }
    }

    /**
     * Validate that the provided node is not a passthrough.
     *
     * @param node The node that we're validating.
     */
    private void validateNodeIsNotPassthrough(final Node node) {
        logger.info("...validating node named {} is not configured as a passthrough node...",
            node.getName());

        if (node.getNodeSettings().getIsPassthrough()) {
            throw new IllegalArgumentException("ERROR: "
                + " node is a passthrough, but this is invalid! ");
        }
    }

    /**
     * Validate that the node is a valid passthrough.
     *
     * @param node The node to validate.
     */
    private void validateNodeIsNotPassthroughWithoutDownstreamNodes(final Node node) {
        logger.info("...validating node named {} is not a passthrough "
                + " without downstream nodes...",
            node.getName());


        if (node.getNodeSettings().getIsPassthrough()
            && node.getDownstreamNodes().isEmpty()) {
            throw new IllegalArgumentException("ERROR: "
                + " node is configured as a passthrough without downstream nodes! ");
        }
    }
}
