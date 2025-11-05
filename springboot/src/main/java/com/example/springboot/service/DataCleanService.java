package com.example.springboot.service;

import com.example.springboot.dao.CommentDao;
import com.example.springboot.dao.ProjectDao;
import com.example.springboot.entity.Comment;
import com.example.springboot.entity.Project;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.util.Pair;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class DataCleanService {

    private final String PYTHON_API = "http://127.0.0.1:5001/clean";

    @Autowired
    private CommentDao commentDao;

    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSyncService dataSyncService;

    @Autowired
    private CommentIndexService commentIndexService;

    /**
     * 上传、清洗、入库、项目管理
     */
    public String processData(MultipartFile file, String projectName, String optionsJson, String userUuid) {
        String pid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            // 0) 新建项目
            if (!StringUtils.hasText(userUuid)) {
                throw new IllegalArgumentException("user_uuid 不能为空");
            }
            Project project = new Project();
            project.setPid(pid);
            project.setProjectName(projectName);
            project.setCleanType(optionsJson == null ? "" : optionsJson.replace("[", "").replace("]", ""));
            project.setCreateTime(now);
            project.setStartTime(now);
            project.setStatus("running");
            project.setUuid(userUuid);
            projectDao.insert(project);

            // 1) 本地临时文件
            File temp = Files.createTempFile("upload_", "_" + file.getOriginalFilename()).toFile();
            file.transferTo(temp);
            log.info("📄 临时文件：{}", temp.getAbsolutePath());

            // 2) 解析文件 -> 原始评论入库（父->子）
            List<Comment> originalList = parseFile(temp);
            log.info("📊 文件解析得到记录：{}", originalList.size());

            Pair<Integer, Integer> rawInsert = insertRawCommentsByOrder(pid, originalList);
            log.info("✅ 原始数据入库完成：父={}，子={}，合计={}",
                    rawInsert.getFirst(), rawInsert.getSecond(), rawInsert.getFirst() + rawInsert.getSecond());

            // 3) 调 Flask 进行清洗
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(temp));
            body.add("project_name", projectName);
            body.add("options", optionsJson);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(PYTHON_API, requestEntity, String.class);
            String resBody = response.getBody();

            JsonNode root = objectMapper.readTree(resBody);
            if (root.has("status") && "success".equals(root.get("status").asText())) {
                String outputPath = root.path("output_path").asText();
                String fileUrl = "http://127.0.0.1:5001/" + outputPath.replace("\\", "/");
                JsonNode previewArray = root.path("preview");

                int cleanedInserted = 0;
                if (previewArray != null && previewArray.isArray()) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                    for (JsonNode item : previewArray) {
                        Comment c = new Comment();
                        c.setPid(pid);
                        c.setCid(UUID.randomUUID().toString());

                        // 内容/用户名长度安全：实体是 255，表是 512，这里按 255 避免校验冲突
                        String content = item.path("content_clean").asText(item.path("content").asText(""));
                        if (content == null) content = "";
                        if (content.length() > 255) content = content.substring(0, 255);
                        c.setContent(content);

                        String username = item.path("username").asText("");
                        if (username == null) username = "";
                        if (username.length() > 50) username = username.substring(0, 50);
                        c.setUsername(username);

                        // 时间
                        String timeStr = item.path("comment_time").asText();
                        try {
                            c.setCommentTime(LocalDateTime.parse(timeStr, formatter));
                        } catch (Exception e) {
                            c.setCommentTime(LocalDateTime.now());
                        }

                        c.setLikeCount(item.path("like_count").asInt(0));
                        c.setReplyCount(item.path("reply_count").asInt(0));
                        c.setCommentType(item.path("comment_type").asInt(0));
                        c.setCleanStatus("cleaned");

                        // 去重：同人 + 同内容 + cleaned
                        Boolean existsCleaned = commentDao.existsByUserContentStatus(c.getUsername(), c.getContent(), "cleaned");
                        if (Boolean.TRUE.equals(existsCleaned)) {
                            continue;
                        }
                        try {
                            commentDao.insert(c);
                            cleanedInserted++;
                        } catch (Exception ex) {
                            log.warn("⚠️ 清洗后入库失败：username={}, contentHash={}, err={}",
                                    c.getUsername(), content.hashCode(), ex.getMessage());
                        }
                    }
                }

                log.info("✅ 清洗后数据入库完成：{}", cleanedInserted);

                // 4) 项目状态 & ES 同步
                project.setStatus("success");
                project.setEndTime(LocalDateTime.now());
                projectDao.updateByPrimaryKey(project);

                commentIndexService.indexCommentsByPid(pid);

                List<Comment> latestCleaned = commentDao.selectRecentCleaned(userUuid, 50);

                ObjectNode successResponse = objectMapper.createObjectNode();
                successResponse.put("status", "success");
                successResponse.put("msg", "清洗完成，预览入库 " + cleanedInserted + " 条（CSV 全量已保存在 Flask 输出文件）");
                successResponse.put("file_url", fileUrl);
                successResponse.set("preview", objectMapper.valueToTree(latestCleaned));
                return successResponse.toString();

            } else {
                updateProjectFail(pid);
                log.error("⚠️ Flask 返回异常：{}", resBody);
                return resBody;
            }

        } catch (Exception e) {
            log.error("❌ 处理失败：", e);
            updateProjectFail(pid);
            return objectMapper.createObjectNode()
                    .put("status", "error")
                    .put("message", "调用 Python 服务失败: " + e.getMessage().replaceAll("[\\r\\n]+", " "))
                    .toString();
        }
    }

    /**
     * 更新项目为失败状态
     */
    private void updateProjectFail(String pid) {
        try {
            Project failProject = new Project();
            failProject.setPid(pid);
            failProject.setStatus("fail");
            failProject.setEndTime(LocalDateTime.now());
            projectDao.updateStatus(failProject);
        } catch (Exception ex) {
            log.error("⚠️ 项目状态更新失败：{}", ex.getMessage(), ex);
        }
    }

    /**
     * 原始评论分批入库（父 -> 子）
     */
    private Pair<Integer, Integer> insertRawCommentsByOrder(String pid, List<Comment> originalList) {
        int parentInserted = 0;
        int childInserted = 0;

        List<Comment> parents = new ArrayList<>();
        List<Comment> children = new ArrayList<>();

        for (Comment c : originalList) {
            if (!StringUtils.hasText(c.getParentCid())) {
                parents.add(c);
            } else {
                children.add(c);
            }
        }

        // 父
        for (Comment c : parents) {
            c.setPid(pid);
            c.setCleanStatus("raw");

            String content = c.getContent() == null ? "" : c.getContent();
            if (content.length() > 255) c.setContent(content.substring(0, 255));
            String username = c.getUsername() == null ? "" : c.getUsername();
            if (username.length() > 50) c.setUsername(username.substring(0, 50));

            Boolean exists = commentDao.existsByUserContentStatus(c.getUsername(), c.getContent(), "raw");
            if (Boolean.TRUE.equals(exists)) continue;

            try {
                commentDao.insert(c);
                parentInserted++;
            } catch (Exception e) {
                log.warn("⚠️ 父评论入库失败：username={}, contentHash={}, err={}",
                        c.getUsername(), content.hashCode(), e.getMessage());
            }
        }

        // 子（如果父不存在，则降级为无父）
        for (Comment c : children) {
            c.setPid(pid);
            c.setCleanStatus("raw");

            if (StringUtils.hasText(c.getParentCid())) {
                Boolean parentOk = commentDao.existsByCid(c.getParentCid());
                if (!Boolean.TRUE.equals(parentOk)) {
                    c.setParentCid(null);
                }
            }

            String content = c.getContent() == null ? "" : c.getContent();
            if (content.length() > 255) c.setContent(content.substring(0, 255));
            String username = c.getUsername() == null ? "" : c.getUsername();
            if (username.length() > 50) c.setUsername(username.substring(0, 50));

            Boolean exists = commentDao.existsByUserContentStatus(c.getUsername(), c.getContent(), "raw");
            if (Boolean.TRUE.equals(exists)) continue;

            try {
                commentDao.insert(c);
                childInserted++;
            } catch (Exception e) {
                log.warn("⚠️ 子评论入库失败：username={}, contentHash={}, err={}",
                        c.getUsername(), content.hashCode(), e.getMessage());
            }
        }

        return Pair.of(parentInserted, childInserted);
    }

    // ---------------------- CSV 解析 ----------------------
    private List<Comment> parseCsv(File file) throws Exception {
        List<Comment> list = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"))) {
            String header = br.readLine();
            if (header == null) return list;

            String line;
            while ((line = br.readLine()) != null) {
                // 逗号分隔（兼容引号）
                String[] arr = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (arr.length < 8) continue;

                Comment c = new Comment();
                c.setCid(safeStr(arr[0]));
                String parent = safeStr(arr[1]);
                c.setParentCid(parent.isEmpty() ? null : parent);

                c.setCommentType(parseIntSafe(arr[2], 0));

                String content = safeStr(arr[3]).replace("\"", "").trim();
                if (content.length() > 255) content = content.substring(0, 255);
                c.setContent(content);

                try {
                    c.setCommentTime(LocalDateTime.parse(safeStr(arr[4]), formatter));
                } catch (Exception e) {
                    c.setCommentTime(LocalDateTime.now());
                }

                String username = safeStr(arr[5]).replace("\"", "").trim();
                if (username.length() > 50) username = username.substring(0, 50);
                c.setUsername(username);

                c.setLikeCount(parseIntSafe(arr[6], 0));
                c.setReplyCount(parseIntSafe(arr[7], 0));

                list.add(c);
            }
        }
        log.info("✅ CSV解析完成，共 {} 条记录。", list.size());
        return list;
    }

    // ---------------------- Excel 解析（无 setCellType，使用 DataFormatter） ----------------------
    private List<Comment> parseExcel(File file) throws Exception {
        List<Comment> list = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(); // 关键：把 Cell 渲染为文本
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) return list;

            Row header = sheet.getRow(0);
            if (header == null) return list;

            // 自动识别列
            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < header.getLastCellNum(); i++) {
                String title = formatter.formatCellValue(header.getCell(i)).trim();
                if (title.contains("评论人") && !title.contains("二级")) colIndex.put("username", i);
                else if (title.contains("评论时间") && !title.contains("二级")) colIndex.put("comment_time", i);
                else if (title.contains("评论内容") && !title.contains("二级")) colIndex.put("content", i);
                else if (title.contains("点赞") && !title.contains("二级")) colIndex.put("like_count", i);
                else if (title.contains("二级评论人")) colIndex.put("reply_username", i);
                else if (title.contains("二级评论时间")) colIndex.put("reply_time", i);
                else if (title.contains("二级评论内容")) colIndex.put("reply_content", i);
                else if (title.contains("二级评论点赞")) colIndex.put("reply_like", i);
            }
            log.info("🧩 Excel字段映射：{}", colIndex);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // 遍历数据
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // 一级评论
                String topCid = UUID.randomUUID().toString();
                Comment top = new Comment();
                top.setCid(topCid);
                top.setParentCid(null);
                top.setCommentType(0);

                String content = getCellString(row, colIndex.get("content"), formatter);
                if (content.length() > 255) content = content.substring(0, 255);
                top.setContent(content);

                String username = getCellString(row, colIndex.get("username"), formatter);
                if (username.length() > 50) username = username.substring(0, 50);
                top.setUsername(username);

                top.setLikeCount(parseIntSafe(getCellString(row, colIndex.get("like_count"), formatter), 0));
                top.setReplyCount(0);

                String timeStr = getCellString(row, colIndex.get("comment_time"), formatter);
                try {
                    top.setCommentTime(LocalDateTime.parse(timeStr, dtf));
                } catch (Exception e) {
                    top.setCommentTime(LocalDateTime.now());
                }
                list.add(top);

                // 二级评论（存在时）
                String replyUser = getCellString(row, colIndex.get("reply_username"), formatter);
                String replyContent = getCellString(row, colIndex.get("reply_content"), formatter);
                if (!replyUser.isEmpty() || !replyContent.isEmpty()) {
                    Comment sub = new Comment();
                    sub.setCid(UUID.randomUUID().toString());
                    sub.setParentCid(topCid);
                    sub.setCommentType(1);

                    if (replyContent.length() > 255) replyContent = replyContent.substring(0, 255);
                    sub.setContent(replyContent);

                    if (replyUser.length() > 50) replyUser = replyUser.substring(0, 50);
                    sub.setUsername(replyUser);

                    sub.setLikeCount(parseIntSafe(getCellString(row, colIndex.get("reply_like"), formatter), 0));
                    sub.setReplyCount(0);

                    String replyTime = getCellString(row, colIndex.get("reply_time"), formatter);
                    try {
                        sub.setCommentTime(LocalDateTime.parse(replyTime, dtf));
                    } catch (Exception e) {
                        sub.setCommentTime(LocalDateTime.now());
                    }
                    list.add(sub);
                }
            }
        }
        log.info("✅ Excel解析完成，共 {} 条记录。", list.size());
        return list;
    }

    // --------- 工具函数（不再使用 setCellType）---------
    private String getCellString(Row row, Integer col, DataFormatter fmt) {
        if (row == null || col == null) return "";
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return fmt.formatCellValue(cell).trim();
    }

    private String safeStr(String s) {
        return s == null ? "" : s.trim();
    }

    private int parseIntSafe(String s, int def) {
        try {
            if (!StringUtils.hasText(s)) return def;
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * 自动识别文件类型（Excel / CSV）
     */
    private List<Comment> parseFile(File file) throws Exception {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            log.info("📘 正在使用 Excel 解析文件: {}", name);
            return parseExcel(file);
        } else if (name.endsWith(".csv")) {
            log.info("📗 正在使用 CSV 解析文件: {}", name);
            return parseCsv(file);
        } else {
            throw new IllegalArgumentException("❌ 不支持的文件类型: " + name);
        }
    }

    // 可选：清洗完成后的 ES 同步封装（目前未直接调用）
    @SuppressWarnings("unused")
    private void afterCleanSuccess(String pid) {
        String msg = dataSyncService.syncProjectCommentsToES(pid);
        log.info(msg);
    }
}
