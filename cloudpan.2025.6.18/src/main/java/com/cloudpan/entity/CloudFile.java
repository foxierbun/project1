package com.cloudpan.entity;

import java.util.Objects;

public class CloudFile {
    public String fileName;
    public Long fileSize;
    public String filePath;
    public String userId;
    public String uploadPath;
    // 新增本地路径属性
    public String localPath;

    public CloudFile() {
    }

    public CloudFile(CloudFileBuilder builder) {
        this.fileName = builder.fileName;
        this.fileSize = builder.fileSize;
        this.filePath = builder.filePath;
        this.userId = builder.userId;
        this.uploadPath = builder.uploadPath;
        this.localPath = builder.localPath;
    }

    // 用于获取属性的getter方法
    public String getFileName() {
        return fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getUserId() {
        return userId;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public String getLocalPath() {
        return localPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass()!= o.getClass()) return false;
        CloudFile cloudFile = (CloudFile) o;
        return Objects.equals(fileName, cloudFile.fileName) &&
                Objects.equals(fileSize, cloudFile.fileSize) &&
                Objects.equals(filePath, cloudFile.filePath) &&
                Objects.equals(userId, cloudFile.userId) &&
                Objects.equals(uploadPath, cloudFile.uploadPath) &&
                Objects.equals(localPath, cloudFile.localPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, fileSize, filePath, userId, uploadPath, localPath);
    }

    public static class CloudFileBuilder {
        public String fileName;
        public Long fileSize;
        public String filePath;
        public String userId;
        public String uploadPath;
        public String localPath;
        public String sizeStr;

        public CloudFileBuilder(String userId) {
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("userId cannot be null or empty");
            }
            this.userId = userId;
        }

        public CloudFileBuilder withFileName(String fileName) {
            if (fileName == null || fileName.trim().isEmpty()) {
                throw new IllegalArgumentException("fileName cannot be null or empty");
            }
            this.fileName = fileName;
            return this;
        }

        public CloudFileBuilder withFileSize(String sizeStr) {
            if (sizeStr == null || sizeStr.trim().isEmpty()) {
                throw new IllegalArgumentException("fileSize cannot be null or empty");
            }
            try {
                this.fileSize = Long.parseLong(sizeStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("fileSize should be a valid number");
            }
            this.sizeStr = sizeStr;
            return this;
        }

        public CloudFileBuilder withFilePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public CloudFileBuilder withUploadPath(String uploadPath) {
            if (uploadPath == null || uploadPath.trim().isEmpty()) {
                throw new IllegalArgumentException("uploadPath cannot be null or empty");
            }
            this.uploadPath = uploadPath;
            return this;
        }

        // 添加设置本地路径的方法
        public CloudFileBuilder withLocalPath(String localPath) {
            if (localPath == null || localPath.trim().isEmpty()) {
                throw new IllegalArgumentException("localPath cannot be null or empty");
            }
            this.localPath = localPath;
            return this;
        }

        public CloudFile build() {
            return new CloudFile(this);
        }
    }
}