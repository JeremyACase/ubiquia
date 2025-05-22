package org.ubiquia.common.models.embeddable;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
@Embeddable
public class AdapterSettings {

    @JsonProperty("isValidateInputPayload")
    private Boolean isValidateInputPayload = false;

    @JsonProperty("isValidateOutputPayload")
    private Boolean isValidateOutputPayload = false;

    @JsonProperty("isPersistInputPayload")
    private Boolean isPersistInputPayload = false;

    @JsonProperty("isPersistOutputPayload")
    private Boolean isPersistOutputPayload = false;

    @JsonProperty("inboxPollFrequencyMilliseconds")
    private Long inboxPollFrequencyMilliseconds = 1000L;

    @JsonProperty("backpressurePollFrequencyMilliseconds")
    private Long backpressurePollFrequencyMilliseconds = 5000L;

    @JsonProperty("inputStampKeychains")
    private List<String> inputStampKeychains = new ArrayList<>();

    @JsonProperty("outputStampKeychains")
    private List<String> outputStampKeychains = new ArrayList<>();

    @JsonProperty("isPassthrough")
    private Boolean isPassthrough = false;

    public Boolean getIsPersistInputPayload() {
        return isPersistInputPayload;
    }

    public void setIsPersistInputPayload(Boolean persistInputPayload) {
        isPersistInputPayload = persistInputPayload;
    }

    public Boolean getIsPersistOutputPayload() {
        return isPersistOutputPayload;
    }

    public void setIsPersistOutputPayload(Boolean persistOutputPayload) {
        isPersistOutputPayload = persistOutputPayload;
    }

    public Long getInboxPollFrequencyMilliseconds() {
        return inboxPollFrequencyMilliseconds;
    }

    public void setInboxPollFrequencyMilliseconds(Long inboxPollFrequencyMilliseconds) {
        this.inboxPollFrequencyMilliseconds = inboxPollFrequencyMilliseconds;
    }

    public Long getBackpressurePollFrequencyMilliseconds() {
        return backpressurePollFrequencyMilliseconds;
    }

    public void setBackpressurePollFrequencyMilliseconds(Long backpressurePollFrequencyMilliseconds) {
        this.backpressurePollFrequencyMilliseconds = backpressurePollFrequencyMilliseconds;
    }

    public List<String> getInputStampKeychains() {
        return inputStampKeychains;
    }

    public void setInputStampKeychains(List<String> inputStampKeychains) {
        this.inputStampKeychains = inputStampKeychains;
    }

    public List<String> getOutputStampKeychains() {
        return outputStampKeychains;
    }

    public void setOutputStampKeychains(List<String> outputStampKeychains) {
        this.outputStampKeychains = outputStampKeychains;
    }

    public Boolean getValidateInputPayload() {
        return isValidateInputPayload;
    }

    public void setValidateInputPayload(Boolean validateInputPayload) {
        isValidateInputPayload = validateInputPayload;
    }

    public Boolean getValidateOutputPayload() {
        return isValidateOutputPayload;
    }

    public void setValidateOutputPayload(Boolean validateOutputPayload) {
        isValidateOutputPayload = validateOutputPayload;
    }

    public Boolean getIsPassthrough() {
        return isPassthrough;
    }

    public void setIsPassthrough(Boolean passthrough) {
        isPassthrough = passthrough;
    }
}
