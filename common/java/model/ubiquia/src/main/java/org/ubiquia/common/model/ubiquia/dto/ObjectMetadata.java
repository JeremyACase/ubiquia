package org.ubiquia.common.model.ubiquia.dto;


public class ObjectMetadata extends AbstractModel {

    private String bucketName;

    private Agent agent;

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

    public Agent getUbiquiaAgent() {
        return agent;
    }

    public void setUbiquiaAgent(Agent agent) {
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
