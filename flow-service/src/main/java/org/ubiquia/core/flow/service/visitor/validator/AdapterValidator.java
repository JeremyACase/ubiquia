package org.ubiquia.core.flow.service.visitor.validator;


import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.model.embeddable.SubSchema;
import org.ubiquia.core.flow.model.entity.Adapter;
import org.ubiquia.core.flow.model.enums.EgressType;

/**
 * A service dedicated to validating adapters.
 */
@Service
@Transactional
public class AdapterValidator {

    private static final Logger logger = LoggerFactory.getLogger(AdapterValidator.class);
    @Autowired
    private SubSchemaValidator subSchemaValidator;

    /**
     * Try to validate a list of adapters.
     *
     * @param adapters The adapters to validate.
     * @throws Exception Exceptions from attempting to validate any adapter.
     */
    public void tryValidate(final List<Adapter> adapters) throws Exception {
        logger.info("...validating adapters...");
        for (var adapter : adapters) {
            this.tryValidate(adapter);
        }
        logger.info("...validated adapters...");
    }

    /**
     * Attempt to validate an adapter, mostly depending on its type.
     *
     * @param adapter The adapter to validate.
     * @throws Exception Exceptions from validating the adapter.
     */
    public void tryValidate(final Adapter adapter) throws Exception {
        switch (adapter.getAdapterType()) {
            case EGRESS: {
                this.tryValidateEgressAdapter(adapter);
            }
            break;

            case MERGE: {
                this.tryValidateMergeAdapter(adapter);
            }
            break;

            case POLL: {
                this.tryValidatePollAdapter(adapter);
            }
            break;

            case PUBLISH: {
                this.tryValidatePublishAdapter(adapter);
            }
            break;

            case PUSH: {
                this.tryValidatePushAdapter(adapter);
            }
            break;

            case QUEUE: {
                this.tryValidateQueueAdapter(adapter);
            }
            break;

            case HIDDEN: {
                this.tryValidateHiddenAdapter(adapter);
            }
            break;

            case SUBSCRIBE: {
                this.tryValidateSubscribeAdapter(adapter);
            }
            break;

            default: {
                throw new Exception("ERROR: Unrecognized adapter type: "
                    + adapter.getAdapterType());
            }
        }
    }

    /**
     * Determine if two adapters having matching output and input schemas.
     *
     * @param outputAdapter The output adapter.
     * @param inputAdapter  The input adapter.
     * @return Whether or not the adapters have matching output/input schemas.
     */
    public Boolean haveMatchingInputOutput(
        final Adapter outputAdapter,
        final Adapter inputAdapter) {

        var hasMatchingInputOutput = false;

        if (Objects.nonNull(outputAdapter.getOutputSubSchema())) {

            var matchingInputSubSchema = this.tryGetMatchingInputSubSchema(
                outputAdapter,
                inputAdapter);

            if (matchingInputSubSchema.isEmpty()) {
                logger.info("Could not find a matching sub schema between output adapter {}"
                        + " and input  {}",
                    outputAdapter.getAdapterName(),
                    inputAdapter.getAdapterName());
            } else {
                hasMatchingInputOutput = this.subSchemaValidator.areEquivalent(
                    matchingInputSubSchema.get(),
                    outputAdapter.getOutputSubSchema());
            }
        }

        return hasMatchingInputOutput;
    }

