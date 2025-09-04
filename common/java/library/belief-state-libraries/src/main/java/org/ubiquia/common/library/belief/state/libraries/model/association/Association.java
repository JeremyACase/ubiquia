package org.ubiquia.common.library.belief.state.libraries.model.association;

import jakarta.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

/**
 * A model to be used by clients towards associating models together relationally.
 */
@Validated
public class Association {

    private ParentAssociation parentAssociation;

    private ChildAssociation childAssociation;

    @NotNull
    public ParentAssociation getParentAssociation() {
        return parentAssociation;
    }

    public void setParentAssociation(ParentAssociation parentAssociation) {
        this.parentAssociation = parentAssociation;
    }

    @NotNull
    public ChildAssociation getChildAssociation() {
        return childAssociation;
    }

    public void setChildAssociation(ChildAssociation childAssociation) {
        this.childAssociation = childAssociation;
    }
}