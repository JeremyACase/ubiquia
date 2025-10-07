package org.ubiquia.core.flow.service.command.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.common.model.ubiquia.entity.FlowMessageEntity;
import org.ubiquia.core.flow.component.adapter.MergeAdapter;
import org.ubiquia.core.flow.repository.AdapterRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.service.builder.FlowEventBuilder;
import org.ubiquia.core.flow.service.orchestrator.AdapterPayloadOrchestrator;
import org.ubiquia.core.flow.service.telemetry.MicroMeterHelper;

/**
 * A service that exposes adapter type logic.
 */
@Service
public class MergeAdapterCommand implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(MergeAdapterCommand.class);

    @Autowired
    private AdapterPayloadOrchestrator adapterPayloadOrchestrator;

    @Autowired
    private AdapterRepository adapterRepository;

    @Autowired
    private FlowEventBuilder flowEventBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FlowMessageRepository flowMessageRepository;

    @Autowired(required = false)
    private MicroMeterHelper microMeterHelper;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Transactional
    public void tryProcessMessageFor(final FlowMessage message, final MergeAdapter adapter) {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterHelper)) {
            sample = this.microMeterHelper.startSample();
        }

        var adapterContext = adapter.getAdapterContext();

        try {
            var messages = this.flowMessageRepository
                .findAllByTargetAdapterIdAndFlowEventFlowId(
                    adapterContext.getAdapterId(),
                    message.getFlowEvent().getFlowId());

            if (!messages.isEmpty()) {

                var targetAdapterRecord = this
                    .adapterRepository
                    .findById(adapterContext.getAdapterId());
                var targetAdapter = targetAdapterRecord.get();

                var upstreamAdapters = targetAdapter.getUpstreamAdapters();
                if (messages.size() == upstreamAdapters.size()) {

                    this.getLogger().info("{} has all {} upstream messages "
                            + "with batch id {}; processing...",
                        adapterContext.getAdapterName(),
                        messages.size(),
                        message.getFlowEvent().getFlowId());

                    var merged = this.mergeMessages(messages);

                    var event = this.flowEventBuilder.makeEventFrom(
                        merged,
                        message.getFlowEvent().getFlowId(),
                        adapter);

                    this.adapterPayloadOrchestrator.forwardPayload(event, adapter, merged);

                    this.flowMessageRepository.deleteAll(messages);
                } else {
                    this.getLogger().debug(" {} has only {} messages with batch id {}; "
                            + "not processing...",
                        adapterContext.getAdapterName(),
                        messages.size(),
                        message.getFlowEvent().getFlowId());
                }
            } else {
                throw new RuntimeException("ERROR: Somehow, the messages list is empty!");
            }
        } catch (Exception e) {
            this.getLogger().error("ERROR: Could not process inbox message: {}",
                e.getMessage());
        }

        if (Objects.nonNull(sample)) {
            this.microMeterHelper.endSample(
                sample,
                "tryProcessInboxMessage",
                adapterContext.getTags());
        }
    }

    private String mergeMessages(final List<FlowMessageEntity> messages)
        throws JsonProcessingException {

        var mergedMap = new HashMap<String, Object>();
        for (var message : messages) {
            var sourceAdapterName = message.getFlowEvent().getAdapter().getName();
            mergedMap.put(sourceAdapterName, message.getPayload());
        }

        var mergedString = this.objectMapper.writeValueAsString(mergedMap);
        return mergedString;
    }
}
