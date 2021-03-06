package cn.withive.wxpay.controller;

import cn.withive.wxpay.entity.WechatUser;
import cn.withive.wxpay.service.WXService;
import cn.withive.wxpay.service.WechatUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.util.StringUtils;

import java.util.Map;

/**
 * @author qiu xiaobing
 * @date 2019/12/6 20:35
 */
@Controller
@RequestMapping("/cert")
public class CertController extends BaseController {

    @Autowired
    private WXService wxService;

    @Autowired
    private WechatUserService wechatUserService;

    /**
     * 用户证书页面
     *
     * @param openId
     * @return
     */
    @RequestMapping({"", "/index"})
    public ModelAndView index(@CookieValue(value = "openId", required = false) String openId) {

        ModelAndView view = new ModelAndView("cert/index");
        ModelAndView homeView = new ModelAndView("redirect:/");

        if (StringUtils.isEmptyOrWhitespace(openId)) {
            // openId 不存在，重新跳转去验证
            return homeView;
        }

        WechatUser wechatUser = wechatUserService.findByOpenId(openId);
        if (wechatUser == null) {
            // 不存在此用户
            return homeView;
        }

        Long count = wechatUserService.getOrderCount(openId);
        if (count < 1) {
            // 没有种植过
            return homeView;
        }

        Map<String, String> config = wxService.getJsApiConfig(getRequestURL());
        view.addObject("jsapi", config);
        view.addObject("orderCount", count);
        String name = StringUtils.isEmptyOrWhitespace(wechatUser.getRealName()) ? wechatUser.getNickname() :
                wechatUser.getRealName();
        view.addObject("name", name);

        return view;
    }
}
