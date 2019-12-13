package cn.withive.wxpay.controller;

import cn.withive.wxpay.model.ResModel;
import cn.withive.wxpay.service.WXService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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

    /**
     * 获取微信权限验证配置
     * @return
     */
    @PostMapping("/getConfig")
    @ResponseBody
    public ResModel getConfig(String url) {
        if (StringUtils.isEmptyOrWhitespace(url)) {
            url = request.getHeader("Referer");
        }

        if (StringUtils.isEmpty(url)) {
            return fail("缺少必要参数：访问页面url");
        }


        try {
            String accessToken = WXService.getGlobalToken();

            if (StringUtils.isEmpty(accessToken)) {
                return fail("获取微信js权限验证配置异常");
            }

            String ticket = WXService.getJsApiTicket(accessToken);

            if (StringUtils.isEmpty(ticket)) {
                return fail("获取微信js权限验证配置异常");
            }

            Map<String, String> config = WXService.getJsApiConfig(ticket, url);

            return success(config);

        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage());

            return fail(ex.getMessage());
        }
    }
}
