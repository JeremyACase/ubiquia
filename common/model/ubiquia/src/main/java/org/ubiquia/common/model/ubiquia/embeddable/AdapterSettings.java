package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;

@Embeddable
public class AdapterSettings {

    private Boolean stimulateInputPayload = false;

    private Boolean validateInputPayload = false;

    private Boolean validateOutputPayload = false;

    private Boolean persistInputPayload = false;

    private Boolean persistOutputPayload = false;

    private Long stimulateFrequencyMilliseconds = 5000L;

    private Long inboxPollFrequencyMilliseconds = 1000L;

    private Long backpressurePollFrequencyMilliseconds = 5000L;

    private List<String> inputStampKeychains = new ArrayList<>();

    private List<String> outputStampKeychains = new ArrayList<>();

    private Boolean isPassthrough = false;

    public Boolean getPersistInputPayload() {
        return persistInputPayload;
    }

    public void setPersistInputPayload(Boolean persistInputPayload) {
        this.persistInputPayload = persistInputPayload;
    }

    public Boolean getPersistOutputPayload() {
        return persistOutputPayload;
    }

    public void setPersistOutputPayload(Boolean persistOutputPayload) {
        this.persistOutputPayload = persistOutputPayload;
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
        return validateInputPayload;
    }

    public void setValidateInputPayload(Boolean validateInputPayload) {
        this.validateInputPayload = validateInputPayload;
    }

    public Boolean getValidateOutputPayload() {
        return validateOutputPayload;
    }

    public void setValidateOutputPayload(Boolean validateOutputPayload) {
        this.validateOutputPayload = validateOutputPayload;
    }

    public Boolean getIsPassthrough() {
        return isPassthrough;
    }

    public void setIsPassthrough(Boolean passthrough) {
        isPassthrough = passthrough;
    }

    public Boolean getStimulateInputPayload() {
        return stimulateInputPayload;
    }

    public void setStimulateInputPayload(Boolean stimulateInputPayload) {
        this.stimulateInputPayload = stimulateInputPayload;
    }

    public Long getStimulateFrequencyMilliseconds() {
        return stimulateFrequencyMilliseconds;
    }

    public void setStimulateFrequencyMilliseconds(Long stimulateFrequencyMilliseconds) {
        this.stimulateFrequencyMilliseconds = stimulateFrequencyMilliseconds;
    }
}
