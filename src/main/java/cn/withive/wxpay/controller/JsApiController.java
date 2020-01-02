package cn.withive.wxpay.controller;

import cn.withive.wxpay.entity.WechatUser;
import cn.withive.wxpay.model.ResModel;
import cn.withive.wxpay.service.WXService;
import cn.withive.wxpay.service.WechatUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.util.StringUtils;

import java.util.Map;

@Controller
@RequestMapping("/jspai")
public class JsApiController extends BaseController {

    @Autowired
    private WXService WXService;

    @Autowired
    private WechatUserService wechatUserService;

    /**
     * 获取微信权限验证配置
     * @return
     */
//    @PostMapping("/getConfig")
//    @ResponseBody
//    public ResModel getConfig(@CookieValue(value = "openId") String openId, String url) {
//        if (StringUtils.isEmptyOrWhitespace(openId)) {
//            return fail("缺少必要参数：微信用户id");
//        }
//        if (StringUtils.isEmptyOrWhitespace(url)) {
//            url = request.getHeader("Referer");
//        }
//        if (StringUtils.isEmpty(url)) {
//            return fail("缺少必要参数：访问页面url");
//        }
//
//        boolean exists = wechatUserService.existsByOpenId(openId);
//        if (!exists) {
//            // 不存在此用户
//            return fail("系统中不存在此用户");
//        }
//
//        try {
//            Map<String, String> config = WXService.getJsApiConfig(url);
//            if (config == null) {
//                return fail("获取微信js权限验证配置异常");
//            }
//
//            return success(config);
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            log.error(ex.getMessage());
//
//            return fail(ex.getMessage());
//        }
//    }
}
