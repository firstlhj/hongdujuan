package cn.withive.wxpay.controller;

import cn.withive.wxpay.constant.OrderStatusEnum;
import cn.withive.wxpay.constant.RedirectViewEnum;
import cn.withive.wxpay.entity.Order;
import cn.withive.wxpay.entity.WechatUser;
import cn.withive.wxpay.model.TreesSuccessModel;
import cn.withive.wxpay.service.OrderService;
import cn.withive.wxpay.service.WechatUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.util.StringUtils;

import java.util.List;

/**
 * @author qiu xiaobing
 * @date 2019/11/17 16:35
 */
@Controller
@RequestMapping("/product")
public class ProductController extends BaseController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private WechatUserService wechatUserService;

    /**
     * 用户支付成功后的产品展示页
     *
     * @param openId
     * @return
     */
    @RequestMapping({"", "/index"})
    public ModelAndView index(@CookieValue(value = "openId", required = false) String openId) {

        ModelAndView productView = new ModelAndView("product/index");
        ModelAndView homeView = new ModelAndView("redirect:/home");

        TreesSuccessModel model = new TreesSuccessModel();

        if (StringUtils.isEmptyOrWhitespace(openId)) {
            // openId 不存在，重新跳转去验证
            homeView.setViewName("redirect:/home?state=" + RedirectViewEnum.product.name());
            return homeView;
        }

        boolean exists = wechatUserService.existsByOpenId(openId);
        if (!exists) {
            // 不存在此用户
            return homeView;
        }

        Order order = orderService.findByWechatOpenIdWithCreated(openId);
        if (order == null) {
            // 不存在状态为未支付的订单
            boolean isExist = orderService.existsByWechatOpenIdAndStatus(openId, OrderStatusEnum.Paid);

            if (isExist) {
                // 当前用户曾经支付过订单
                Long rank = orderService.getRank(openId);
                WechatUser wechatUser = wechatUserService.findByOpenId(openId);
                model.setRank(rank);
                model.setAvatar(wechatUser.getAvatar());
                model.setNickname(wechatUser.getNickname());
                productView.addObject("model", model);
                return productView;
            } else {
                // 当前用户没有下过单
                return homeView;
            }
        }

        // 存在状态为未支付的订单
        boolean isPaid = orderService.checkPaidWithCode(order.getCode());
        if (!isPaid) {
            // 未付款
            orderService.checkOvertime(order);

            boolean isExist = orderService.existsByWechatOpenIdAndStatus(openId, OrderStatusEnum.Paid);

            if (isExist) {
                // 当前用户曾经支付过订单
                Long rank = orderService.getRank(openId);
                WechatUser wechatUser = wechatUserService.findByOpenId(openId);
                model.setRank(rank);
                model.setAvatar(wechatUser.getAvatar());
                model.setNickname(wechatUser.getNickname());
                productView.addObject("model", model);

                List<Order> orders = orderService.findByWechatOpenIdAndStatus(openId, OrderStatusEnum.Paid);
                productView.addObject("orders", orders);
                return productView;
            } else {
                // 当前用户没有下过单
                return homeView;
            }
        }

        // 订单已完成支付
        // 修改订单状态为已支付
        boolean result = orderService.markToPaid(order);

        // 获取用户头像
        WechatUser wechatUser = wechatUserService.findByOpenId(openId);
        model.setRank(orderService.getRank(openId));
        model.setAvatar(wechatUser.getAvatar());
        model.setNickname(wechatUser.getNickname());
        productView.addObject("model", model);

        List<Order> orders = orderService.findByWechatOpenIdAndStatus(openId, OrderStatusEnum.Paid);
        productView.addObject("orders", orders);

        return productView;
    }
}
