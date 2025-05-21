package org.ubiquia.core.flow.model.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public class GraphEdgeDto {

    private String leftAdapterName;

    private List<String> rightAdapterNames;

    @NotNull
    public String getLeftAdapterName() {
        return leftAdapterName;
    }

    public void setLeftAdapterName(String leftAdapterName) {
        this.leftAdapterName = leftAdapterName;
    }

    @NotNull
    @Size(min = 1)
    public List<String> getRightAdapterNames() {
        return rightAdapterNames;
    }

    public void setRightAdapterNames(List<String> rightAdapterNames) {
        this.rightAdapterNames = rightAdapterNames;
    }
}

