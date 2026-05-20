package org.ubiquia.common.library.belief.state.libraries.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.ubiquia.common.model.domain.entity.AbstractDomainModelEntity;

@Entity(name = "BeliefStateObjectMetadataEntity")
@Table(name = "belief_state_object_metadata")
public class ObjectMetadataEntity extends AbstractDomainModelEntity {

    private String bucketName;
    private String originalFilename;
    private String contentType;
    private Long size;

    @Override
    public String getModelType() {
        return "ObjectMetadata";
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
