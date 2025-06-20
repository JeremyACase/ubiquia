package org.ubiquia.common.library.belief.state.libraries.model.embed;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.validation.annotation.Validated;
import org.ubiquia.common.model.acl.dto.AbstractAclEntityDto;

/**
 * A model to be used by clients towards associating models together relationally.
 */
@Validated
public class Embed {

    private String parentId;

    private JsonNode toEmbed;

    @NotNull
    @Pattern(regexp = "[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}")
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @NotNull
    public JsonNode getToEmbed() {
        return toEmbed;
    }

    public void setToEmbed(JsonNode toEmbed) {
        this.toEmbed = toEmbed;
    }
}