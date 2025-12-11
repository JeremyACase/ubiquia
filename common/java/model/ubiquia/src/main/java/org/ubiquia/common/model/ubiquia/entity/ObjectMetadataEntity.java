package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.*;

@Entity
public class ObjectMetadataEntity extends AbstractModelEntity {

    private String bucketName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metadata_agent_join_id", nullable = false)
    private AgentEntity agent;

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

    public AgentEntity getAgent() {
        return agent;
    }

    public void setAgent(AgentEntity agent) {
        this.agent = agent;
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
