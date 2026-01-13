package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class GraphEdge {

    private String leftNodeName;

    private List<String> rightNodeNames;

    @NotNull
    public String getLeftNodeName() {
        return leftNodeName;
    }

    public void setLeftNodeName(String leftNodeName) {
        this.leftNodeName = leftNodeName;
    }

    @NotNull
    @Size(min = 1)
    public List<String> getRightNodeNames() {
        return rightNodeNames;
    }

    public void setRightNodeNames(List<String> rightNodeNames) {
        this.rightNodeNames = rightNodeNames;
    }
}

