package com.cloudpan.controller;

import com.cloudpan.entity.CloudFile;
import com.cloudpan.service.FileService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;
    private final Gson gson = new Gson();

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String userId,
            @RequestParam String uploadPath) {
        try {
            CloudFile.CloudFileBuilder cloudFileBuilder = new CloudFile.CloudFileBuilder(userId)
                    .withFileName(file.getOriginalFilename())
                    .withFileSize(file.getSize() + "")
                    .withUploadPath(uploadPath);

            String result = fileService.upload(cloudFileBuilder, file.getInputStream());
            // 统一返回JSON格式
            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "文件上传失败: " + e.getMessage());
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/download")
    public ResponseEntity<?> download(
            @RequestBody FileService.DownloadRequest request) {
        try {
            String result = fileService.download(request);
            // 统一返回JSON格式
            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "文件下载失败: " + e.getMessage());
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/download/serve")
    public void serveDownloadedFile(@RequestParam("userId") String userId,
                                    @RequestParam("uploadPath") String uploadPath,
                                    HttpServletResponse response) {
        try {
            // 构建本地文件路径
            String fileName = new File(uploadPath).getName();
            File file = new File("D:\\download\\" + fileName);

            if (file.exists()) {
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

                try (InputStream in = new FileInputStream(file);
                     OutputStream out = response.getOutputStream()) {

                    byte[] buffer = new byte[1024 * 8];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "提供下载文件失败");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(
            @RequestBody FileService.DeleteRequest request) {
        try {
            String result = fileService.delete(request);
            // 统一返回JSON格式
            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "文件删除失败: " + e.getMessage());
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/list/{userId}")
    public ResponseEntity<?> listFiles(
            @PathVariable String userId,
            @RequestParam String path) {
        try {
            List<CloudFile> files = fileService.listFiles(userId, path);
            return new ResponseEntity<>(gson.toJson(files), HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "文件列表获取失败: " + e.getMessage());
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/rename")
    public ResponseEntity<?> renameFile(
            @RequestBody FileService.RenameRequest request) {
        try {
            if (fileService.renameFile(request)) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "文件重命名成功");
                return new ResponseEntity<>(gson.toJson(response), HttpStatus.OK);
            }
            Map<String, String> response = new HashMap<>();
            response.put("message", "文件重命名失败");
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "文件重命名出现异常: " + e.getMessage());
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/copy")
    public ResponseEntity<?> copyFile(
            @RequestBody FileService.CopyRequest request) {
        try {
            // 验证必要参数
            if (request.getSourceUserId() == null ||
                    request.getSourceUploadPath() == null ||
                    request.getTargetUploadPath() == null) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "文件复制失败：缺少必要参数");
                return new ResponseEntity<>(gson.toJson(response), HttpStatus.BAD_REQUEST);
            }

            if (fileService.copyFile(request)) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "文件复制成功");
                return new ResponseEntity<>(gson.toJson(response), HttpStatus.OK);
            }
            Map<String, String> response = new HashMap<>();
            response.put("message", "文件复制失败");
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "文件复制出现异常: " + e.getMessage());
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/move")
    public ResponseEntity<?> moveFile(
            @RequestBody FileService.MoveRequest request) {
        try {
            // 验证必要参数
            if (request.getSourceUserId() == null ||
                    request.getSourceUploadPath() == null ||
                    request.getTargetUploadPath() == null) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "文件移动失败：缺少必要参数");
                return new ResponseEntity<>(gson.toJson(response), HttpStatus.BAD_REQUEST);
            }

            if (fileService.moveFile(request)) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "文件移动成功");
                return new ResponseEntity<>(gson.toJson(response), HttpStatus.OK);
            }
            Map<String, String> response = new HashMap<>();
            response.put("message", "文件移动失败");
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "文件移动出现异常: " + e.getMessage());
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}