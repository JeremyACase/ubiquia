package org.ubiquia.common.models.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

@Embeddable
public class SemanticVersion {

    private Integer major;

    private Integer minor;

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
