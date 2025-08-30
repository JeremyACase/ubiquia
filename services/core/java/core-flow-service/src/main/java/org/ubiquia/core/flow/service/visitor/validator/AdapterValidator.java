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
import org.ubiquia.common.model.ubiquia.embeddable.SubSchema;
import org.ubiquia.common.model.ubiquia.entity.AdapterEntity;
import org.ubiquia.common.model.ubiquia.enums.EgressType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;

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
     * @param adapterEntities The adapters to validate.
     * @throws Exception Exceptions from attempting to validate any adapter.
     */
    public void tryValidate(final List<AdapterEntity> adapterEntities) throws Exception {
        logger.info("...validating adapters...");
        for (var adapter : adapterEntities) {
            this.tryValidate(adapter);
        }
        logger.info("...validated adapters...");
    }

    /**
     * Attempt to validate an adapter, mostly depending on its type.
     *
     * @param adapterEntity The adapter to validate.
     * @throws Exception Exceptions from validating the adapter.
     */
    public void tryValidate(final AdapterEntity adapterEntity) throws Exception {
        switch (adapterEntity.getAdapterType()) {
            case EGRESS: {
                this.tryValidateEgressAdapter(adapterEntity);
            }
            break;

            case MERGE: {
                this.tryValidateMergeAdapter(adapterEntity);
            }
            break;

            case POLL: {
                this.tryValidatePollAdapter(adapterEntity);
            }
            break;

            case PUBLISH: {
                this.tryValidatePublishAdapter(adapterEntity);
            }
            break;

            case PUSH: {
                this.tryValidatePushAdapter(adapterEntity);
            }
            break;

            case QUEUE: {
                this.tryValidateQueueAdapter(adapterEntity);
            }
            break;

            case HIDDEN: {
                this.tryValidateHiddenAdapter(adapterEntity);
            }
            break;

            case SUBSCRIBE: {
                this.tryValidateSubscribeAdapter(adapterEntity);
            }
            break;

            default: {
                throw new Exception("ERROR: Unrecognized adapter type: "
                    + adapterEntity.getAdapterType());
            }
        }
    }

    /**
     * Determine if two adapters having matching output and input schemas.
     *
     * @param outputAdapterEntity The output adapter.
     * @param inputAdapterEntity  The input adapter.
     * @return Whether or not the adapters have matching output/input schemas.
     */
    public Boolean haveMatchingInputOutput(
        final AdapterEntity outputAdapterEntity,
        final AdapterEntity inputAdapterEntity) {

        var hasMatchingInputOutput = false;

        if (Objects.nonNull(outputAdapterEntity.getOutputSubSchema())) {

            var matchingInputSubSchema = this.tryGetMatchingInputSubSchema(
                outputAdapterEntity,
                inputAdapterEntity);

            if (matchingInputSubSchema.isEmpty()) {
                logger.info("Could not find a matching sub schema between output adapter {}"
                        + " and input  {}",
                    outputAdapterEntity.getName(),
                    inputAdapterEntity.getName());
            } else {
                hasMatchingInputOutput = this.subSchemaValidator.areEquivalent(
                    matchingInputSubSchema.get(),
                    outputAdapterEntity.getOutputSubSchema());
            }
        }

        return hasMatchingInputOutput;
    }

    /**
     * Attempt to validate that an egress adapter has appropriate configurations.
     *
     * @param adapterEntity The adapter entity to validate.
     */
    public void tryValidateEgressAdapter(final AdapterEntity adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
        this.validateAdapterHasNoComponent(adapterEntity);
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
    public void tryValidateMergeAdapter(final AdapterEntity adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
        this.validateAdapterHasEgressSettings(adapterEntity);
        this.validateAdapterIsNotPassthroughWithoutDownstreamAdapters(adapterEntity);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a poll adapter has appropriate configurations.
     *
     * @param adapterEntity The adapter entity to validate.
     */
    public void tryValidatePollAdapter(final AdapterEntity adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
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
    public void tryValidatePublishAdapter(final AdapterEntity adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
        this.validateAdapterHasNoComponent(adapterEntity);
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
    public void tryValidatePushAdapter(final AdapterEntity adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
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
    public void tryValidateQueueAdapter(final AdapterEntity adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
        this.validateAdapterHasNoComponent(adapterEntity);
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
    public void tryValidateHiddenAdapter(final AdapterEntity adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
        this.validateAdapterHasASingleUpstreamAdapter(adapterEntity);
        this.validateAdapterHasEgressSettings(adapterEntity);
        this.validateAdapterHasASingleInputSubSchema(adapterEntity);
        this.validateAdapterIsNotPassthroughWithoutDownstreamAdapters(adapterEntity);
        logger.info("...validated...");
    }

    /**
     * Attempt to validate that a subscribe adapter has appropriate configurations.
     *
     * @param adapterEntity The adapter entity to validate.
     */
    public void tryValidateSubscribeAdapter(final AdapterEntity adapterEntity) {
        this.tryValidateAdapterSettings(adapterEntity);
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
     * @param outputAdapterEntity The output adapter.
     * @param inputAdapterEntity  The input adapter.
     * @return A matching subschema, if applicable.
     */
    private Optional<SubSchema> tryGetMatchingInputSubSchema(
        final AdapterEntity outputAdapterEntity,
        final AdapterEntity inputAdapterEntity) {

        var outputSubSchema = outputAdapterEntity.getOutputSubSchema();
        var match = inputAdapterEntity.getInputSubSchemas()
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
    private void tryValidateAdapterSettings(final AdapterEntity adapterEntity) {
        logger.info("Validating {} adapter for graph {} named {}...",
            adapterEntity.getAdapterType(),
            adapterEntity.getGraph().getName(),
            adapterEntity.getName());

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
    private void validateAdapterHasEgressSettings(final AdapterEntity adapterEntity) {

        logger.info("...validating adapter for graph {} named {} has "
                + "valid egress settings...",
            adapterEntity.getGraph().getName(),
            adapterEntity.getName());

        var egressSettings = adapterEntity.getEgressSettings();
        if (Objects.isNull(egressSettings)) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is required to "
                + "have egress settings!");
        }

        if (egressSettings.getEgressType().equals(EgressType.NONE)) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is required to "
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
                + adapterEntity.getAdapterType()
                + " type adapter is required to "
                + "have an http output type set!");
        }
    }

    /**
     * A helper method to validate whether an adapter has valid egress settings.
     *
     * @param adapterEntity The adapter to verify.
     */
    private void validateAdapterHasPollSettings(final AdapterEntity adapterEntity) {

        logger.info("...validating adapter for graph {} named {} has "
                + "valid egress settings...",
            adapterEntity.getGraph().getName(),
            adapterEntity.getName());

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
    private void validateAdapterHasNoEgressSettings(final AdapterEntity adapterEntity) {

        logger.info("...validating adapter named {} for graph {} has "
                + "no egress settings...",
            adapterEntity.getName(),
            adapterEntity.getGraph().getName());

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
    private void validateAdapterHasASingleUpstreamAdapter(final AdapterEntity adapterEntity) {

        logger.info("...validating adapter named {} for graph {} has "
                + "only a single upstream adapter...",
            adapterEntity.getName(),
            adapterEntity.getGraph().getName());

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
    private void validateAdapterHasNoDownstreamAdapters(final AdapterEntity adapterEntity) {

        logger.info("...validating adapter named {} for graph {} has "
                + "no downstream adapters...",
            adapterEntity.getName(),
            adapterEntity.getGraph().getName());

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
    private void validateAdapterHasASingleInputSubSchema(final AdapterEntity adapterEntity) {
        logger.info("...validating adapter for graph {} named {} has "
                + "only a single input sub schema...",
            adapterEntity.getGraph().getName(),
            adapterEntity.getName());

        if (adapterEntity.getInputSubSchemas().size() != 1) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is required to "
                + "have a single input subschemas!");
        }
    }

    /**
     * Validate than at adapter has no component.
     *
     * @param adapterEntity The adapter to validate for.
     */
    private void validateAdapterHasNoComponent(final AdapterEntity adapterEntity) {

        logger.info("...validating adapter for graph {} has "
                + "no component...",
            adapterEntity.getGraph().getName());

        if (Objects.nonNull(adapterEntity.getComponent())) {
            throw new IllegalArgumentException("ERROR: "
                + adapterEntity.getAdapterType()
                + " type adapter is not allowed to "
                + "have a component!");
        }
    }

    /**
     * Validate that at adapter has no upstream adapters.
     *
     * @param adapterEntity The adapter to validate.
     */
    private void validateAdapterHasNoUpstreamAdapters(final AdapterEntity adapterEntity) {

        logger.info("...validating adapter named {} for graph {} has "
                + "no upstream adapters...",
            adapterEntity.getName(),
            adapterEntity.getGraph().getName());

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
    private void validateAdapterIsNotPassthrough(final AdapterEntity adapterEntity) {
        logger.info("...validating adapter named {} for graph {} has "
                + "is not configured as a passthrough adapter...",
            adapterEntity.getName(),
            adapterEntity.getGraph().getName());

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
    private void validateAdapterIsNotPassthroughWithoutDownstreamAdapters(final AdapterEntity adapterEntity) {
        logger.info("...validating adapter named {} for graph {} is not a passthrough "
                + " without downstream adapters...",
            adapterEntity.getName(),
            adapterEntity.getGraph().getName());


        if (adapterEntity.getAdapterSettings().getIsPassthrough()
            && adapterEntity.getDownstreamAdapters().isEmpty()) {
            throw new IllegalArgumentException("ERROR: "
                + " adapter is configured as a passthrough without downstream adapters! ");
        }
    }
}
