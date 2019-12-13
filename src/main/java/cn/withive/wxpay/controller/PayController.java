package cn.withive.wxpay.controller;

import cn.withive.wxpay.constant.OrderStatusEnum;
import cn.withive.wxpay.constant.OrderTypeEnum;
import cn.withive.wxpay.entity.Order;
import cn.withive.wxpay.model.ResModel;
import cn.withive.wxpay.sdk.WXPayUtil;
import cn.withive.wxpay.service.OrderService;
import cn.withive.wxpay.service.WXService;
import cn.withive.wxpay.service.WechatUserService;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/pay")
public class PayController extends BaseController {

    @Autowired
    private WXService WXService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private WechatUserService wechatUserService;

    @RequestMapping({"/{orderCode}"})
    public ModelAndView index(@CookieValue(value = "openId", required = false) String openId,
                              @PathVariable(value = "orderCode") String orderCode) {
        ModelAndView payView = new ModelAndView("pay/index");
        ModelAndView homeView = new ModelAndView("redirect:/home");

        if (StringUtils.isEmptyOrWhitespace(openId)) {
            // openId 不存在，重新跳转去验证
            return homeView;
        }

        // 获取用户当前未支付订单
        Order order = orderService.findByWechatOpenIdAndCodeAndStatusIsCreated(openId, orderCode);
        if (order == null) {
            // 当前用户不存在未支付订单
            return homeView;
        }

        String payParams = orderService.getPayParams(orderCode);

        if (StringUtils.isEmpty(payParams)) {
            // 微信统一下单
            Map<String, String> result =
                    WXService.getUnifiedOrderResult(order.getAmount(), order.getCode(), getClientIp(), openId);

            if (result == null || !result.containsKey("prepay_id")) {
//                return fail("微信统一下单失败");
                return homeView;
            }

            String prepayId = result.get("prepay_id");
            // 获取H5 支付参数
            Map<String, String> jsApiParameters = WXService.getJsApiPayParameters(prepayId);
            payParams = JSON.toJSONString(jsApiParameters);

            orderService.setPayParamsCache(orderCode, payParams);
        }

        payView.addObject("payParams", payParams);
        payView.addObject("orderCode", orderCode);

        return payView;
    }

    @PostMapping("/payNotify")
    @ResponseBody
    public String payNotify() throws Exception {
        Map<String, String> returnData = new HashMap<>();

        try {
            InputStream inStream = request.getInputStream();
            ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inStream.read(buffer)) != -1) {
                outSteam.write(buffer, 0, len);
            }
            String resultXml = new String(outSteam.toByteArray(), "utf-8");
            outSteam.close();
            inStream.close();

            Map<String, String> result = WXPayUtil.xmlToMap(resultXml);

            if (!"SUCCESS".equals(result.get("return_code"))) {
                returnData.put("return_code", "FAIL");
                returnData.put("return_msg", "回调通知失败");
                return WXPayUtil.mapToXml(returnData);
            }

            // 查询微信订单
            String outTradeNo = result.get("out_trade_no");
            boolean isValid = orderService.checkPaidWithCode(outTradeNo);
            if (!isValid) {
                returnData.put("return_code", "FAIL");
                returnData.put("return_msg", "订单未完成支付");
                return WXPayUtil.mapToXml(returnData);
            }

            // 查询商户订单
            String openId = result.get("openid");
            Order order = orderService.findByWechatOpenIdAndCode(openId, outTradeNo);
            if (order == null) {
                returnData.put("return_code", "SUCCESS");
                returnData.put("return_msg", "OK");
                return WXPayUtil.mapToXml(returnData);
            }

            // 订单已完成支付
            // 修改订单状态为已支付
            orderService.markToPaid(order);

            returnData.put("return_code", "SUCCESS");
            returnData.put("return_msg", "OK");

            return WXPayUtil.mapToXml(returnData);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage());

            returnData.put("return_code", "FAIL");
            returnData.put("return_msg", ex.getMessage());
            return WXPayUtil.mapToXml(returnData);
        }
    }

}
