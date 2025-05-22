package org.ubiquia.core.flow.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.models.embeddable.OverrideSettings;
import org.ubiquia.common.models.embeddable.OverrideSettingsStringified;


/**
 * A service dedicated to mapping override settings.
 */
@Service
public class OverrideSettingsMapper {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Map from a list of stringified override settings to their object representations.
     *
     * @param froms The settings to map from.
     * @return The converted override settings.
     * @throws JsonProcessingException Exception from converting from string to object.
     */
    public List<OverrideSettings> mapToObjectRepresentation(
        final Set<OverrideSettingsStringified> froms)
        throws JsonProcessingException {
        var tos = new ArrayList<OverrideSettings>();
        for (var from : froms) {
            tos.add(this.map(from));
        }
        return tos;
    }

    /**
     * Map from a list of object representation settings to their stringified version.
     *
     * @param froms The settings to map from.
     * @return The converted override settings.
     * @throws JsonProcessingException Exception from converting.
     */
    public List<OverrideSettingsStringified> mapToStringified(
        final List<OverrideSettings> froms)
        throws JsonProcessingException {
        var tos = new ArrayList<OverrideSettingsStringified>();
        for (var from : froms) {
            tos.add(this.map(from));
        }
        return tos;
    }

    /**
     * Map an override settings to contain an object instead of a value.
     *
     * @param from The object we're mapping from.
     * @return The object we've mapped to.
     * @throws JsonProcessingException Exception from converting from string to object.
     */
    public OverrideSettings map(final OverrideSettingsStringified from)
        throws JsonProcessingException {
        var to = new OverrideSettings();

        var objectified = this.objectMapper.readValue(from.getValue(), Object.class);
        to.setFlag(from.getFlag());
        to.setKey(from.getKey());
        to.setValue(objectified);

        return to;
    }

    /**
     * Map from an override settings to a stringified representation.
     *
     * @param from The settings to map from.
     * @return A stringified representation of the override settings.
     * @throws JsonProcessingException Exceptions from mapping.
     */
    public OverrideSettingsStringified map(final OverrideSettings from)
        throws JsonProcessingException {
        var to = new OverrideSettingsStringified();
        var stringified = this.objectMapper.writeValueAsString(from.getValue());
        to.setFlag(from.getFlag());
        to.setKey(from.getKey());
        to.setValue(stringified);
        return to;
    }
}
