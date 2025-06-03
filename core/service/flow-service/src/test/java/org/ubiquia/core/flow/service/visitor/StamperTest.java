package org.ubiquia.core.flow.service.visitor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ubiquia.common.model.ubiquia.embeddable.AdapterSettings;
import org.ubiquia.common.model.ubiquia.embeddable.KeyValuePair;
import org.ubiquia.common.model.ubiquia.entity.Adapter;
import org.ubiquia.common.model.ubiquia.entity.FlowEvent;


@SpringBootTest
public class StamperTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StamperVisitor stamper;

    @Test
    public void assertStampsEventInputPayload_isValid() throws JsonProcessingException {

        var payload = new HashMap<String, String>();
        payload.put("noStampKey", UUID.randomUUID().toString());
        payload.put("stampKey", UUID.randomUUID().toString());
        var json = this.objectMapper.writeValueAsString(payload);

        var event = new FlowEvent();
        event.setInputPayload(json);
        event.setInputPayloadStamps(new HashSet<>());

        var kvp = new KeyValuePair();
        kvp.setKey("stampKey");
        kvp.setValue(payload.get("stampKey"));
        event.getInputPayloadStamps().add(kvp);

        var settings = new AdapterSettings();
        settings.setInputStampKeychains(new ArrayList<>());
        settings.getInputStampKeychains().add(kvp.getKey());

        var adapter = new Adapter();
        adapter.setAdapterSettings(settings);
        event.setAdapter(adapter);

        this.stamper.tryStampInputs(event, json);
        var match = event.getInputPayloadStamps().stream().filter(x ->
            x.getKey().equals("stampKey")).findFirst();
        Assertions.assertEquals(1, (long) event.getInputPayloadStamps().size());
        Assertions.assertEquals(payload.get("stampKey"), match.get().getValue());
    }

    @Test
    public void assertStampsEventOutputPayload_isValid() throws JsonProcessingException {

        var payload = new HashMap<String, String>();
        payload.put("noStampKey", UUID.randomUUID().toString());
        payload.put("stampKey", UUID.randomUUID().toString());
        var json = this.objectMapper.writeValueAsString(payload);

        var event = new FlowEvent();
        event.setOutputPayload(json);
        event.setOutputPayloadStamps(new HashSet<>());

        var kvp = new KeyValuePair();
        kvp.setKey("stampKey");
        kvp.setValue(payload.get("stampKey"));
        event.getOutputPayloadStamps().add(kvp);

        var settings = new AdapterSettings();
        settings.setOutputStampKeychains(new ArrayList<>());
        settings.getOutputStampKeychains().add(kvp.getKey());

        var adapter = new Adapter();
        adapter.setAdapterSettings(settings);
        event.setAdapter(adapter);

        this.stamper.tryStampInputs(event, json);
        var match = event.getOutputPayloadStamps().stream().filter(x ->
            x.getKey().equals("stampKey")).findFirst();
        Assertions.assertEquals(1, (long) event.getOutputPayloadStamps().size());
        Assertions.assertEquals(payload.get("stampKey"), match.get().getValue());
    }
}