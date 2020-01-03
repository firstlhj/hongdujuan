package cn.withive.wxpay.controller;

import cn.withive.wxpay.constant.CookieEnum;
import cn.withive.wxpay.model.ResModel;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;

@Component
public class BaseController {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected HttpServletRequest request;

    @Value("${project.name}")
    protected String projectName;

    protected ResModel success(String msg) {
        ResModel resModel = new ResModel();
        resModel.setMsg(msg);
        resModel.setCode(ResModel.StatusEnum.SUCCESS);
        return resModel;
    }

    protected ResModel success(Object data) {
        ResModel resModel = new ResModel();
        resModel.setData(data);
        resModel.setCode(ResModel.StatusEnum.SUCCESS);
        return resModel;
    }

    protected ResModel success(String msg, Object data) {
        ResModel resModel = new ResModel();
        resModel.setMsg(msg);
        resModel.setData(data);
        resModel.setCode(ResModel.StatusEnum.SUCCESS);
        return resModel;
    }

    protected ResModel fail(String msg) {
        ResModel resModel = new ResModel();
        resModel.setMsg(msg);
        resModel.setCode(ResModel.StatusEnum.FAILURE);
        return resModel;
    }

    protected ResModel fail(Object data) {
        ResModel resModel = new ResModel();
        resModel.setData(data);
        resModel.setCode(ResModel.StatusEnum.FAILURE);
        return resModel;
    }

    protected ModelAndView errorView(Exception ex) {
        ModelAndView view = new ModelAndView();
        view.addObject("ex", new IllegalArgumentException("页面不存在区域编号"));
        view.addObject("url", request.getRequestURL());
        view.setViewName("error/50x");
        return view;
    }

    public void showCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (null == cookies) {//如果没有cookie数组
            System.out.println("没有cookie");
        } else {
            for (Cookie cookie : cookies) {
                System.out.println("cookieName:" + cookie.getName() + ",cookieValue:" + cookie.getValue());
            }
        }
    }

    protected void addCookie(CookieEnum name, String value) {
        this.addCookie(name.name(), value);
    }

    @Deprecated
    protected void addCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(2 * 60 * 60);
        cookie.setPath("/");
        getResponse().addCookie(cookie);
    }

    protected void removeCookie(String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        getResponse().addCookie(cookie);
    }

    protected void addOpenIdToCookie(String value) {
        // TODO: Use enum replace string
        addCookie("openId", value);
    }

    protected void removeOpenId() {
        removeCookie("openId");
    }

    protected String getClientIp() {

        String ip = request.getHeader("X-Real-IP");
        if (!StringUtils.isEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.isEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP。
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index);
            } else {
                return ip;
            }
        } else {
            return request.getRemoteAddr();
        }
    }

    protected String getRequestURL() {
        String requestUrl = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        if (!StringUtils.isEmptyOrWhitespace(queryString)) {
            requestUrl = requestUrl + "?" + queryString;
        }

//        URL requestURL = new URL(request.getRequestURL().toString());
//        String port = requestURL.getPort() == -1 ? "" : ":" + requestURL.getPort();
//        return requestURL.getProtocol() + "://" + requestURL.getHost() + port;


        return requestUrl;
    }

    protected HttpServletRequest getRequest() {
        ServletRequestAttributes servletRequestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        HttpServletRequest request = servletRequestAttributes.getRequest();
        return request;
    }

    protected HttpServletResponse getResponse() {
        ServletRequestAttributes servletRequestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = servletRequestAttributes.getResponse();


        return response;
    }
}
