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
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.component.adapter.QueueAdapter;
import org.ubiquia.common.models.dto.FlowMessageDto;
import org.ubiquia.core.flow.repository.AdapterRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.service.calculator.AdapterConcurrencyCalculator;
import org.ubiquia.core.flow.service.mapper.FlowMessageDtoMapper;

/**
 * A serviced dedicated to polling for incoming messages from the database on behalf
 * of adapters.
 */
@Service
@Transactional
public class Inbox {

    private static final Logger logger = LoggerFactory.getLogger(Inbox.class);
    @Autowired
    private AdapterConcurrencyCalculator adapterConcurrencyCalculator;
    @Autowired
    private FlowMessageDtoMapper flowMessageDtoMapper;
    @Autowired
    private AdapterRepository adapterRepository;
    @Autowired
    private FlowMessageRepository flowMessageRepository;

    /**
     * Try to query an inbox message for a queue adapter.
     *
     * @param adapter The adapter to query a message for.
     * @return An event associated with the query.
     * @throws JsonProcessingException Exceptions from parsing payloads.
     */
    public FlowMessageDto tryQueryInboxMessagesFor(final QueueAdapter adapter)
        throws JsonProcessingException {

        var adapterContext = adapter.getAdapterContext();
        logger.debug("Querying inbox records for adapter {}...",
            adapterContext.getAdapterName());

        var sort = Sort.by("createdAt").ascending();
        var pageRequest = PageRequest
            .of(0, 1)
            .withSort(sort);
        var query = this.flowMessageRepository.findAllByTargetAdapterId(
            pageRequest,
            adapterContext.getAdapterId());

        FlowMessageDto flowMessage = null;
        if (query.hasContent()) {
            logger.debug("queried message with ID: {}", query.getContent().get(0).getId());
            flowMessage = this.flowMessageDtoMapper.map(query.getContent().get(0));
        }

        return flowMessage;
    }

    /**
     * Provided an adapter, query the inbox for any of the adapter's inbox messages.
     *
     * @param adapter The adapter to query for.
     * @throws JsonProcessingException Exceptions from parsing payloads.
     */
    public List<FlowMessageDto> tryQueryInboxMessagesFor(final AbstractAdapter adapter)
        throws JsonProcessingException {

        var adapterContext = adapter.getAdapterContext();
        logger.debug("Querying inbox records for adapter {}...", adapterContext.getAdapterName());

        var sort = Sort.by("createdAt").ascending();
        var pageSize = this.adapterConcurrencyCalculator.getInboxQueryPageSizeFor(adapter);
        var pageRequest = PageRequest
            .of(0, pageSize)
            .withSort(sort);

        var query = this.flowMessageRepository.findAllByTargetAdapterId(
            pageRequest,
            adapterContext.getAdapterId());

        var messages = new ArrayList<FlowMessageDto>();
        for (var flowMessageEntity : query.getContent()) {
            logger.debug("queried message with ID: {}", flowMessageEntity.getId());
            var flowMessages = this.flowMessageDtoMapper.map(flowMessageEntity);
            messages.add(flowMessages);
        }
        logger.debug("...queried {} inbox records...", query.getTotalElements());
        return messages;
    }
}
