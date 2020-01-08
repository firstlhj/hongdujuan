package cn.withive.wxpay.controller;

import cn.withive.wxpay.entity.WechatUser;
import cn.withive.wxpay.exception.TokenExpireException;
import cn.withive.wxpay.exception.WxException;
import cn.withive.wxpay.model.WXUserTokenModel;
import cn.withive.wxpay.model.WXUserInfoModel;
import cn.withive.wxpay.service.AreaService;
import cn.withive.wxpay.service.OrderService;
import cn.withive.wxpay.service.WXService;
import cn.withive.wxpay.service.WechatUserService;
import cn.withive.wxpay.util.RandomUtil;
import jdk.nashorn.internal.runtime.regexp.joni.exception.InternalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.util.StringUtils;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Map;

import static cn.withive.wxpay.constant.RedirectViewEnum.valueOf;

@Controller
@RequestMapping({"", "/home"})
public class HomeController extends BaseController {

    @Autowired
    private WXService wxService;

    @Autowired
    private WechatUserService wechatUserService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AreaService areaService;

    /**
     * 微信验证回调
     *
     * @param redirectUri
     * @param state
     * @return
     */
    @RequestMapping("/authorize")
    public String authorize(String redirectUri, String state) {

        if (StringUtils.isEmptyOrWhitespace(redirectUri)) {
            redirectUri = wxService.getConfig().getServerUrl();
        }

        String url = wxService.getAuthorizeUrl(redirectUri, "snsapi_userinfo", state);
        return "redirect:" + url;
    }

    @RequestMapping({"", "/index"})
    public ModelAndView index(@CookieValue(value = "openId", required = false) String openId, String code,
                              String state, String area) throws WxException {
        if (StringUtils.isEmptyOrWhitespace(state) && StringUtils.isEmptyOrWhitespace(area)) {
            return errorView(new IllegalArgumentException("页面不存在区域编号"));
        }
        if (!StringUtils.isEmptyOrWhitespace(area)) {
            boolean existArea = areaService.exist(area);
            if (!existArea) {
                throw new EntityNotFoundException("不存在区域编号：" + area);
            }
        }

        ModelAndView view = new ModelAndView("home/index");

        if (StringUtils.isEmptyOrWhitespace(openId)) {
            if (StringUtils.isEmptyOrWhitespace(code)) {
                // 没有openId,也没有code,那么重定向到微信验证页面
                view.setViewName(redirectToAuthorize(area));
                return view;
            } else {
                WXUserTokenModel userToken = wxService.getUserToken(code);
                if (userToken == null) {
                    throw new InternalException("获取微信用户授权数据异常");
                }
                addOpenIdToCookie(userToken.getOpenid());

                // 避免地址栏上出现code参数
                view.setViewName(redirectToHome(state));
                return view;
            }
        }

        try {
            WechatUser user = wechatUserService.syncInfo(openId);
            Map<String, String> config = wxService.getJsApiConfig(getRequestURL());
            boolean isExist = orderService.existsByWechatOpenIdAndPaid(openId);

            view.addObject("jsapi", config);
            view.addObject("showMyTree", isExist);
            view.addObject("realName", user.getRealName());
            view.addObject("phone", user.getPhone());

            return view;
        } catch (TokenExpireException e) {
            removeOpenIdFromCookie();
            view.setViewName(redirectToAuthorize(area));
            return view;
        }
    }
}
