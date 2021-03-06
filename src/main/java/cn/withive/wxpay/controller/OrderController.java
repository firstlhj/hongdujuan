package cn.withive.wxpay.controller;

import cn.withive.wxpay.entity.Order;
import cn.withive.wxpay.entity.Product;
import cn.withive.wxpay.entity.WechatUser;
import cn.withive.wxpay.model.ListModel;
import cn.withive.wxpay.model.OrderModel;
import cn.withive.wxpay.model.ResModel;
import cn.withive.wxpay.service.*;
import cn.withive.wxpay.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.util.StringUtils;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/order")
public class OrderController extends BaseController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private WechatUserService wechatUserService;

    @Autowired
    private AreaService areaService;

    @Autowired
    private WXService wxService;

    @GetMapping("/list")
    @ResponseBody
    public ListModel list(@CookieValue(value = "openId", required = false) String openId, Integer page, Integer size) {
        if (StringUtils.isEmptyOrWhitespace(openId)) {
            ListModel resModel = new ListModel();
            resModel.setMsg("缺少必要参数：微信用户id");
            resModel.setCode(ListModel.StatusEnum.FAILURE);
            return resModel;
        }
        if (page == null || size == null) {
            ListModel resModel = new ListModel();
            resModel.setMsg("缺少必要参数：页码和页长");
            resModel.setCode(ListModel.StatusEnum.FAILURE);
            return resModel;
        }
        boolean isExist = wechatUserService.existsByOpenId(openId);
        if (!isExist) {
            ListModel resModel = new ListModel();
            resModel.setMsg("该用户不存在");
            resModel.setCode(ListModel.StatusEnum.FAILURE);
            return resModel;
        }

        // 后台page从0记起
        Page<Order> orders = orderService.findByWechatOpenIdAndPaid(openId, PageRequest.of(page - 1, size));

        if (orders == null || orders.getTotalElements() == 0) {
            ListModel resModel = new ListModel();
            resModel.setMsg("该用户未种植树木");
            resModel.setCode(ListModel.StatusEnum.FAILURE);
            return resModel;
        }

        ListModel model = new ListModel();
        model.setData(orders.getContent());
        model.setCode(ListModel.StatusEnum.SUCCESS);
        model.setTotal(orders.getTotalElements());
        return model;
    }

    @PostMapping("/create")
    @ResponseBody
    public ResModel create(@CookieValue(value = "openId") String openId,
                           @RequestBody OrderModel model) {
        if (StringUtils.isEmptyOrWhitespace(model.getProductCode())) {
            return fail("缺少创建订单必要参数：商品编号");
        }
        if (StringUtils.isEmptyOrWhitespace(openId)) {
            return fail("缺少创建订单必要参数：微信用户id");
        }
        if (model.getType() == null) {
            return fail("缺少创建订单必要参数：订单类型");
        }
        if (model.getQuantity() == null) {
            return fail("缺少创建订单必要参数：商品数量");
        }
        if (model.getQuantity() > 200) {
            return fail("创建订单失败：单次商品数量不能超过200");
        }
        if (StringUtils.isEmptyOrWhitespace(model.getName())) {
            return fail("缺少创建订单必要参数：姓名");
        }
        if (model.getName().length() > 100) {
            return fail("创建订单失败：姓名长度太长");
        }
        if (!StringUtils.isEmptyOrWhitespace(model.getPhone())) {
            if (model.getPhone().length() > 20) {
                return fail("创建订单失败：手机格式不正确");
            }
        }
        if (StringUtils.isEmptyOrWhitespace(model.getAreaCode())) {
            return fail("缺少创建订单必要参数：区域编号");
        }
        Product product = productService.findByCode(model.getProductCode());
        if (product == null) {
            return fail("创建订单失败：所选商品不存在");
        }
        WechatUser user = wechatUserService.findByOpenId(openId);
        if (user == null) {
            return fail("创建订单失败：系统中不存在此用户");
        }
        boolean existArea = areaService.exist(model.getAreaCode());
        if (!existArea) {
            return fail("创建订单失败：不存在的区域编号");
        }

        // 对同一个openId进行加锁
        synchronized (openId) {
            boolean check = orderService.checkUserCreateCount(openId);
            if (check) {
                // 一时段内，创建订单总数大于预定阈值
                return fail("创建订单失败：频率过快");
            }

            // 创建订单
            try {
                // 保存订单
                BigDecimal quantity = new BigDecimal(model.getQuantity());
                Order entity = new Order();
                entity.setCode(RandomUtil.generateUniqueStr());
                entity.setWechatOpenId(openId);
                entity.setProductId(product.getId());
                entity.setProductName(product.getName());
                entity.setType(model.getType());
                entity.setQuantity(model.getQuantity());
                entity.setName(model.getName());
                entity.setPhone(model.getPhone());
                entity.setAmount(product.getAmount().multiply(quantity));
                entity.setRemark(model.getRemark());
                entity.setAreaCode(model.getAreaCode());
                orderService.create(entity);

                // 保存用户个人信息
                user.setPhone(model.getPhone());
                user.setRealName(model.getName());
                wechatUserService.save(user);

                // 自增用户创建订单数
                orderService.setUserCreateOrderCode(openId, entity.getCode());

                return success("订单创建成功", entity.getCode());
            } catch (Exception ex) {
                ex.printStackTrace();
                log.error(ex.getMessage());
                return fail(ex.getMessage());
            }
        }
    }
}
