package cn.withive.wxpay.controller;

import cn.withive.wxpay.constant.OrderStatusEnum;
import cn.withive.wxpay.constant.RedirectViewEnum;
import cn.withive.wxpay.entity.WechatUser;
import cn.withive.wxpay.model.WXAccessTokenModel;
import cn.withive.wxpay.model.WXUserInfoModel;
import cn.withive.wxpay.service.OrderService;
import cn.withive.wxpay.service.WXService;
import cn.withive.wxpay.service.WechatUserService;
import cn.withive.wxpay.util.RandomUtil;
import jdk.nashorn.internal.runtime.regexp.joni.exception.InternalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.util.StringUtils;

import java.time.LocalDateTime;

import static cn.withive.wxpay.constant.RedirectViewEnum.valueOf;

@Controller
@RequestMapping("/home")
public class HomeController extends BaseController {

    @Autowired
    private WXService WXService;

    @Autowired
    private WechatUserService wechatUserService;

    @Autowired
    private OrderService orderService;

    @RequestMapping("/authorize")
    public String authorize(String redirectUri, String state) {

        if (StringUtils.isEmptyOrWhitespace(redirectUri)) {
            redirectUri = WXService.getConfig().getServerUrl() + "/home/index";
        }

        String url = WXService.getAuthorizeUrl(redirectUri, "snsapi_userinfo", state);
        return "redirect:" + url;
    }

    @RequestMapping({"", "/index"})
    public ModelAndView index(@CookieValue(value = "openId", required = false) String openId, String code,
                              String state) {
        ModelAndView view = new ModelAndView("home/index");

        if (StringUtils.isEmptyOrWhitespace(openId)) {
            if (StringUtils.isEmptyOrWhitespace(code)) {
                String url = StringUtils.isEmptyOrWhitespace(code) ? "redirect:/home/authorize"
                        : "redirect:/home/authorize?state=" + state;

                view.setViewName(url);
                return view;
            } else {
                WXAccessTokenModel accessTokenModel = WXService.getAccessToken(code);
                if (accessTokenModel == null) {
                    throw new InternalException("获取微信用户授权数据异常");
                }

                openId = accessTokenModel.getOpenid();

                addOpenId(openId);

                // 避免地址栏上出现code参数
                view.setViewName("redirect:/home/index");
                return view;
            }
        }

        // 检查数据库中是否存在该用户信息
        boolean exist = wechatUserService.existsByOpenId(openId);
        if (!exist) {
            // 数据库中不存在才保存微信用户信息
            String token = WXService.getAccessTokenFormCache(openId);

            if (StringUtils.isEmpty(token)) {
                // token 失效了
                removeOpenId();
                view.setViewName("redirect:/home/index");
                return view;
            }

            WXUserInfoModel userInfoModel = WXService.getUserInfo(token, openId);

            if (userInfoModel == null) {
                throw new InternalException("获取微信用户信息异常");
            }

            WechatUser user = new WechatUser();
            user.setId(RandomUtil.generateUniqueStr());
            user.setCreatTime(LocalDateTime.now());
            user.setOpenId(openId);
            user.setNickname(userInfoModel.getNickname());
            user.setAvatar(userInfoModel.getHeadimgurl());
            user.setCountry(userInfoModel.getCountry());
            user.setProvince(userInfoModel.getProvince());
            user.setCity(userInfoModel.getCity());

            wechatUserService.save(user);
        }

        if (!StringUtils.isEmptyOrWhitespace(state)) {
            try  {
                RedirectViewEnum viewEnum = valueOf(state);

                switch (viewEnum) {
                    case pay:
                        view.setViewName("redirect:/pay/index");
                        break;
                    case product:
                        view.setViewName("redirect:/product/index");
                        break;
                }
            } catch (IllegalArgumentException ignored) {

            }
        }

        boolean isExist = orderService.existsByWechatOpenIdAndPaid(openId);
        // 用户曾经下过订单，那么页面上显示我的认种页面
        view.addObject("showMyTree", isExist);

        return view;
    }
}
