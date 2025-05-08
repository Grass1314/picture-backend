package com.grass.picturebackend.api.imagesearch.sub;

import com.grass.picturebackend.exception.BusinessException;
import com.grass.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mr.Liuxq
 * @description: 获取图片列表的页面地址
 * @date 2025年05月07日 08:42
 */
@Slf4j
public class GetImageFirstUrlApi {

    /**
     * 获取图片列表页面地址
     * @param url 搜索地址
     * @return 图片列表页面地址
     */
    public static String getImageFirstUrl(String url) {
        try {
            Document document = Jsoup.connect(url).timeout(5000).get();
            // 获取所有<script>
            Elements scriptElements = document.getElementsByTag("script");

            // 遍历到包含‘firstUrl’的脚本内容
            for (Element script : scriptElements) {
                String scriptContent = script.html();
                if (scriptContent.contains("\"firstUrl\"")) {
                    // 使用正则表达式提取firstUrl的值
                    Pattern pattern = Pattern.compile("\"firstUrl\"\\s*:\\s*\"(.*?)\"");
                    Matcher matcher = pattern.matcher(scriptContent);
                    if (matcher.find()) {
                        String firstUrl = matcher.group(1);
                        // 处理转义字符
                        firstUrl = firstUrl.replace("\\/", "/");
                        return firstUrl;
                    }
                }
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到 Url");
        } catch (Exception e) {
            log.error("搜索失败");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }
}
