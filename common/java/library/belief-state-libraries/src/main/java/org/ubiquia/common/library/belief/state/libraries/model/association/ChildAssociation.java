package org.ubiquia.common.library.belief.state.libraries.model.association;

import jakarta.validation.constraints.Pattern;

import org.springframework.validation.annotation.Validated;

/**
 * Object defining a child association.
 */
@Validated
public class ChildAssociation {

    private String childId;

    @Pattern(regexp = "[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}")
    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }
}