package org.ubiquia.core.flow.service.manager;


import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.service.decorator.override.ComponentOverrideDecorator;
import org.ubiquia.core.flow.service.k8s.ComponentOperator;
import org.ubiquia.core.flow.service.mapper.ComponentDtoMapper;
import org.ubiquia.core.flow.service.mapper.GraphDtoMapper;

/**
 * This is a service that will manage agents for Ubiquia at runtime.
 */
@Component
public class ComponentManager {

    private static final Logger logger = LoggerFactory.getLogger(ComponentManager.class);
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
            logger.info("...deploying component: {}",
                componentEntity.getName());

            var component = this.componentDtoMapper.map(componentEntity);

            // Set the graph here as the DTO Mapper will not.
            component.setGraph(this.graphDtoMapper.map(graphEntity));

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
        logger.info("...completed deploying agents.");
    }
}
