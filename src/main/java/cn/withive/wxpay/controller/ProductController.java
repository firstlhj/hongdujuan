package cn.withive.wxpay.controller;

import cn.withive.wxpay.constant.RedirectViewEnum;
import cn.withive.wxpay.entity.Order;
import cn.withive.wxpay.entity.WechatUser;
import cn.withive.wxpay.model.TreesSuccessModel;
import cn.withive.wxpay.service.OrderService;
import cn.withive.wxpay.service.WechatUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.util.StringUtils;

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
    @RequestMapping({"/{orderCode}", ""})
    public ModelAndView index(@CookieValue(value = "openId", required = false) String openId,
                              @PathVariable(value = "orderCode", required = false) String orderCode) {
        ModelAndView productView = new ModelAndView("product/index");
        ModelAndView homeView = new ModelAndView("redirect:/home");

        TreesSuccessModel model = new TreesSuccessModel();

        if (StringUtils.isEmptyOrWhitespace(openId)) {
            // openId 不存在，重新跳转去验证
            homeView.setViewName("redirect:/home?state=" + RedirectViewEnum.product.name());
            return homeView;
        }

        if (StringUtils.isEmptyOrWhitespace(orderCode)) {
            // 没有订单编号，那么检查用户是否曾经付过订单
            boolean isExist = orderService.existsByWechatOpenIdAndPaid(openId);
            if (!isExist) {
                // 当前用户从未付过单
                return homeView;
            }
        } else {
            // 有订单编号
            Order order = orderService.findByWechatOpenIdAndCode(openId, orderCode);
            if (order == null) {
                // 这个订单不是你的
                return homeView;
            }

            boolean isPaid = orderService.checkPaidWithCode(order.getCode());
            if (!isPaid) {
                // 这个订单没付钱
                return homeView;
            }

            orderService.markToPaid(order);
        }

        // 获取用户头像
        WechatUser wechatUser = wechatUserService.findByOpenId(openId);
        model.setRank(orderService.getRank(openId));
        model.setAvatar(wechatUser.getAvatar());
        model.setNickname(wechatUser.getNickname());
        productView.addObject("model", model);

        // 用户已支付订单
//        List<Order> orders = orderService.findByWechatOpenIdAndStatus(openId, OrderStatusEnum.Paid);
//        productView.addObject("orders", orders);

        Long count = wechatUserService.getOrderCount(openId);
        productView.addObject("orderCount", count);

        return productView;
    }
}
