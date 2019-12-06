package cn.withive.wxpay.controller;

import cn.withive.wxpay.model.ResModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    protected ResModel ok(Object data) {
        ResModel resModel = new ResModel();
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

    protected void addCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(2 * 60 * 60);
        cookie.setPath("/");
        getResponse().addCookie(cookie);
    }

    protected void removeCookie(String name) {
        Cookie cookie = new Cookie(name,null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        getResponse().addCookie(cookie);
    }

    protected void addOpenId(String value) {
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
