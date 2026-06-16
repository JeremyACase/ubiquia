package org.ubiquia.core.belief.state.generator.service.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

/** Tests for {@link K8sLabelBuilder}. */
class K8sLabelBuilderTest {

    @Test
    void from_copiesBaseLabels_doesNotMutateOriginal() {
        var base = new HashMap<String, String>();
        base.put("app", "ubiquia");

        var built = K8sLabelBuilder.from(base).build();
        built.put("injected", "value");

        assertThat(base).doesNotContainKey("injected");
    }

    @Test
    void remove_removesOnlySpecifiedKeys() {
        var base = new HashMap<String, String>();
        base.put("a", "1");
        base.put("b", "2");
        base.put("c", "3");

        var result = K8sLabelBuilder.from(base).remove("a", "c").build();

        assertThat(result)
            .doesNotContainKey("a")
            .doesNotContainKey("c")
            .containsEntry("b", "2");
    }

    @Test
    void remove_withUnknownKey_doesNotThrow() {
        var base = new HashMap<String, String>();
        base.put("x", "1");

        var result = K8sLabelBuilder.from(base).remove("nonexistent").build();

        assertThat(result).containsEntry("x", "1");
    }

    @Test
    void put_addsNewEntry() {
        var result = K8sLabelBuilder.from(new HashMap<>()).put("env", "prod").build();

        assertThat(result).containsEntry("env", "prod");
    }

    @Test
    void put_overwritesExistingEntry() {
        var base = new HashMap<String, String>();
        base.put("component", "old");

        var result = K8sLabelBuilder.from(base).put("component", "new").build();

        assertThat(result).containsEntry("component", "new");
    }

    @Test
    void withBeliefState_stampsBeliefStateAndManagedByLabels() {
        var result = K8sLabelBuilder.from(new HashMap<>())
            .withBeliefState("my-bs")
            .build();

        assertThat(result)
            .containsEntry("belief-state", "my-bs")
            .containsEntry("app.kubernetes.io/managed-by", "ubiquia");
    }

    @Test
    void withBeliefState_overwritesExistingManagedByLabel() {
        var base = new HashMap<String, String>();
        base.put("app.kubernetes.io/managed-by", "helm");

        var result = K8sLabelBuilder.from(base).withBeliefState("bs").build();

        assertThat(result).containsEntry("app.kubernetes.io/managed-by", "ubiquia");
    }

    @Test
    void fullChain_serviceLabels_matchExpected() {
        var base = new HashMap<String, String>();
        base.put("app", "ubiquia");
        base.put("component", "core");
        base.put("app.kubernetes.io/managed-by", "helm");

        var result = K8sLabelBuilder.from(base)
            .remove("component", "app.kubernetes.io/managed-by")
            .put("domain", "finance")
            .withBeliefState("bs-finance")
            .build();

        assertThat(result)
            .containsEntry("app", "ubiquia")
            .containsEntry("domain", "finance")
            .containsEntry("belief-state", "bs-finance")
            .containsEntry("app.kubernetes.io/managed-by", "ubiquia")
            .doesNotContainKey("component");
    }

    @Test
    void fullChain_deploymentLabels_matchExpected() {
        var base = new HashMap<String, String>();
        base.put("app", "ubiquia");
        base.put("component", "core");
        base.put("app.kubernetes.io/managed-by", "helm");

        var result = K8sLabelBuilder.from(base)
            .remove("component", "app.kubernetes.io/managed-by")
            .put("component", "retail")
            .withBeliefState("bs-retail")
            .build();

        assertThat(result)
            .containsEntry("app", "ubiquia")
            .containsEntry("component", "retail")
            .containsEntry("belief-state", "bs-retail")
            .containsEntry("app.kubernetes.io/managed-by", "ubiquia");
    }
}
