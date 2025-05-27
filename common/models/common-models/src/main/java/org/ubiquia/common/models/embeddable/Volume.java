package org.ubiquia.common.models.embeddable;


import jakarta.persistence.Embeddable;
import org.springframework.validation.annotation.Validated;

@Validated
@Embeddable
public class Volume {

    private String name;

    private String persistentVolumeClaimName;

    private String mountPath;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPersistentVolumeClaimName() {
        return persistentVolumeClaimName;
    }

    public void setPersistentVolumeClaimName(String persistentVolumeClaimName) {
        this.persistentVolumeClaimName = persistentVolumeClaimName;
    }

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }
}
