package cn.withive.wxpay.controller;

import cn.withive.wxpay.constant.OrderStatusEnum;
import cn.withive.wxpay.constant.RedirectViewEnum;
import cn.withive.wxpay.entity.Order;
import cn.withive.wxpay.entity.WechatUser;
import cn.withive.wxpay.model.ResModel;
import cn.withive.wxpay.model.TreesSuccessModel;
import cn.withive.wxpay.service.OrderService;
import cn.withive.wxpay.service.WXService;
import cn.withive.wxpay.service.WechatUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.util.StringUtils;

import java.util.Map;

/**
 * @author qiu xiaobing
 * @date 2019/12/6 20:35
 */
@Controller
@RequestMapping("/user")
public class UserController extends BaseController {

    @Autowired
    private WechatUserService wechatUserService;

    @GetMapping("/getOrderCount")
    @ResponseBody
    public ResModel getJsApiPay(@CookieValue(value = "openId", required = false) String openId) {
        try {
            if (StringUtils.isEmptyOrWhitespace(openId)) {
                return fail("支付缺少必要参数：微信用户参数");
            }

            boolean exists = wechatUserService.existsByOpenId(openId);
            if (!exists) {
                // 不存在此用户
                return fail("不存在此用户");
            }

            int count = wechatUserService.getOrderCount(openId);

            return success(count);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage());

            return fail(ex.getMessage());
        }
    }
}
