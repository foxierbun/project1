package com.cloudpan.config;
//配置类
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;


//声明这是一个 Spring 配置类，Spring 容器启动时会扫描此类，并将其内部的配置逻辑（如重写的方法）加载到容器中
@Configuration
//实现 WebMvcConfigurer 接口，该接口提供了一组回调方法，用于自定义 Spring MVC 的核心配置（如静态资源、内容协商、拦截器等）
public class WebConfig implements WebMvcConfigurer {
    //定义一个日志记录器（基于 JDK 自带的 java.util.logging），用于在配置类中记录关键日志（此处未直接使用）
    private static final Logger logger = Logger.getLogger(WebConfig.class.getName());


    //用于配置静态资源的映射规则，
    // 核心作用是
    // 告诉 Spring MVC：“当用户请求 /favicon.ico 时，去哪个目录找这个文件？”
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {//registry 像是一个实例名
        //声明需要处理的静态资源路径模式 ，favicon.ico 指定的是网站的 “收藏夹图标”（浏览器标签页左上角的小图标），路径固定为 /favicon.ico
        registry.addResourceHandler("/favicon.ico")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(0);
        //指定静态资源的存储位置。
        // classpath:/static/ 表示从类路径下的 static 目录中查找资源（
        // 例如 src/main/resources/static/favicon.ico）
        //设置资源的缓存周期（单位：秒）。
        // 这里若设置为 0 表示不缓存，修改 favicon.ico 后无需重启应用即可生效
        // （生产环境中可能需要设置合理的缓存时间，如 3600 秒）
    }


    //内容协商配置
    //这部分代码用于配置内容协商策略，核心作用是告诉 Spring MVC：“当用户请求数据时，应该返回什么格式（如 JSON、XML）？”


    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        //configurer实例的 禁用通过请求参数（如 ?format=json）来指定响应格式。
        // 例如，即使请求是 /user?format=xml，Spring 也不会返回 XML
        configurer.favorParameter(false)
            .ignoreAcceptHeader(true)
            .defaultContentType(MediaType.APPLICATION_JSON);
        //ignoreAcceptHeader(true)：
        //忽略 Accept 请求头。例如，即使请求头是 Accept: application/xml，Spring 也不会返回 XML。

        //defaultContentType(MediaType.APPLICATION_JSON)：
        //设置默认的响应格式为 JSON（application/json）。如果没有其他协商方式（如 Accept 头、请求参数），Spring 会强制返回 JSON
    }

    
    
}

//总结：这个配置类的核心作用
//静态资源管理：明确 favicon.ico 的存储位置和缓存策略，确保浏览器能正确加载网站图标。
//强制 JSON 响应：忽略客户端的格式请求（如 Accept 头、参数），统一返回 JSON，简化前后端交互（适合前后端分离项目）