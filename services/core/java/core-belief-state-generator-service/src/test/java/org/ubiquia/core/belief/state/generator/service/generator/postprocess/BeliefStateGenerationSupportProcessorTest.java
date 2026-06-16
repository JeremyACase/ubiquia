package org.ubiquia.core.belief.state.generator.service.generator.postprocess;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@code SupportTemplate} enum inside
 * {@link BeliefStateGenerationSupportProcessor}.
 *
 * <p>These tests guard the enum registry from silent regressions — a new template with bad paths
 * or wrong {@code requiresReplacement} flag would be caught here before it reaches generation.
 */
class BeliefStateGenerationSupportProcessorTest {

    @Test
    void supportTemplate_allValuesHaveNonBlankResourcePath() {
        for (var template : BeliefStateGenerationSupportProcessor.SupportTemplate.values()) {
            assertThat(template.resourcePath)
                .as("SupportTemplate.%s.resourcePath should not be blank", template.name())
                .isNotBlank();
        }
    }

    @Test
    void supportTemplate_allValuesHaveNonBlankDestinationPath() {
        for (var template : BeliefStateGenerationSupportProcessor.SupportTemplate.values()) {
            assertThat(template.destinationPath)
                .as("SupportTemplate.%s.destinationPath should not be blank", template.name())
                .isNotBlank();
        }
    }

    @Test
    void supportTemplate_onlyApplicationYamlRequiresReplacement() {
        var requiresReplacement = Arrays.stream(
                BeliefStateGenerationSupportProcessor.SupportTemplate.values())
            .filter(t -> t.requiresReplacement)
            .collect(Collectors.toList());

        assertThat(requiresReplacement)
            .as("Exactly one template should require placeholder replacement")
            .hasSize(1)
            .extracting(t -> t.name())
            .containsExactly("APPLICATION_YAML");
    }

    @Test
    void supportTemplate_hasExpectedCount() {
        // Guard: a new template added to the enum should be reviewed and tested.
        assertThat(BeliefStateGenerationSupportProcessor.SupportTemplate.values())
            .hasSize(5);
    }

    @Test
    void supportTemplate_resourcePathsAreClasspathRelative() {
        for (var template : BeliefStateGenerationSupportProcessor.SupportTemplate.values()) {
            assertThat(template.resourcePath)
                .as("SupportTemplate.%s.resourcePath should be a relative classpath path",
                    template.name())
                .doesNotStartWith("/")
                .startsWith("template/");
        }
    }

    @Test
    void supportTemplate_destinationPathsTargetGeneratedDirectory() {
        for (var template : BeliefStateGenerationSupportProcessor.SupportTemplate.values()) {
            assertThat(template.destinationPath)
                .as("SupportTemplate.%s.destinationPath should be under generated/", template.name())
                .startsWith("generated/");
        }
    }
}
