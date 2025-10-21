package org.ubiquia.core.flow.service.manager;


import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ubiquia.common.library.implementation.service.mapper.ComponentDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.GraphDtoMapper;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.entity.ComponentEntity;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.service.decorator.override.ComponentOverrideDecorator;
import org.ubiquia.core.flow.service.k8s.ComponentOperator;
import org.ubiquia.core.flow.service.visitor.ComponentCardinalityVisitor;

/**
 * This is a service that will manage agents for Ubiquia at runtime.
 */
@Component
public class ComponentManager {

    private static final Logger logger = LoggerFactory.getLogger(ComponentManager.class);
    @Autowired
    private ComponentCardinalityVisitor componentCardinalityVisitor;
    @Autowired(required = false)
    private ComponentOperator componentOperator;
    @Autowired
    private ComponentDtoMapper componentDtoMapper;
    @Autowired
    private ComponentOverrideDecorator componentOverrideDecorator;
    @Autowired
    private GraphRepository graphRepository;
    @Autowired
    private GraphDtoMapper graphDtoMapper;

    /**
     * Attempt to deploy agents for a provided graph deployment.
     *
     * @param graphDeployment The graph deployment to deploy componnets for.
     * @throws JsonProcessingException Exceptions from parsing stringified objects.
     * @throws IllegalAccessException  Exceptions from trying to override values.
     */
    @Transactional
    public void tryDeployComponentsFor(final GraphDeployment graphDeployment)
        throws JsonProcessingException,
        IllegalAccessException {

        logger.info("Attempting to deploy a list of components for graph {} with settings "
                + "{}...",
            graphDeployment.getName(),
            graphDeployment.getGraphSettings());

        var graphRecord = this
            .graphRepository
            .findByNameAndVersionMajorAndVersionMinorAndVersionPatch(
                graphDeployment.getName(),
                graphDeployment.getVersion().getMajor(),
                graphDeployment.getVersion().getMinor(),
                graphDeployment.getVersion().getPatch());

        if (graphRecord.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Could not find graph: " + graphDeployment);
        }
        var graphEntity = graphRecord.get();

        for (var componentEntity : graphEntity.getComponents()) {
            logger.info("...attempting to deploy component: {}",
                componentEntity.getName());

            var component = this.componentDtoMapper.map(componentEntity);

            // Do this explicitly here to prevent the DTO Mapper from inadvertently egressing
            // nested objects by our controllers
            var graph = this.graphDtoMapper.map(graphEntity);
            component.setGraph(graph);

            if (this.componentCardinalityVisitor.hasMatchingCardinality(
                component.getName(),
                graphDeployment)) {

                if (this.componentCardinalityVisitor.isCardinalityEnabled(
                    component.getName(),
                    graphDeployment)) {

                    this.tryDeployComponent(component, componentEntity, graphDeployment);
                } else {
                    logger.info("...cardinality disabled for component: {} "
                            + "...not deploying...",
                        component.getName());
                }
            } else {
                this.tryDeployComponent(component, componentEntity, graphDeployment);
            }
        }
        logger.info("...completed deploying agents.");
    }

    /**
     * Attempt to deploy the component.
     *
     * @param component       The component DTO we're deploying.
     * @param componentEntity The database record of the component.
     * @param graphDeployment The deployment and associated settings.
     * @throws JsonProcessingException Exceptions from JSON parsing.
     * @throws IllegalAccessException  Exceptions from overriding.
     */
    @Transactional
    private void tryDeployComponent(
        final org.ubiquia.common.model.ubiquia.dto.Component component,
        final ComponentEntity componentEntity,
        final GraphDeployment graphDeployment)
        throws JsonProcessingException, IllegalAccessException {

        this.componentOverrideDecorator.tryOverrideBaselineValues(
            component,
            componentEntity.getOverrideSettings().stream().toList(),
            graphDeployment);

        if (component.getComponentType().equals(ComponentType.POD)) {
            if (Objects.isNull(this.componentOperator)) {
                throw new IllegalArgumentException("ERROR: Kubernetes is not enabled and "
                    + "the agent is a POD type!"
                    + " Cannot deploy POD components without Kubernetes!");
            } else {
                this.componentOperator.tryDeployComponent(component);
            }
        } else {
            logger.info("...agent is not a POD type; not deploying...");
        }
    }
}
