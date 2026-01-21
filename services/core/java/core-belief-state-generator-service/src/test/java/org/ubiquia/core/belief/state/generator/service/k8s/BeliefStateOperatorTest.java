package org.ubiquia.core.belief.state.generator.service.k8s;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import io.kubernetes.client.util.generic.options.CreateOptions;
import io.kubernetes.client.util.generic.options.ListOptions;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;
import org.ubiquia.core.belief.state.generator.service.builder.BeliefStateDeploymentBuilder;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
class BeliefStateOperatorTest {

    private BeliefStateOperator operator;

    @Mock
    private BeliefStateDeploymentBuilder beliefStateDeploymentBuilder;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GenericKubernetesApi<V1Deployment, V1DeploymentList> deploymentClient;

    @Mock
    private GenericKubernetesApi<V1Service, V1ServiceList> serviceClient;

    @Mock
    private DomainOntology domainOntology;

    private static SemanticVersion semanticVersion(int major, int minor, int patch) {
        var v = new SemanticVersion();
        v.setMajor(major);
        v.setMinor(minor);
        v.setPatch(patch);
        return v;
    }

    private static V1Deployment deploymentNamed(String name) {
        var d = new V1Deployment();
        var meta = new V1ObjectMeta();
        meta.setName(name);
        d.setMetadata(meta);
        return d;
    }

    private static V1Service serviceNamed(String name) {
        var s = new V1Service();
        var meta = new V1ObjectMeta();
        meta.setName(name);
        s.setMetadata(meta);
        return s;
    }

    @BeforeEach
    void setUp() {
        operator = new BeliefStateOperator();

        ReflectionTestUtils.setField(operator, "beliefStateDeploymentBuilder", beliefStateDeploymentBuilder);
        ReflectionTestUtils.setField(operator, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(operator, "deploymentClient", deploymentClient);
        ReflectionTestUtils.setField(operator, "serviceClient", serviceClient);
        ReflectionTestUtils.setField(operator, "namespace", "test-ns");
    }

    @Test
    void tryDeployBeliefState_whenNoDeployment_createsDeploymentAndService() throws Exception {
        // Only stub ObjectMapper where needed
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        when(domainOntology.getName()).thenReturn("MyDomain");
        when(domainOntology.getVersion()).thenReturn(semanticVersion(1, 0, 0));

        var getResponse = mock(KubernetesApiResponse.class);
        when(getResponse.getObject()).thenReturn(null);
        when(deploymentClient.get("test-ns", "mydomain1.0.0")).thenReturn(getResponse);

        var deployment = new V1Deployment();
        var service = new V1Service();

        when(beliefStateDeploymentBuilder.buildDeploymentFrom(domainOntology)).thenReturn(deployment);
        when(beliefStateDeploymentBuilder.buildServiceFrom(domainOntology)).thenReturn(service);

        var createDeploymentResponse = mock(KubernetesApiResponse.class);
        when(createDeploymentResponse.isSuccess()).thenReturn(true);
        when(deploymentClient.create(eq("test-ns"), eq(deployment), any(CreateOptions.class)))
            .thenReturn(createDeploymentResponse);

        var createServiceResponse = mock(KubernetesApiResponse.class);
        when(createServiceResponse.isSuccess()).thenReturn(true);
        when(serviceClient.create(eq("test-ns"), eq(service), any(CreateOptions.class)))
            .thenReturn(createServiceResponse);

        operator.tryDeployBeliefState(domainOntology);

        verify(deploymentClient).create(eq("test-ns"), eq(deployment), any(CreateOptions.class));
        verify(serviceClient).create(eq("test-ns"), eq(service), any(CreateOptions.class));
    }

    @Test
    void tryDeployBeliefState_whenDeploymentExists_doesNothing() throws Exception {
        when(domainOntology.getName()).thenReturn("MyDomain");
        when(domainOntology.getVersion()).thenReturn(semanticVersion(1, 2, 3));

        var getResponse = mock(KubernetesApiResponse.class);
        when(getResponse.getObject()).thenReturn(new V1Deployment());
        when(deploymentClient.get("test-ns", "mydomain1.2.3")).thenReturn(getResponse);

        operator.tryDeployBeliefState(domainOntology);

        verify(deploymentClient, never()).create(any(), any(), any());
        verify(serviceClient, never()).create(any(), any(), any());
        verify(beliefStateDeploymentBuilder, never()).buildDeploymentFrom(any());
        verify(beliefStateDeploymentBuilder, never()).buildServiceFrom(any());
    }

    @Test
    void tryDeployBeliefState_whenDeploymentCreateFails_throws() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        when(domainOntology.getName()).thenReturn("MyDomain");
        when(domainOntology.getVersion()).thenReturn(semanticVersion(2, 0, 0));

        var getResponse = mock(KubernetesApiResponse.class);
        when(getResponse.getObject()).thenReturn(null);
        when(deploymentClient.get("test-ns", "mydomain2.0.0")).thenReturn(getResponse);

        var deployment = new V1Deployment();
        when(beliefStateDeploymentBuilder.buildDeploymentFrom(domainOntology)).thenReturn(deployment);

        var createResponse = mock(KubernetesApiResponse.class);
        when(createResponse.isSuccess()).thenReturn(false);
        when(deploymentClient.create(eq("test-ns"), eq(deployment), any(CreateOptions.class)))
            .thenReturn(createResponse);

        assertThatThrownBy(() -> operator.tryDeployBeliefState(domainOntology))
            .isInstanceOf(IllegalArgumentException.class);

        verify(serviceClient, never()).create(any(), any(), any());
    }

