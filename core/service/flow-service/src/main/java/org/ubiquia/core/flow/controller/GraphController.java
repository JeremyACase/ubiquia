package org.ubiquia.core.flow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.ubiquia.core.flow.model.IngressResponse;
import org.ubiquia.core.flow.model.dto.GraphDto;
import org.ubiquia.core.flow.model.embeddable.GraphDeployment;
import org.ubiquia.core.flow.model.embeddable.SemanticVersion;
import org.ubiquia.core.flow.model.entity.Graph;
import org.ubiquia.core.flow.repository.AdapterRepository;
import org.ubiquia.core.flow.repository.AgentRepository;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.service.k8s.AgentOperator;
import org.ubiquia.core.flow.service.manager.AdapterManager;
import org.ubiquia.core.flow.service.manager.AgentManager;
import org.ubiquia.core.flow.service.registrar.GraphRegistrar;

@RestController
@RequestMapping("/ubiquia/graph")
public class GraphController extends GenericEntityController<Graph, GraphDto> {

    private static final Logger logger = LoggerFactory.getLogger(GraphController.class);

    @Autowired
    private AdapterManager adapterManager;

    @Autowired(required = false)
    private AgentOperator agentOperator;

    @Autowired
    private AdapterRepository adapterRepository;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private GraphRegistrar graphRegistrar;

    @Autowired
    private GraphRepository graphRepository;

    @Override
    public Logger getLogger() {
        return logger;
    }

    /**
     * Delete a graph provided its ID.
     *
     * @param id The id of the graph to delete.
     * @return The ID of the deleted graph.
     */
    @DeleteMapping("/delete/{id}")
    @Transactional
    public ResponseEntity<String> delete(@PathVariable("id") final String id) {
        logger.info("Received request to delete: ID {}", id);
        ResponseEntity<String> response = null;
        var record = this.graphRepository.findById(id);
        if (record.isEmpty()) {
            response = ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
            response = ResponseEntity.status(HttpStatus.OK).body(record.get().getId());
            this.adapterRepository.deleteAll(record.get().getAdapters());
            this.agentRepository.deleteAll(record.get().getAgents());
            this.graphRepository.delete(record.get());
        }
        return response;
    }

    @PostMapping("/register/post")
    @Transactional
    public IngressResponse register(@RequestBody @Valid final GraphDto graphRegistration)
        throws Exception {
        logger.info("Received a registration request: {}", graphRegistration);
        var graphEntity = this.graphRegistrar.tryRegister(graphRegistration);
        var response = super.ingressResponseBuilder.buildIngressResponseFrom(graphEntity);
        return response;
    }

    @PostMapping(value = "/register/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngressResponse upload(@RequestParam("file") MultipartFile file) {
        logger.info("Received uploaded graph; processing...");

        IngressResponse response = null;

        try {
            var payload = IOUtils.toString(file.getInputStream(), StandardCharsets.UTF_8);
            var yamlMapper = new ObjectMapper(new YAMLFactory());
            var graphRegistration = yamlMapper.readValue(payload, GraphDto.class);
            var graphEntity = this.graphRegistrar.tryRegister(graphRegistration);
            response = super.ingressResponseBuilder.buildIngressResponseFrom(graphEntity);
        } catch (Exception e) {
            throw new IllegalArgumentException("ERROR: Could not transform file into graph - "
                + e.getMessage());
        }

        return response;
    }

    /**
     * Attempt to deploy a graph provided its name and version.
     *
     * @param deployment Data representing the graph to deploy.
     * @throws Exception Any exceptions from attempting to deploy the graph.
     */
    @PostMapping("/deploy")
    @Transactional
    public void tryDeployGraph(@RequestBody @Valid GraphDeployment deployment)
        throws Exception {

        logger.info("Received a request to deploy graph {} with version {}...",
            deployment.getName(),
            deployment.getVersion());

        this.agentManager.tryDeployAgentsFor(deployment);
        this.adapterManager.tryDeployAdaptersFor(deployment);
    }
    
    @PostMapping("/teardown")
    @Transactional
    public void tryTeardownGraph(
        @RequestParam(value = "graph-name") String graphName,
        @RequestBody @Valid SemanticVersion version) {

        logger.info("Received a request to teardown graph {} with version {}...",
            graphName,
            version);

        var graphRecord = this
            .graphRepository
            .findByGraphNameAndVersionMajorAndVersionMinorAndVersionPatch(
                graphName,
                version.getMajor(),
                version.getMinor(),
                version.getPatch());

        if (graphRecord.isEmpty()) {
            logger.error("ERROR: Could not find a graph registered with name {} and version {}...",
                graphName,
                version);
            throw new IllegalArgumentException("ERROR: Could not find a graph registered with name "
                + graphName
                + " and semantic version "
                + version);
        } else {
            this.adapterManager.tearDownAdaptersFor(graphName, version);

            if (Objects.nonNull(this.agentOperator)) {
                logger.info("...running in kubernetes; attempting to tear down any of the graph's "
                    + " resources deployed in Kubernetes...");
                this.agentOperator.deleteGraphResources(graphName);
            }
        }
        logger.info("...finished tearing down the graph...");
    }

}
