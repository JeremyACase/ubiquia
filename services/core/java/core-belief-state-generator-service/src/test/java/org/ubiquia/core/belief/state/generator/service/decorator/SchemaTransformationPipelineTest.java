package org.ubiquia.core.belief.state.generator.service.decorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for {@link SchemaTransformationPipeline}. */
@ExtendWith(MockitoExtension.class)
class SchemaTransformationPipelineTest {

    @Mock
    private SchemaTransformer first;

    @Mock
    private SchemaTransformer second;

    @Mock
    private SchemaTransformer third;

    private SchemaTransformationPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new SchemaTransformationPipeline();
    }

    @Test
    void apply_callsEachTransformerInOrder() throws IOException {
        ReflectionTestUtils.setField(pipeline, "transformers", List.of(first, second, third));
        when(first.transform(any())).thenReturn("after-first");
        when(second.transform(any())).thenReturn("after-second");
        when(third.transform(any())).thenReturn("after-third");

        var result = pipeline.apply("input");

        assertThat(result).isEqualTo("after-third");
        var order = inOrder(first, second, third);
        order.verify(first).transform("input");
        order.verify(second).transform("after-first");
        order.verify(third).transform("after-second");
    }

    @Test
    void apply_passesOutputOfOneTransformerToNextAsInput() throws IOException {
        ReflectionTestUtils.setField(pipeline, "transformers", List.of(first, second));
        when(first.transform("raw")).thenReturn("normalized");
        when(second.transform("normalized")).thenReturn("injected");

        var result = pipeline.apply("raw");

        assertThat(result).isEqualTo("injected");
    }

    @Test
    void apply_withSingleTransformer_returnsTransformedResult() throws IOException {
        ReflectionTestUtils.setField(pipeline, "transformers", List.of(first));
        when(first.transform("input")).thenReturn("output");

        assertThat(pipeline.apply("input")).isEqualTo("output");
    }

    @Test
    void apply_withEmptyTransformerList_returnsOriginalInput() throws IOException {
        ReflectionTestUtils.setField(pipeline, "transformers", List.of());

        assertThat(pipeline.apply("unchanged")).isEqualTo("unchanged");
    }
}
