package com.tlcsdm.psdcd;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.net.UserPassAuthenticator;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.hutool.poi.excel.style.StyleUtil;
import org.apache.poi.ss.usermodel.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Girret数据读取
 */
public class GirretTest {

    //用户配置
    // cookie GerritAccount
    private final static String gerritAccountValue = "aQIiprqRsF8g0Y10wLs8xgHhOmuNZuuKBW";
    // cookie XSRF_TOKEN
    private final static String tokenValue = "aQIiprt-AbdNE069k01HfzMWbVQk0bMm.G";
    // girret userName
    private final static String userName = "liang.tang.yk";
    // girret password
    private final static String password = "xxxxxx";
    // 开始索引
    private int paramS = 0;
    // 每次查询数量
    private final int paramN = 50;
    // 结果信息输出路径
    private final String resultPath = "C:\\workspace\\test";
    // 结果文件名称(目前支持结果输出到excel)
    private final String resultFileName = "GirretComments.xlsx";
    // 是否保留json文件到resultPath路径下
    private final boolean reserveJson = false;

    //参数配置
    private final static String paramO = "81";
    private final static String paramQ = "is:closed -is:ignored (-is:wip OR owner:self) (owner:self OR reviewer:self OR assignee:self OR cc:self)";
    // cookie 作用域
    private final static String cookieAddrString = "http://172.29.44.217/";
    // changes请求路径
    private final String changesRequestUrl = "http://172.29.44.217/changes/?O=%s&S=%s&n=%s&q=%s";
    // comments请求路径
    private final String commentsRequestUrl = "http://172.29.44.217/changes/e2-studio-eclipse%2Ftools-smartconfigurator~%s/comments";

    // 待初始化对象
    private static HttpClient client;
    private List<Map<String, String>> changesList = new ArrayList<>();
    private List<Map<String, String>> commentsList = new ArrayList<>();