    /**
     * Attempt to validate that an egress adapter has appropriate configurations.
     *
     * @param adapterEntity The adapter entity to validate.
     */
    public void tryValidateEgressAdapter(final Adapter adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
        this.validateAdapterHasNoDataTransform(adapterEntity);
        this.validateAdapterHasNoDownstreamAdapters(adapterEntity);
        this.validateAdapterHasASingleUpstreamAdapter(adapterEntity);
        this.validateAdapterHasEgressSettings(adapterEntity);
        this.validateAdapterIsNotPassthrough(adapterEntity);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a merge adapter has appropriate configurations.
     *
     * @param adapterEntity The adapter entity to validate.
     */
    public void tryValidateMergeAdapter(final Adapter adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
        this.validateAdapterIsNotPassthroughWithADataTransform(adapterEntity);
        this.validateAdapterHasEgressSettings(adapterEntity);
        this.validateAdapterIsNotPassthroughWithoutDownstreamAdapters(adapterEntity);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a poll adapter has appropriate configurations.
     *
     * @param adapterEntity The adapter entity to validate.
     */
    public void tryValidatePollAdapter(final Adapter adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
        this.validateAdapterIsNotPassthroughWithADataTransform(adapterEntity);
        this.validateAdapterHasNoUpstreamAdapters(adapterEntity);
        this.validateAdapterHasPollSettings(adapterEntity);
        this.validateAdapterHasASingleInputSubSchema(adapterEntity);
        this.validateAdapterIsNotPassthroughWithoutDownstreamAdapters(adapterEntity);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a publish adapter has appropriate configurations.
     *
     * @param adapterEntity The adapter entity to validate.
     */
    public void tryValidatePublishAdapter(final Adapter adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
        this.validateAdapterHasNoDataTransform(adapterEntity);
        this.validateAdapterHasASingleUpstreamAdapter(adapterEntity);
        this.validateAdapterHasNoDownstreamAdapters(adapterEntity);
        this.validateAdapterHasNoEgressSettings(adapterEntity);
        this.validateAdapterHasASingleInputSubSchema(adapterEntity);
        this.validateAdapterIsNotPassthrough(adapterEntity);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a push adapter has appropriate configurations.
     *
     * @param adapterEntity The adapter entity to validate.
     */
    public void tryValidatePushAdapter(final Adapter adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
        this.validateAdapterIsNotPassthroughWithADataTransform(adapterEntity);
        this.validateAdapterHasNoUpstreamAdapters(adapterEntity);
        this.validateAdapterHasEgressSettings(adapterEntity);
        this.validateAdapterHasASingleInputSubSchema(adapterEntity);
        this.validateAdapterIsNotPassthroughWithoutDownstreamAdapters(adapterEntity);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a queue adapter has appropriate configurations.
     *
     * @param adapterEntity The adapter entity to validate.
     */
    public void tryValidateQueueAdapter(final Adapter adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
        this.validateAdapterHasNoDataTransform(adapterEntity);
        this.validateAdapterHasASingleUpstreamAdapter(adapterEntity);
        this.validateAdapterHasNoDownstreamAdapters(adapterEntity);
        this.validateAdapterHasNoEgressSettings(adapterEntity);
        this.validateAdapterHasASingleInputSubSchema(adapterEntity);
        this.validateAdapterIsNotPassthrough(adapterEntity);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a hidden adapter has appropriate configurations.
     *
     * @param adapterEntity The adapter entity to validate.
     */
    public void tryValidateHiddenAdapter(final Adapter adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
        this.validateAdapterIsNotPassthroughWithADataTransform(adapterEntity);
        this.validateAdapterHasASingleUpstreamAdapter(adapterEntity);
        this.validateAdapterHasEgressSettings(adapterEntity);
        this.validateAdapterHasASingleUpstreamAdapter(adapterEntity);
        this.validateAdapterHasASingleInputSubSchema(adapterEntity);
        this.validateAdapterIsNotPassthroughWithoutDownstreamAdapters(adapterEntity);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a subscribe adapter has appropriate configurations.
     *
     * @param adapterEntity The adapter entity to validate.
     */
    public void tryValidateSubscribeAdapter(final Adapter adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
        this.validateAdapterIsNotPassthroughWithADataTransform(adapterEntity);
        this.validateAdapterHasEgressSettings(adapterEntity);
        this.validateAdapterHasNoUpstreamAdapters(adapterEntity);
        this.validateAdapterHasASingleInputSubSchema(adapterEntity);
        this.validateAdapterIsNotPassthroughWithoutDownstreamAdapters(adapterEntity);
        logger.info("...validated...");
    }

    /**
     * Helper method that attempts to find a matching input sub schema for a pair of output/input
     * adapters.
     *
     * @param outputAdapter The output adapter.
     * @param inputAdapter  The input adapter.
     * @return A matching subschema, if applicable.
     */
    private Optional<SubSchema> tryGetMatchingInputSubSchema(
        final Adapter outputAdapter,
        final Adapter inputAdapter) {

        var outputSubSchema = outputAdapter.getOutputSubSchema();
        var match = inputAdapter.getInputSubSchemas()
            .stream()
            .filter(x -> x.getModelName().equals(outputSubSchema.getModelName()))
            .findFirst();
        return match;
    }

    /**
     * A helper method for verifying adapters.
     *
     * @param adapterEntity The adapter to verify.
     */
    private void tryValidateAdapterSettings(final Adapter adapterEntity) {
        logger.info("Validating {} adapter for graph {} named {}...",
            adapterEntity.getAdapterType(),
            adapterEntity.getGraph().getGraphName(),
            adapterEntity.getAdapterName());

        var adapterSettings = adapterEntity.getAdapterSettings();
        if (Objects.nonNull(adapterSettings.getBackpressurePollFrequencyMilliseconds())
            && adapterSettings.getBackpressurePollFrequencyMilliseconds()
            > TimeUnit.MINUTES.toMillis(1)) {
            throw new IllegalArgumentException("ERROR: Backpressure polling frequency must be"
                + " less than a minute...");
        }
    }

    /**
     * A helper method to validate whether an adapter has valid egress settings.
     *
     * @param adapterEntity The adapter to verify.
     */
    private void validateAdapterHasEgressSettings(final Adapter adapterEntity) {

        logger.info("...validating adapter for graph {} named {} has "
                + "valid egress settings...",
            adapterEntity.getGraph().getGraphName(),
            adapterEntity.getAdapterName());

        if (Objects.isNull(adapterEntity.getEgressSettings())) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is required to "
                + "have egress settings!");
        }

        if (adapterEntity.getEgressSettings().getEgressType().equals(EgressType.NONE)) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is required to "
                + "have an egress type set!");
        }
    }

    /**
     * A helper method to validate whether an adapter has valid egress settings.
     *
     * @param adapterEntity The adapter to verify.
     */
    private void validateAdapterHasPollSettings(final Adapter adapterEntity) {

        logger.info("...validating adapter for graph {} named {} has "
                + "valid egress settings...",
            adapterEntity.getGraph().getGraphName(),
            adapterEntity.getAdapterName());

        if (Objects.isNull(adapterEntity.getPollSettings())) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is required to "
                + "have poll settings!");
        }

        if (Objects.isNull(adapterEntity.getPollSettings().getPollEndpoint())) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is required to "
                + "have a poll endpoint!");
        }

