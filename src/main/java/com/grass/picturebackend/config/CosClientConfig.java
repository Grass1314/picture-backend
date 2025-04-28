package com.grass.picturebackend.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mr.Liuxq
 * @description: cos 配置
 * @date 2025年04月27日 11:50
 */
@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {

    /**
     * 腾讯云对象存储，COS 域名
     */
    private String host;

    /**
     * 腾讯云对象存储，COS id
     */
    private String secretId;

    /**
     * 腾讯云对象存储，COS 密钥
     */
    private String secretKey;

    /**
     * 腾讯云对象存储，COS 区域
     */
    private String region;

    /**
     * 腾讯云对象存储，COS 桶名称
     */
    private String bucket;

    @Bean
    public COSClient cosClient() {
        // 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 设置bucket的区域, COS地域的简称请参照 https://cloud.tencent.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 生成cos客户端
        return new COSClient(cred, clientConfig);
    }
}
