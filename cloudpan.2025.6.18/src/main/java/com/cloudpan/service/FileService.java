package com.cloudpan.service;

import com.cloudpan.entity.CloudFile;
import com.google.gson.Gson;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {
    private static final String HDFS_ROOT = "hdfs://mycluster/";
    private static final String LOCAL_DOWNLOAD_DIR = "D:\\download\\";

    private final Configuration conf;
    private final Gson gson;
    private final Logger logger = LoggerFactory.getLogger(FileService.class);

    public FileService() {
        this.conf = loadConfig();
        this.gson = new Gson();

        // 确保本地目录存在
        ensureLocalDirectoryExists(LOCAL_DOWNLOAD_DIR);
    }

    private Configuration loadConfig() {
        Configuration conf = new Configuration();
        conf.addResource(new Path("src/main/resources/hadoop/core-site.xml"));
        conf.addResource(new Path("src/main/resources/hadoop/hdfs-site.xml"));
        return conf;
    }

    private void ensureLocalDirectoryExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                logger.error("无法创建本地目录: {}", path);
            }
        }
    }

    private void checkUserId(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
    }

    private String formatHdfsPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }

        // 确保路径以hdfs://mycluster开头
        if (!path.startsWith("hdfs://")) {
            return HDFS_ROOT + (path.startsWith("/") ? path.substring(1) : path);
        }
        return path;
    }

    public String upload(CloudFile.CloudFileBuilder cloudFileBuilder, InputStream fileStream) {
        String userId = cloudFileBuilder.userId;
        checkUserId(userId);
        String uploadPath = cloudFileBuilder.uploadPath;
        if (uploadPath == null) {
            throw new IllegalArgumentException("uploadPath cannot be null");
        }
        uploadPath = formatHdfsPath(uploadPath);

        CloudFile cloudFile = cloudFileBuilder.build();
        try (FileSystem fs = FileSystem.get(URI.create(HDFS_ROOT), conf)) {
            Path targetPath = new Path(uploadPath, cloudFile.fileName);
            if (!fs.exists(targetPath.getParent())) {
                fs.mkdirs(targetPath.getParent());
            }
            if (fs.exists(targetPath)) {
                return gson.toJson(new ResponseData("文件已存在，上传失败"));
            }
            try (FSDataOutputStream out = fs.create(targetPath)) {
                byte[] buffer = new byte[1024 * 8];
                int bytesRead;
                while ((bytesRead = fileStream.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return gson.toJson(new ResponseData("文件上传成功"));
        } catch (IOException e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            return gson.toJson(new ResponseData("文件上传失败: " + e.getMessage()));
        }
    }

    public String download(DownloadRequest request) {
        String userId = request.getUserId();
        checkUserId(userId);
        String uploadPath = request.getUploadPath();
        uploadPath = formatHdfsPath(uploadPath);

        try (FileSystem fs = FileSystem.get(URI.create(HDFS_ROOT), conf)) {
            Path hdfsPath = new Path(uploadPath);
            if (!fs.exists(hdfsPath)) {
                return gson.toJson(new ResponseData("文件下载失败，文件不存在"));
            }

            // 确保下载目录存在
            File localDir = new File(LOCAL_DOWNLOAD_DIR);
            if (!localDir.exists()) {
                if (!localDir.mkdirs()) {
                    logger.error("无法创建下载目录: {}", LOCAL_DOWNLOAD_DIR);
                    return gson.toJson(new ResponseData("文件下载失败，无法创建下载目录"));
                }
            }

            // 构建本地文件路径
            String fileName = hdfsPath.getName();
            File localFile = new File(localDir, fileName);

            logger.info("开始下载文件: {}", uploadPath);
            logger.info("本地保存路径: {}", localFile.getAbsolutePath());

            try (FSDataInputStream in = fs.open(hdfsPath);
                 FileOutputStream out = new FileOutputStream(localFile)) {

                byte[] buffer = new byte[1024 * 8];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            logger.info("文件下载成功: {}", localFile.getAbsolutePath());
            return gson.toJson(new ResponseData("文件下载成功,请检查D:\\download路径"));
        } catch (IOException e) {
            logger.error("文件下载失败: {}", e.getMessage(), e);
            return gson.toJson(new ResponseData("文件下载失败: " + e.getMessage()));
        }
    }

    public String delete(DeleteRequest request) {
        String userId = request.getUserId();
        checkUserId(userId);
        String uploadPath = request.getUploadPath();
        uploadPath = formatHdfsPath(uploadPath);

        try (FileSystem fs = FileSystem.get(URI.create(HDFS_ROOT), conf)) {
            Path hdfsPath = new Path(uploadPath);
            if (!fs.exists(hdfsPath)) {
                return gson.toJson(new ResponseData("文件删除失败，文件不存在"));
            }

            // 检查是否为目录，如果是目录则递归删除
            boolean isDir = fs.getFileStatus(hdfsPath).isDirectory();
            boolean success = fs.delete(hdfsPath, isDir);

            return success
                    ? gson.toJson(new ResponseData("文件/目录删除成功"))
                    : gson.toJson(new ResponseData("文件/目录删除失败"));
        } catch (IOException e) {
            logger.error("文件删除失败: {}", e.getMessage(), e);
            return gson.toJson(new ResponseData("文件删除失败: " + e.getMessage()));
        }
    }

    public List<CloudFile> listFiles(String userId, String path) {
        checkUserId(userId);
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        path = formatHdfsPath(path);
        List<CloudFile> fileList = new ArrayList<>();
        try (FileSystem fs = FileSystem.get(URI.create(HDFS_ROOT), conf)) {
            Path targetPath = new Path(path);
            if (!fs.exists(targetPath)) {
                return fileList;
            }
            FileStatus[] fileStatuses = fs.listStatus(targetPath);
            for (FileStatus status : fileStatuses) {
                CloudFile tempFile = new CloudFile();
                tempFile.fileName = status.getPath().getName();
                tempFile.fileSize = status.getLen();
                tempFile.filePath = status.getPath().toString();
                tempFile.userId = userId;
                tempFile.uploadPath = path;
                fileList.add(tempFile);
            }
        } catch (IOException e) {
            logger.error("获取文件列表失败: {}", e.getMessage(), e);
            return fileList;
        }
        return fileList;
    }

    public boolean renameFile(RenameRequest request) {
        String userId = request.getUserId();
        checkUserId(userId);
        String uploadPath = request.getUploadPath();
        String newFileName = request.getNewFileName();
        uploadPath = formatHdfsPath(uploadPath);

        try (FileSystem fs = FileSystem.get(URI.create(HDFS_ROOT), conf)) {
            Path oldPath = new Path(uploadPath);
            Path newPath = new Path(uploadPath.substring(0, uploadPath.lastIndexOf('/') + 1) + newFileName);
            if (fs.exists(newPath)) {
                return false;
            }
            boolean result = fs.rename(oldPath, newPath);
            return result;
        } catch (IOException e) {
            logger.error("文件重命名失败: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean copyFile(CopyRequest request) {
        String sourceUserId = request.getSourceUserId();
        checkUserId(sourceUserId);
        String sourceUploadPath = request.getSourceUploadPath();
        String targetUploadPath = request.getTargetUploadPath();

        sourceUploadPath = formatHdfsPath(sourceUploadPath);
        targetUploadPath = formatHdfsPath(targetUploadPath);

        try (FileSystem fs = FileSystem.get(URI.create(HDFS_ROOT), conf)) {
            Path sourcePath = new Path(sourceUploadPath);
            if (!fs.exists(sourcePath)) {
                logger.error("复制失败，源文件不存在: {}", sourceUploadPath);
                return false;
            }

            // 构建目标路径
            String fileName = sourcePath.getName();
            Path targetPath = new Path(targetUploadPath, fileName);

            // 确保目标目录存在
            if (!fs.exists(targetPath.getParent())) {
                fs.mkdirs(targetPath.getParent());
            }

            // 检查目标文件是否已存在
            if (fs.exists(targetPath)) {
                logger.error("复制失败，目标文件已存在: {}", targetPath);
                return false;
            }

            // 使用流复制处理HDFS内的文件复制
            try (FSDataInputStream in = fs.open(sourcePath);
                 FSDataOutputStream out = fs.create(targetPath)) {

                byte[] buffer = new byte[1024 * 8];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            logger.info("文件复制成功: {} 到 {}", sourcePath, targetPath);
            return true;
        } catch (IOException e) {
            logger.error("文件复制失败: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean moveFile(MoveRequest request) {
        String sourceUserId = request.getSourceUserId();
        checkUserId(sourceUserId);
        String sourceUploadPath = request.getSourceUploadPath();
        String targetUploadPath = request.getTargetUploadPath();

        sourceUploadPath = formatHdfsPath(sourceUploadPath);
        targetUploadPath = formatHdfsPath(targetUploadPath);

        try (FileSystem fs = FileSystem.get(URI.create(HDFS_ROOT), conf)) {
            Path sourcePath = new Path(sourceUploadPath);
            if (!fs.exists(sourcePath)) {
                logger.error("移动失败，源文件不存在: {}", sourceUploadPath);
                return false;
            }

            // 构建目标路径
            String fileName = sourcePath.getName();
            Path targetPath = new Path(targetUploadPath, fileName);

            // 确保目标目录存在
            if (!fs.exists(targetPath.getParent())) {
                fs.mkdirs(targetPath.getParent());
            }

            // 检查目标文件是否已存在
            if (fs.exists(targetPath)) {
                logger.error("移动失败，目标文件已存在: {}", targetPath);
                return false;
            }

            // 移动文件
            boolean success = fs.rename(sourcePath, targetPath);

            logger.info("文件移动成功: {} 到 {}", sourcePath, targetPath);
            return success;
        } catch (IOException e) {
            logger.error("文件移动失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // 内部类定义
    public static class DownloadRequest {
        private String userId;
        private String uploadPath;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUploadPath() {
            return uploadPath;
        }

        public void setUploadPath(String uploadPath) {
            this.uploadPath = uploadPath;
        }
    }

    public static class DeleteRequest {
        private String userId;
        private String uploadPath;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUploadPath() {
            return uploadPath;
        }

        public void setUploadPath(String uploadPath) {
            this.uploadPath = uploadPath;
        }
    }

    public static class RenameRequest {
        private String userId;
        private String uploadPath;
        private String newFileName;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUploadPath() {
            return uploadPath;
        }

        public void setUploadPath(String uploadPath) {
            this.uploadPath = uploadPath;
        }

        public String getNewFileName() {
            return newFileName;
        }

        public void setNewFileName(String newFileName) {
            this.newFileName = newFileName;
        }
    }

    public static class CopyRequest {
        private String sourceUserId;
        private String sourceUploadPath;
        private String targetUserId;
        private String targetUploadPath;
        // 移除本地路径相关字段
        // private String localTargetPath;

        public String getSourceUserId() {
            return sourceUserId;
        }

        public void setSourceUserId(String sourceUserId) {
            this.sourceUserId = sourceUserId;
        }

        public String getSourceUploadPath() {
            return sourceUploadPath;
        }

        public void setSourceUploadPath(String sourceUploadPath) {
            this.sourceUploadPath = sourceUploadPath;
        }

        public String getTargetUserId() {
            return targetUserId;
        }

        public void setTargetUserId(String targetUserId) {
            this.targetUserId = targetUserId;
        }

        public String getTargetUploadPath() {
            return targetUploadPath;
        }

        public void setTargetUploadPath(String targetUploadPath) {
            this.targetUploadPath = targetUploadPath;
        }


    }

    public static class MoveRequest {
        private String sourceUserId;
        private String sourceUploadPath;
        private String targetUserId;
        private String targetUploadPath;
        // 移除本地路径相关字段
        // private String localTargetPath;

        public String getSourceUserId() {
            return sourceUserId;
        }

        public void setSourceUserId(String sourceUserId) {
            this.sourceUserId = sourceUserId;
        }

        public String getSourceUploadPath() {
            return sourceUploadPath;
        }

        public void setSourceUploadPath(String sourceUploadPath) {
            this.sourceUploadPath = sourceUploadPath;
        }

        public String getTargetUserId() {
            return targetUserId;
        }

        public void setTargetUserId(String targetUserId) {
            this.targetUserId = targetUserId;
        }

        public String getTargetUploadPath() {
            return targetUploadPath;
        }

        public void setTargetUploadPath(String targetUploadPath) {
            this.targetUploadPath = targetUploadPath;
        }


    }

    private static class ResponseData {
        private String message;

        public ResponseData(String message) {
            this.message = message;
        }
    }
}