package com.grass.picturebackend.config;

import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author Mr.Liuxq
 * @description: 请求包装过滤器
 * @date 2025年05月13日 11:43
 */
// 优先级最高
@Order(1)
@Component
public class HttpRequestWrapperFilter implements Filter {

    /**
     * @param servletRequest
     * @param servletResponse
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
            if (ContentType.JSON.getValue().equals(contentType)) {
                filterChain.doFilter(new RequestWrapper(request), servletResponse);
            } else {
                filterChain.doFilter(servletRequest, servletResponse);
            }
        }
    }
}
