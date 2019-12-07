package cn.withive.wxpay.controller;

import cn.withive.wxpay.constant.OrderStatusEnum;
import cn.withive.wxpay.entity.Order;
import cn.withive.wxpay.entity.Product;
import cn.withive.wxpay.model.ListModel;
import cn.withive.wxpay.model.OrderModel;
import cn.withive.wxpay.model.ResModel;
import cn.withive.wxpay.service.OrderService;
import cn.withive.wxpay.service.ProductService;
import cn.withive.wxpay.service.WechatUserService;
import cn.withive.wxpay.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.util.StringUtils;

import java.util.List;

@Controller
@RequestMapping("/order")
public class OrderController extends BaseController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private WechatUserService wechatUserService;

    @PostMapping("/list")
    @ResponseBody
    public ListModel list(@CookieValue(value = "openId", required = false) String openId) {
        if (StringUtils.isEmptyOrWhitespace(openId)) {
            ListModel resModel = new ListModel();
            resModel.setMsg("创建订单缺少必要参数：微信用户id");
            resModel.setCode(ListModel.StatusEnum.FAILURE);
            return resModel;
        }

        boolean exists = wechatUserService.existsByOpenId(openId);
        if (!exists) {
            ListModel resModel = new ListModel();
            resModel.setMsg("不存在此用户");
            resModel.setCode(ListModel.StatusEnum.FAILURE);
            return resModel;
        }

        List<Order> orders = orderService.findByWechatOpenIdAndStatus(openId, OrderStatusEnum.Paid);

        if (orders == null) {
            ListModel resModel = new ListModel();
            resModel.setMsg("当前用户未曾下单");
            resModel.setCode(ListModel.StatusEnum.FAILURE);
            return resModel;
        }

        ListModel model = new ListModel();
        model.setData(orders);
        model.setCode(ListModel.StatusEnum.SUCCESS);
        model.setTotal(orders.size());
        return model;
    }

    @PostMapping("/create")
    @ResponseBody
    public ResModel create(@CookieValue(value = "openId", required = false) String openId,
                           @RequestBody OrderModel model) {
        if (StringUtils.isEmptyOrWhitespace(model.getProductCode())) {
            return fail("创建订单缺少必要参数：商品编号");
        }

        if (StringUtils.isEmptyOrWhitespace(openId)) {
            return fail("创建订单缺少必要参数：微信用户id");
        }

        // 对同一个openId进行加锁
        synchronized(openId) {
            // 判断用户是否存在未支付订单
            Order order = orderService.findByWechatOpenIdWithCreated(openId);

            if (order != null) {
                // 有订单，判断是否超时
                boolean overtime = orderService.checkOvertime(order);

                if (!overtime) {
                    // 未超时订单
                    return success("重新发起订单");
                }
            }

            // 创建订单
            try {
                Product product = productService.findByCode(model.getProductCode());

                if (product == null) {
                    return fail("所选商品不存在");
                }

                Order entity = new Order();

                entity.setCode(RandomUtil.generateUniqueStr());
                entity.setWechatOpenId(openId);
                entity.setProductId(product.getId());
                entity.setProductName(product.getName());
                entity.setAmount(product.getAmount());
                entity.setRemark(model.getRemark());

                orderService.create(entity);

                return success("订单创建成功");
            } catch (Exception ex) {
                ex.printStackTrace();
                log.error(ex.getMessage());
                return fail(ex.getMessage());
            }
        }
    }

}
