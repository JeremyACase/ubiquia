package org.ubiquia.core.flow.service.io.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.core.flow.component.config.bootstrap.GraphBootstrapConfig;
import org.ubiquia.core.flow.interfaces.InterfaceBootstrapper;
import org.ubiquia.core.flow.service.finder.GraphFinder;
import org.ubiquia.core.flow.service.manager.ComponentManager;
import org.ubiquia.core.flow.service.manager.NodeManager;

/**
 * Bootstrapper that deploys graphs defined in
 * {@code ubiquia.agent.flow-service.bootstrap.graph.deployments} on startup.
 *
 * <p>Runs after domain ontologies have been registered (ontologies must exist before their
 * graphs can be deployed). Already-deployed graphs are skipped so restarts are idempotent.</p>
 */
@ConditionalOnProperty(
    value = "ubiquia.agent.flow-service.bootstrap.graph.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class GraphBootstrapper implements InterfaceBootstrapper {

    private static final Logger logger = LoggerFactory.getLogger(GraphBootstrapper.class);

    @Autowired
    private GraphBootstrapConfig config;

    @Autowired
    private GraphFinder graphFinder;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private ComponentManager componentManager;

    @Autowired
    private NodeManager nodeManager;

    @Override
    @Transactional
    public void bootstrap() throws Exception {
        logger.info("...bootstrapping graphs...");

        if (this.config.getDeployments() == null || this.config.getDeployments().isEmpty()) {
            logger.info("...no graph deployments configured; skipping.");
            return;
        }

        for (var deployment : this.config.getDeployments()) {
            var graphName = deployment.getGraphName();
            var ontologyName = deployment.getDomainOntologyName();
            var version = deployment.getDomainVersion();

            logger.info("...deploying graph '{}' from ontology '{}' version {}...",
                graphName, ontologyName, version);

            var alreadyDeployed = this.graphFinder.findDeployedGraphRecordWith(
                graphName, ontologyName, version, this.agentConfig.getId());

            if (alreadyDeployed.isPresent()) {
                logger.info("...graph '{}' is already deployed; skipping.", graphName);
                continue;
            }

            var agentRecord = this.agentRepository.findById(this.agentConfig.getId());
            if (agentRecord.isEmpty()) {
                throw new RuntimeException(
                    "ERROR: Could not find Ubiquia Agent with id " + this.agentConfig.getId());
            }

            var graphEntity = this.graphFinder.findGraphWith(graphName, ontologyName, version);

            var agentEntity = agentRecord.get();
            agentEntity.getDeployedGraphs().add(graphEntity);
            this.agentRepository.save(agentEntity);

            this.componentManager.tryDeployComponentsFor(deployment);
            this.nodeManager.tryDeployNodesFor(deployment);

            logger.info("...deployed graph '{}'.", graphName);
        }

        logger.info("...completed bootstrapping graphs.");
    }
}
