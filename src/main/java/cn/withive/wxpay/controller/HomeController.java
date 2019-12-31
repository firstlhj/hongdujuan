package cn.withive.wxpay.controller;

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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${user.sync_info_day}")
    private Integer syncUserInfoDay;

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
                String url = "redirect:/home/authorize";
                if (!StringUtils.isEmptyOrWhitespace(state)) {
                    url += "?state=" + state;
                }

                // 没有openId,也没有code,那么重定向到微信验证页面
                view.setViewName(url);
                return view;
            } else {
                WXAccessTokenModel accessTokenModel = WXService.getAccessToken(code);
                if (accessTokenModel == null) {
                    throw new InternalException("获取微信用户授权数据异常");
                }
                openId = accessTokenModel.getOpenid();
                addOpenIdToCookie(openId);

                String url = "redirect:/home/index";
                if (!StringUtils.isEmptyOrWhitespace(state)) {
                    url += "?state=" + state;
                }

                // 避免地址栏上出现code参数
                view.setViewName(url);
                return view;
            }
        }

        WechatUser user = wechatUserService.findByOpenId(openId);
        if (user == null) {
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

            user = new WechatUser();
            user.setId(RandomUtil.generateUniqueStr());
            user.setCreatTime(LocalDateTime.now());
            user.setOpenId(openId);
            user.setNickname(userInfoModel.getNickname());
            user.setAvatar(userInfoModel.getHeadimgurl());
            user.setCountry(userInfoModel.getCountry());
            user.setProvince(userInfoModel.getProvince());
            user.setCity(userInfoModel.getCity());
            wechatUserService.save(user);
        } else {
            // 用户信息超过七天,或者缺少详细信息,那么拉取更新用户最新个人信息
            if (user.getCreatTime().plusDays(syncUserInfoDay).isBefore(LocalDateTime.now()) || StringUtils.isEmptyOrWhitespace(user.getNickname())) {
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

                user.setCreatTime(LocalDateTime.now());
                user.setNickname(userInfoModel.getNickname());
                user.setAvatar(userInfoModel.getHeadimgurl());
                user.setCountry(userInfoModel.getCountry());
                user.setProvince(userInfoModel.getProvince());
                user.setCity(userInfoModel.getCity());
                wechatUserService.save(user);
            }
        }


//        if (!StringUtils.isEmptyOrWhitespace(state)) {
//            try {
//                RedirectViewEnum viewEnum = valueOf(state);
//
//                switch (viewEnum) {
//                    case pay:
//                        view.setViewName("redirect:/pay/index");
//                        break;
//                    case product:
//                        view.setViewName("redirect:/product/index");
//                        break;
//                }
//            } catch (IllegalArgumentException ignored) {
//
//            }
//        }

        boolean isExist = orderService.existsByWechatOpenIdAndPaid(openId);
        // 用户曾经下过订单，那么页面上显示我的认种页面
        view.addObject("showMyTree", isExist);

        return view;
    }
}