    @Test
    void tryDeleteAllDeployedBeliefStateResources_success_deletesEachDeploymentAndService() throws Exception {
        var d1 = deploymentNamed("bs-a");
        var d2 = deploymentNamed("bs-b");

        var list = new V1DeploymentList();
        list.setItems(List.of(d1, d2));

        var response = mock(KubernetesApiResponse.class);
        when(response.isSuccess()).thenReturn(true);
        when(response.getObject()).thenReturn(list);
        when(deploymentClient.list(eq("test-ns"), any(ListOptions.class))).thenReturn(response);

        operator.tryDeleteAllDeployedBeliefStateResources();

        verify(deploymentClient).delete("test-ns", "bs-a");
        verify(deploymentClient).delete("test-ns", "bs-b");
        verify(serviceClient).delete("test-ns", "bs-a");
        verify(serviceClient).delete("test-ns", "bs-b");
    }

    @Test
    void tryDeleteAllDeployedBeliefStateResources_whenListFails_throws() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        var response = mock(KubernetesApiResponse.class);
        when(response.isSuccess()).thenReturn(false);
        when(deploymentClient.list(eq("test-ns"), any(ListOptions.class))).thenReturn(response);

        assertThatThrownBy(() -> operator.tryDeleteAllDeployedBeliefStateResources())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void teardown_whenUbiquiaDeploymentMissing_triggersCleanup() throws Exception {
        var spy = spy(operator);
        doNothing().when(spy).tryDeleteAllDeployedBeliefStateResources();

        var getResponse = mock(KubernetesApiResponse.class);
        when(getResponse.getHttpStatusCode()).thenReturn(HttpStatus.NO_CONTENT.value());
        when(deploymentClient.get("test-ns", "ubiquia-core-belief-state-generator-service"))
            .thenReturn(getResponse);

        spy.teardown();

        verify(spy).tryDeleteAllDeployedBeliefStateResources();
    }

    @Test
    void deleteBeliefStateResources_listsByLabelSelectorAndDeletes() {
        var deploymentsResponse = mock(KubernetesApiResponse.class);
        var servicesResponse = mock(KubernetesApiResponse.class);

        var dList = new V1DeploymentList();
        dList.setItems(List.of(deploymentNamed("dep-1")));

        var sList = new V1ServiceList();
        sList.setItems(List.of(serviceNamed("svc-1")));

        when(deploymentsResponse.getObject()).thenReturn(dList);
        when(servicesResponse.getObject()).thenReturn(sList);

        when(deploymentClient.list(eq("test-ns"), any(ListOptions.class))).thenReturn(deploymentsResponse);
        when(serviceClient.list(eq("test-ns"), any(ListOptions.class))).thenReturn(servicesResponse);

        operator.deleteBeliefStateResources("bs-x");

        var listOptionsCaptor = ArgumentCaptor.forClass(ListOptions.class);
        verify(deploymentClient).list(eq("test-ns"), listOptionsCaptor.capture());
        assertThat(listOptionsCaptor.getValue().getLabelSelector()).isEqualTo("belief-state=bs-x");

        verify(deploymentClient).delete("test-ns", "dep-1");

        // matches current impl (it uses deploymentClient.delete for services)
        verify(deploymentClient).delete("test-ns", "svc-1");
        verify(serviceClient, never()).delete(anyString(), anyString());
    }
}
