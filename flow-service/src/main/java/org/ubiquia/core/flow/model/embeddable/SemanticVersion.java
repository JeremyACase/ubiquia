package org.ubiquia.core.flow.model.embeddable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;

@ApiModel(
    description = "A model defining a key-value-pair that can be persisted in the database.")
@Validated
@Embeddable
public class SemanticVersion {

    @JsonProperty("major")
    private Integer major;

    @JsonProperty("minor")
    private Integer minor;

    @JsonProperty("patch")
    private Integer patch;

    @NotNull
    public Integer getMajor() {
        return major;
    }

    public void setMajor(Integer major) {
        this.major = major;
    }

    @NotNull
    public Integer getMinor() {
        return minor;
    }

    public void setMinor(Integer minor) {
        this.minor = minor;
    }

    @NotNull
    public Integer getPatch() {
        return patch;
    }

    public void setPatch(Integer patch) {
        this.patch = patch;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SemanticVersion semanticVersion = (SemanticVersion) o;
        return
            Objects.equals(this.major, semanticVersion.major)
                && Objects.equals(this.minor, semanticVersion.minor)
                && Objects.equals(this.patch, semanticVersion.patch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.major, this.minor, this.patch);
    }

    @Override
    public String toString() {
        var string = this.getMajor()
            + "."
            + this.getMinor()
            + "."
            + this.getPatch();
        return string;
    }
}