    @BeforeClass
    public static void init() {
        CookieManager manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        HttpCookie gerritAccount = new HttpCookie("GerritAccount", gerritAccountValue);
        HttpCookie token = new HttpCookie("XSRF_TOKEN", tokenValue);
        manager.getCookieStore().add(URI.create(cookieAddrString), gerritAccount);
        manager.getCookieStore().add(URI.create(cookieAddrString), token);
        Authenticator authenticator = new UserPassAuthenticator(userName, password.toCharArray());
        client = HttpClient.newBuilder().version(Version.HTTP_1_1).followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(5000)).authenticator(authenticator).cookieHandler(manager).build();
    }

    /**
     * girret 提交信息读取
     */
    @Test
    public void GirretTest1() throws IOException, InterruptedException, UnsupportedEncodingException {
        Console.log("================== Getting changes ==================");
        for (; ; ) {
            String url = String.format(changesRequestUrl, URLEncoder.encode(paramO, StandardCharsets.UTF_8),
                    paramS, paramN, URLEncoder.encode(paramQ, StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().headers("Content-Type", "application/json",
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36 Edg/105.0.1343.50")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String result = response.body();
                if (result.startsWith(")]}'")) {
                    result = result.substring(4);
                }
                //JSONArray array = JSONUtil.readJSONArray(FileUtil.file(ResourceUtil.getResource("static/private/girret/changes.json")), CharsetUtil.CHARSET_UTF_8);
                JSONArray array = JSONUtil.parseArray(result);
                handleChanges(array);
                if (array.size() < paramN) {
                    break;
                }
                paramS = paramS + paramN;
            } else {
                Console.error("changes request call failed", response.body());
                break;
            }
            break;
        }
        Console.log("================== Getting comments ==================");
        handleComments();
        Console.log("================== END ==================");
    }

    /**
     * girret changes 数据处理
     */
    private void handleChanges(JSONArray array) {
        for (int i = 0; i < array.size(); i++) {
            if (changesFilter(array.get(i), array, i)) {
                Map<String, String> map = new HashMap<>();
                map.put("girretNum", String.valueOf(array.getByPath("[" + i + "]._number")));
                map.put("changeId", String.valueOf(array.getByPath("[" + i + "].change_id")));
                map.put("subject", String.valueOf(array.getByPath("[" + i + "].subject")));
                map.put("created", String.valueOf(array.getByPath("[" + i + "].created")));
                map.put("submitted", String.valueOf(array.getByPath("[" + i + "].submitted")));
                map.put("insertions", String.valueOf(array.getByPath("[" + i + "].insertions")));
                map.put("deletions", String.valueOf(array.getByPath("[" + i + "].deletions")));
                map.put("ownerName", String.valueOf(array.getByPath("[" + i + "].owner.name")));
                map.put("ownerUserName", String.valueOf(array.getByPath("[" + i + "].owner.username")));
                changesList.add(map);
            }
        }
        //可增加后续数据处理
    }

    /**
     * changes数据过滤
     */
    private boolean changesFilter(Object obj, JSONArray array, int i) {
        if (!Objects.equals("MERGED", array.getByPath("[" + i + "].status"))) {
            return false;
        }
        //可添加自定义过滤条件
        return true;
    }

    /**
     * girret comments 数据处理
     */
    private void handleComments() throws IOException, InterruptedException {
        for (int i = 0; i < changesList.size(); i++) {
            String url = String.format(commentsRequestUrl, changesList.get(i).get("girretNum"));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().headers("Content-Type", "application/json",
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36 Edg/105.0.1343.50")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String result = response.body();
                if (result.startsWith(")]}'")) {
                    result = result.substring(4);
                }
                JSONObject jsonObject = JSONUtil.parseObj(result);
                //JSONObject jsonObject = JSONUtil.readJSONObject(FileUtil.file(ResourceUtil.getResource("static/private/girret/comments.json")), CharsetUtil.CHARSET_UTF_8);
                jsonObject.remove("/PATCHSET_LEVEL");
                for (Map.Entry<String, Object> vo : jsonObject.entrySet()) {
                    Map<String, String> map = new HashMap<>(changesList.get(i));
                    map.put("commentFileName", vo.getKey());
                    JSONArray array = JSONUtil.parseArray(vo.getValue());
                    for (int j = 0; j < array.size(); j++) {
                        String commentMessage = String.valueOf(array.getByPath("[" + j + "].message"));
                        String commentAutherUserName = String.valueOf(array.getByPath("[" + j + "].author.username"));
                        if ("Done".equals(commentMessage) || map.get("ownerUserName").equals(commentAutherUserName)) {
                            continue;
                        }
                        Map<String, String> comment = new HashMap<>(map);
                        comment.put("commentAutherUserName", commentAutherUserName);
                        comment.put("commentAutherName", String.valueOf(array.getByPath("[" + j + "].author.name")));
                        comment.put("commentMessage", commentMessage);
                        commentsList.add(comment);
                    }
                }
            } else {
                Console.error("comments request call failed", response.body());
                break;
            }
        }
        handleResult();
    }

    /**
     * 数据结果处理
     */
    private void handleResult() {
        if (commentsList.size() == 0) {
            Console.log("No need comments");
            return;
        }
        ExcelWriter writer = ExcelUtil.getWriter(FileUtil.file(resultPath, resultFileName));
        setExcelStyle(writer);
        writer.setOnlyAlias(true);
        writer.addHeaderAlias("girretNum", "Girret Number");
        writer.addHeaderAlias("changeId", "changeId");
        writer.addHeaderAlias("subject", "subject");
        writer.addHeaderAlias("commentAutherName", "指摘人");
        writer.addHeaderAlias("commentMessage", "指摘信息");
        writer.addHeaderAlias("submitted", "合并时间");
        writer.addHeaderAlias("created", "开始时间");
        writer.addHeaderAlias("insertions", "新增行数");
        writer.addHeaderAlias("deletions", "删除行数");
        writer.addHeaderAlias("commentFileName", "指摘文件名");
        writer.write(commentsList, true);
        writer.close();
        //保留json结果文件
        if (reserveJson) {
            FileUtil.writeUtf8String(JSONUtil.toJsonPrettyStr(changesList), FileUtil.file(resultPath, LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + "-changes.json"));
            FileUtil.writeUtf8String(JSONUtil.toJsonPrettyStr(commentsList), FileUtil.file(resultPath, LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + "-comments.json"));
        }
    }

    // 设置生成的excel样式
    private void setExcelStyle(ExcelWriter writer) {
        writer.getStyleSet().setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        writer.getHeadCellStyle().setAlignment(HorizontalAlignment.CENTER);
        CellStyle style = writer.getStyleSet().getHeadCellStyle();
        StyleUtil.setColor(style, IndexedColors.LIGHT_YELLOW, FillPatternType.SOLID_FOREGROUND);
        writer.setColumnWidth(0, 15);
        writer.setColumnWidth(1, 45);
        writer.setColumnWidth(2, 50);
        writer.setColumnWidth(3, 15);
        writer.setColumnWidth(4, 60);
        writer.setColumnWidth(5, 40);
        writer.setColumnWidth(6, 40);
        writer.setColumnWidth(7, 10);
        writer.setColumnWidth(8, 10);
        writer.setColumnWidth(9, 80);
    }

}