package org.ubiquia.common.models.embeddable;


import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;

@Embeddable
public class AdapterSettings {

    private Boolean isValidateInputPayload = false;

    private Boolean isValidateOutputPayload = false;

    private Boolean isPersistInputPayload = false;

    private Boolean isPersistOutputPayload = false;

    private Long inboxPollFrequencyMilliseconds = 1000L;

    private Long backpressurePollFrequencyMilliseconds = 5000L;

    private List<String> inputStampKeychains = new ArrayList<>();

    private List<String> outputStampKeychains = new ArrayList<>();

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
