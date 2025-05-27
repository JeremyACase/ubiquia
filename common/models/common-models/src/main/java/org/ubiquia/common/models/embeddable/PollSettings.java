package org.ubiquia.common.models.embeddable;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Embeddable
public class PollSettings {

    private Long nowTimeOffsetStartInMinutes;

    private Long nowTimeOffsetEndInMinutes;

    private String pollEndpoint;

    private Long pollFrequencyInMilliseconds = 5000L;

    @NotNull
    public String getPollEndpoint() {
        return pollEndpoint;
    }

    public void setPollEndpoint(String pollEndpoint) {
        this.pollEndpoint = pollEndpoint;
    }

    public Long getNowTimeOffsetStartInMinutes() {
        return nowTimeOffsetStartInMinutes;
    }

    public void setNowTimeOffsetStartInMinutes(Long nowTimeOffsetStartInMinutes) {
        this.nowTimeOffsetStartInMinutes = nowTimeOffsetStartInMinutes;
    }

    public Long getNowTimeOffsetEndInMinutes() {
        return nowTimeOffsetEndInMinutes;
    }

    public void setNowTimeOffsetEndInMinutes(Long nowTimeOffsetEndInMinutes) {
        this.nowTimeOffsetEndInMinutes = nowTimeOffsetEndInMinutes;
    }

    public Long getPollFrequencyInMilliseconds() {
        return pollFrequencyInMilliseconds;
    }

    public void setPollFrequencyInMilliseconds(Long pollFrequencyInMilliseconds) {
        this.pollFrequencyInMilliseconds = pollFrequencyInMilliseconds;
    }
}
