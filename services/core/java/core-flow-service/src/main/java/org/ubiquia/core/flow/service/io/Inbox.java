package org.ubiquia.core.flow.service.io;


import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.implementation.service.mapper.FlowMessageDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.component.node.QueueNode;
import org.ubiquia.core.flow.repository.NodeRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.service.calculator.NodeConcurrencyCalculator;

/**
 * A serviced dedicated to polling for incoming messages from the database on behalf
 * of adapters.
 */
@Service
@Transactional
public class Inbox {

    private static final Logger logger = LoggerFactory.getLogger(Inbox.class);
    @Autowired
    private NodeConcurrencyCalculator nodeConcurrencyCalculator;
    @Autowired
    private FlowMessageDtoMapper flowMessageDtoMapper;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private FlowMessageRepository flowMessageRepository;

    /**
     * Try to query an inbox message for a queue adapter.
     *
     * @param node The adapter to query a message for.
     * @return An event associated with the query.
     * @throws JsonProcessingException Exceptions from parsing payloads.
     */
    public FlowMessage tryQueryInboxMessagesFor(final QueueNode node)
        throws JsonProcessingException {

        var nodeContext = node.getNodeContext();
        logger.debug("Querying inbox records for adapter {}...",
            nodeContext.getNodeName());

        var sort = Sort.by("createdAt").ascending();

        var pageRequest = PageRequest
            .of(0, 1)
            .withSort(sort);

        var query = this
            .flowMessageRepository
            .findAllByTargetNodeId(pageRequest, nodeContext.getNodeId());

        FlowMessage flowMessage = null;
        if (query.hasContent()) {
            logger.debug("queried message with ID: {}", query.getContent().get(0).getId());
            flowMessage = this.flowMessageDtoMapper.map(query.getContent().get(0));
        }

        return flowMessage;
    }

    /**
     * Provided an adapter, query the inbox for any of the adapter's inbox messages.
     *
     * @param node The adapter to query for.
     * @throws JsonProcessingException Exceptions from parsing payloads.
     */
    public List<FlowMessage> tryQueryInboxMessagesFor(final AbstractNode node)
        throws JsonProcessingException {

        var nodeContext = node.getNodeContext();
        logger.debug("Querying inbox records for node {}...",
            nodeContext.getNodeName());

        var sort = Sort.by("createdAt").ascending();
        var pageSize = this.nodeConcurrencyCalculator.getInboxQueryPageSizeFor(node);
        var pageRequest = PageRequest
            .of(0, pageSize)
            .withSort(sort);

        var query = this.flowMessageRepository.findAllByTargetNodeId(
            pageRequest,
            nodeContext.getNodeId());

        var messages = new ArrayList<FlowMessage>();
        for (var flowMessageEntity : query.getContent()) {
            logger.debug("queried message with ID: {}", flowMessageEntity.getId());
            var flowMessages = this.flowMessageDtoMapper.map(flowMessageEntity);
            messages.add(flowMessages);
        }
        logger.debug("...queried first {} records of {} total inbox records...",
            messages.size(),
            query.getTotalElements());
        return messages;
    }
}
