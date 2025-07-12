package org.ubiquia.common.model.ubiquia.dto;


public class ObjectMetadata extends AbstractModel {

    private String bucketName;

    private UbiquiaAgent ubiquiaAgent;

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

    public UbiquiaAgent getUbiquiaAgent() {
        return ubiquiaAgent;
    }

    public void setUbiquiaAgent(UbiquiaAgent ubiquiaAgent) {
        this.ubiquiaAgent = ubiquiaAgent;
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
