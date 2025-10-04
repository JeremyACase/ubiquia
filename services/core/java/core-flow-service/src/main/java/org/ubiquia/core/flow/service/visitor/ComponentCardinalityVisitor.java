package org.ubiquia.core.flow.service.visitor;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;

/**
 * A service that can verify cardinalities of components.
 */
@Service
public class ComponentCardinalityVisitor {

    /**
     * Determine whether the component name has a matching cardinality setting.
     *
     * @param componentName   The component we're verifying.
     * @param graphDeployment The deployment settings.
     * @return Whether there exists a matching cardinality setting for the component.
     */
    public Boolean hasMatchingCardinality(
        final String componentName,
        final GraphDeployment graphDeployment) {

        var matchingCardinality = false;

        var cardinality = graphDeployment.getCardinality();
        if (Objects.nonNull(cardinality)
            && Objects.nonNull(cardinality.getComponentSettings())) {

            var match = cardinality
                .getComponentSettings()
                .stream()
                .filter(x -> x.getName().equals(componentName))
                .findFirst();

            if (match.isPresent()) {
                matchingCardinality = true;
            }

        }
        return matchingCardinality;
    }

    /**
     * Determine whether cardinality is enabled for the component.
     *
     * @param componentName   The component we're verifying cardinality for.
     * @param graphDeployment The graph deployment settings containing the cardinality.
     * @return Whether the component is enabled or not.
     */
    public Boolean isCardinalityEnabled(
        final String componentName,
        final GraphDeployment graphDeployment) {

        var cardinalityEnabled = false;

        var cardinality = graphDeployment.getCardinality();

        var match = cardinality
            .getComponentSettings()
            .stream()
            .filter(x -> x.getName().equals(componentName))
            .findFirst();

        cardinalityEnabled = match.get().getEnabled();

        return cardinalityEnabled;
    }
}
