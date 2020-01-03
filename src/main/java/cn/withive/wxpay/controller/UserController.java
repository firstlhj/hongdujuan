package cn.withive.wxpay.controller;

import cn.withive.wxpay.entity.WechatUser;
import cn.withive.wxpay.model.ResModel;
import cn.withive.wxpay.model.UserModel;
import cn.withive.wxpay.service.WXService;
import cn.withive.wxpay.service.WechatUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.util.StringUtils;

import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController extends BaseController {

    @Autowired
    private WXService WXService;

    @Autowired
    private WechatUserService wechatUserService;

    @PostMapping("/sign")
    @ResponseBody
    public ResModel sign(@CookieValue(value = "openId") String openId, @RequestBody UserModel model) {
        if (StringUtils.isEmptyOrWhitespace(openId)) {
            return fail("缺少必要参数：微信用户id");
        }
        if (!StringUtils.isEmptyOrWhitespace(model.getSignature())) {
            if (model.getSignature().length() > 500) {
                return fail("用户签名不能超过500字");
            }
        }

        WechatUser user = wechatUserService.findByOpenId(openId);
        if (user == null) {
            return fail("系统中不存在此用户");
        }

        user.setSignature(model.getSignature());
        wechatUserService.save(user);
        return success(true);
    }

//    @PostMapping("/update")
//    @ResponseBody
//    public ResModel update(@CookieValue(value = "openId") String openId, @RequestBody UserModel model) {
//        if (StringUtils.isEmptyOrWhitespace(openId)) {
//            return fail("缺少必要参数：微信用户id");
//        }
//        if (StringUtils.isEmptyOrWhitespace(model.getRealName())) {
//            return fail("缺少必要参数：姓名");
//        }
//        if (StringUtils.isEmptyOrWhitespace(model.getPhone())) {
//            return fail("缺少必要参数：手机");
//        }
//
//        WechatUser user = wechatUserService.findByOpenId(openId);
//        if (user == null) {
//            return fail("系统中不存在此用户");
//        }
//
//        user.setPhone(model.getPhone());
//        user.setRealName(model.getRealName());
//        wechatUserService.save(user);
//        return success(true);
//    }
}
