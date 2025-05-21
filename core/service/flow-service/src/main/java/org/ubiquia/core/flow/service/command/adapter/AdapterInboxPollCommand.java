package org.ubiquia.core.flow.service.command.adapter;

import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.model.dto.FlowMessageDto;
import org.ubiquia.core.flow.service.io.Inbox;
import org.ubiquia.core.flow.service.telemetry.MicroMeterHelper;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
public class AdapterInboxPollCommand {

    private static final Logger logger = LoggerFactory.getLogger(AdapterInboxPollCommand.class);

    @Autowired
    private Inbox inbox;

    @Autowired
    private AdapterInboxMessageCommand adapterInboxMessageCommand;

    @Autowired(required = false)
    private MicroMeterHelper microMeterHelper;

    public Logger getLogger() {
        return logger;
    }

    @Transactional
    public List<FlowMessageDto> tryPollInboxFor(final AbstractAdapter adapter) {
        var adapterContext = adapter.getAdapterContext();
        this.getLogger().debug("Adapter {} of graph {} with is polling inbox...",
            adapterContext.getAdapterName(),
            adapterContext.getGraphName());

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterHelper)) {
            sample = this.microMeterHelper.startSample();
        }

        var messages = new ArrayList<FlowMessageDto>();
        try {
            messages.addAll(this.inbox.tryQueryInboxMessagesFor(adapter));
        } catch (Exception e) {
            this.getLogger().error("ERROR with inbox polling: {}", e.getMessage());
        }

        if (Objects.nonNull(sample)) {
            this.microMeterHelper.endSample(sample, "pollInbox", adapterContext.getTags());
        }
        this.getLogger().debug("...finished polling inbox.");

        return messages;
    }
}