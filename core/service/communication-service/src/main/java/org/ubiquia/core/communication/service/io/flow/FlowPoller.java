package org.ubiquia.core.communication.service.io.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.ubiquia.core.communication.config.FlowServiceConfig;

@Service
public class FlowPoller {

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Scheduled(fixedRateString = "#{@flowServiceConfig.pollFrequencyMilliseconds}")
    public void pollEndpoint() {
        try {
            // Replace with actual polling logic (e.g., REST call)
            System.out.println("Polling endpoint...");
            // e.g., use RestTemplate or WebClient to call the endpoint
        } catch (Exception e) {
            System.err.println("Error during polling: " + e.getMessage());
        }
    }

}