        if (Objects.isNull(adapterEntity.getPollSettings().getPollFrequencyInMilliseconds())) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is required to "
                + "have a poll frequency set!");
        }
    }

    /**
     * A helper method to verify that an adapter has no egress settings.
     *
     * @param adapterEntity The adapter to verify.
     */
    private void validateAdapterHasNoEgressSettings(final Adapter adapterEntity) {

        logger.info("...validating adapter named {} for graph {} has "
                + "no egress settings...",
            adapterEntity.getAdapterName(),
            adapterEntity.getGraph().getGraphName());

        if (Objects.nonNull(adapterEntity.getEgressSettings())) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is required to "
                + "have no egress settings!");
        }
    }

    /**
     * Validate that an adapter only has a single upstream adapter.
     *
     * @param adapterEntity The adapter to validate.
     */
    private void validateAdapterHasASingleUpstreamAdapter(final Adapter adapterEntity) {

        logger.info("...validating adapter named {} for graph {} has "
                + "only a single upstream adapter...",
            adapterEntity.getAdapterName(),
            adapterEntity.getGraph().getGraphName());

        if (adapterEntity.getUpstreamAdapters().size() != 1) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is required to have only a single upstream adapter!");
        }
    }

    /**
     * Validate than an adapter has no downstream adapters.
     *
     * @param adapterEntity The adapter to validate.
     */
    private void validateAdapterHasNoDownstreamAdapters(final Adapter adapterEntity) {

        logger.info("...validating adapter named {} for graph {} has "
                + "no downstream adapters...",
            adapterEntity.getAdapterName(),
            adapterEntity.getGraph().getGraphName());

        if (!adapterEntity.getDownstreamAdapters().isEmpty()) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is not allowed to "
                + "have downstream adapters!");
        }
    }

    /**
     * Validate that an adapter has only a single input sub schema.
     *
     * @param adapterEntity The adapter to valiate.
     */
    private void validateAdapterHasASingleInputSubSchema(final Adapter adapterEntity) {
        logger.info("...validating adapter for graph {} named {} has "
                + "only a single input sub schema...",
            adapterEntity.getGraph().getGraphName(),
            adapterEntity.getAdapterName());

        if (adapterEntity.getInputSubSchemas().size() != 1) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is required to "
                + "have a single input subschemas!");
        }
    }

    /**
     * Validate than at adapter has no data transform.
     *
     * @param adapterEntity The adapter to validate for.
     */
    private void validateAdapterHasNoDataTransform(final Adapter adapterEntity) {

        logger.info("...validating adapter for graph {} has "
                + "no data transform...",
            adapterEntity.getGraph().getGraphName());

        if (Objects.nonNull(adapterEntity.getAgent())) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is not allowed to "
                + "have a data transform!");
        }
    }

    /**
     * Validate that an adapter only a single data transform.
     *
     * @param adapterEntity The adapter to validate.
     */
    private void validateAdapterIsNotPassthroughWithADataTransform(final Adapter adapterEntity) {

        if (adapterEntity.getAdapterSettings().getIsPassthrough()) {
            logger.info("...adapter named {} for graph {} has "
                    + " is configured as passthrough, not verifying downstream transform...",
                adapterEntity.getAdapterName(),
                adapterEntity.getGraph().getGraphName());
        } else {
            logger.info("...validating adapter named {} for graph {} has "
                    + "a data transform...",
                adapterEntity.getAdapterName(),
                adapterEntity.getGraph().getGraphName());

            if (Objects.isNull(adapterEntity.getAgent())) {
                throw new IllegalArgumentException("ERROR: "
                    + adapterEntity.getAdapterType()
                    + " type adapter has to have "
                    + "a data transform!");
            }
        }
    }

    /**
     * Validate that at adapter has no upstream adapters.
     *
     * @param adapterEntity The adapter to validate.
     */
    private void validateAdapterHasNoUpstreamAdapters(final Adapter adapterEntity) {

        logger.info("...validating adapter named {} for graph {} has "
                + "no upstream adapters...",
            adapterEntity.getAdapterName(),
            adapterEntity.getGraph().getGraphName());

        if (!adapterEntity.getUpstreamAdapters().isEmpty()) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is not allowed to "
                + "have upstream adapters!");
        }
    }

    /**
     * Validate that the provided adapter is not a passthrough.
     *
     * @param adapterEntity The adapter entity that we're validating.
     */
    private void validateAdapterIsNotPassthrough(final Adapter adapterEntity) {
        logger.info("...validating adapter named {} for graph {} has "
                + "is not configured as a passthrough adapter...",
            adapterEntity.getAdapterName(),
            adapterEntity.getGraph().getGraphName());

        if (adapterEntity.getAdapterSettings().getIsPassthrough()) {
            throw new IllegalArgumentException("ERROR: "
                + " adapter is a passthrough, but this is invalid! ");
        }
    }

    /**
     * Validate that the adapter is a valid passthrough.
     *
     * @param adapterEntity The adapter to validate.
     */
    private void validateAdapterIsNotPassthroughWithoutDownstreamAdapters(final Adapter adapterEntity) {
        logger.info("...validating adapter named {} for graph {} is not a passthrough "
                + " without downstream adapters...",
            adapterEntity.getAdapterName(),
            adapterEntity.getGraph().getGraphName());


        if (adapterEntity.getAdapterSettings().getIsPassthrough()
            && adapterEntity.getDownstreamAdapters().isEmpty()) {
            throw new IllegalArgumentException("ERROR: "
                + " adapter is configured as a passthrough without downstream adapters! ");
        }
    }
}
