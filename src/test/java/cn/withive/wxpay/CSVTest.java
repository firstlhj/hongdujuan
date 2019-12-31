package cn.withive.wxpay;

import cn.withive.wxpay.config.WXPayMchConfig;
import cn.withive.wxpay.constant.BillTypeEnum;
import cn.withive.wxpay.constant.CacheKeyConstEnum;
import cn.withive.wxpay.constant.OrderStatusEnum;
import cn.withive.wxpay.constant.OrderTypeEnum;
import cn.withive.wxpay.entity.*;
import cn.withive.wxpay.repository.*;
import cn.withive.wxpay.service.OrderService;
import cn.withive.wxpay.service.ProductService;
import cn.withive.wxpay.service.WXService;
import cn.withive.wxpay.service.WechatUserService;
import cn.withive.wxpay.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.csvreader.CsvReader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
public class CSVTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderBakRepository orderBakRepository;

    @Autowired
    private WechatUserService wechatUserService;

    @Autowired
    private WechatUserRepository wechatUserRepository;

    @Autowired
    private WechatUserBakRepository wechatUserBakRepository;

    @Autowired
    private WXService wxService;

    @Autowired
    private WXPayMchConfig mchConfig;

    private String folder = "C:\\Users\\qxb51\\Documents\\wxpay.bill";

    @Test
    void test0() {
        LocalDate startDate = LocalDate.parse("2019-12-31");
        String content = wxService.downloadBill(startDate, BillTypeEnum.ALL);
        System.out.println(content);
    }

    @Test
    void test2() throws IOException {
        File file = new File("C:\\Users\\qxb51\\Documents\\wxpay.bill\\20191207.csv");

        if (!file.exists()) {
            log.info("文件不存在");
            return;
        }

        CsvReader reader = new CsvReader(new FileInputStream(file), StandardCharsets.UTF_8);

        reader.readHeaders();

        int i = 0;
        while (reader.readRecord()) {
            if ("总交易单数".equals(reader.get(0))) {
                break;
            }
            i++;
//            System.out.println(reader.getRawRecord());
            System.out.println(reader.get(0));
//            System.out.println(reader.get("商户订单号"));
        }
        System.out.println(i);
    }

    @Test
    void downloadBill() {
        LocalDate startDate = LocalDate.parse("2019-12-07");
        LocalDate endDate = LocalDate.now();

        while (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {
            wxService.downloadBill(startDate, folder, BillTypeEnum.SUCCESS);
            wxService.downloadBill(startDate, folder, BillTypeEnum.REFUND);

            startDate = startDate.plusDays(1);
        }
    }

    @Test
    void syncBillToDB() throws IOException {
        String successFolder = folder + "\\" + BillTypeEnum.SUCCESS;
        String refundFolder = folder + "\\" + BillTypeEnum.REFUND;

        File folder = new File(successFolder);
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        Product product = productService.findByCode("001");
        BigDecimal price = new BigDecimal(100);

        List<Order> orders = new LinkedList<>();
        List<WechatUser> users = new LinkedList<>();

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            CsvReader reader = new CsvReader(new FileInputStream(file), StandardCharsets.UTF_8);
            reader.readHeaders();

            while (reader.readRecord()) {
                String payTimeStr = reader.get(0);
                if ("总交易单数".equals(payTimeStr)) {
                    break;
                }

                // 获取表格列数据
                BigDecimal amount = new BigDecimal(reader.get(18));
                if (amount.compareTo(price) < 0) {
                    // 存在部分一分钱订单，过滤
                    continue;
                }
                String machId = reader.get(2);
                if (!mchConfig.getMchID().equals(machId)) {
                    // 过滤非本商户订单
                    continue;
                }

                String code = reader.get(6);
                String openId = reader.get(7);

                int quantity = amount.divide(price, RoundingMode.HALF_UP).intValue();
                LocalDateTime payTime = LocalDateTime.parse(payTimeStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                LocalDateTime now = LocalDateTime.now();

                // 订单数据
                Order order = new Order();
                order.setId(RandomUtil.generateUniqueStr());
                order.setCreatTime(now);
                order.setAmount(amount);
                order.setCode(code);
                order.setName("");
                order.setPayTime(payTime);
                order.setPhone("");
                order.setProductId(product.getId());
                order.setProductName(product.getName());
                order.setQuantity(quantity);
                order.setRemark("");
                order.setStatus(OrderStatusEnum.Paid);
                order.setType(OrderTypeEnum.myself);
                order.setWechatOpenId(openId);
                orders.add(order);

                // 微信用户数据
                WechatUser wechatUser = new WechatUser();
                wechatUser.setId(RandomUtil.generateUniqueStr());
                wechatUser.setCreatTime(now);
                wechatUser.setAvatar("");
                wechatUser.setCity("");
                wechatUser.setCountry("");
                wechatUser.setNickname("");
                wechatUser.setOpenId(openId);
                wechatUser.setProvince("");
                users.add(wechatUser);
            }

            reader.close();
        }


        folder = new File(refundFolder);
        File[] refunds = folder.listFiles();
        if (refunds != null) {
            List<String> refundCodes = new LinkedList<>();

            for (File refund : refunds) {
                if (!refund.isFile()) {
                    continue;
                }

                CsvReader reader = new CsvReader(new FileInputStream(refund), StandardCharsets.UTF_8);
                reader.readHeaders();

                while (reader.readRecord()) {
                    String payTimeStr = reader.get(0);

                    if ("总交易单数".equals(payTimeStr)) {
                        break;
                    }

                    String code = reader.get(6);
                    refundCodes.add(code);
                }
            }

            orders = orders.stream().filter(x -> !refundCodes.contains(x.getCode())).collect(Collectors.toList());
        }

        // 批量保存
        orderRepository.deleteAllInBatch();
        orderRepository.saveAll(orders);
        wechatUserRepository.deleteAllInBatch();
        wechatUserRepository.saveAll(users);
    }

    /**
     * 同步备份数据到数据库
     */
    @Test
    void syncBakDB() {
        List<Order> orders = orderRepository.findAll();
        List<OrderBak> orderBaks = orderBakRepository.findAll();
        for (Order order : orders) {
            Optional<OrderBak> bak = orderBaks.stream().filter(x -> x.getCode().equals(order.getCode())).findFirst();

            bak.ifPresent(x -> {
                order.setName(x.getName());
                order.setPhone(x.getPhone());
                order.setType(x.getType());
            });
        }
        orderRepository.saveAll(orders);


        List<WechatUser> users = wechatUserRepository.findAll();
        List<WechatUserBak> userBaks = wechatUserBakRepository.findAll();
        for (WechatUser user : users) {
            Optional<WechatUserBak> bak = userBaks.stream().filter(x -> x.getOpenId().equals(user.getOpenId())).findFirst();

            bak.ifPresent(x -> {
                user.setAvatar(x.getAvatar());
                user.setCity(x.getCity());
                user.setCountry(x.getCountry());
                user.setNickname(x.getNickname());
                user.setProvince(x.getProvince());
            });
        }
        wechatUserRepository.saveAll(users);
    }

    @Test
    void syncDBtoCache() {
        List<Order> orders =
                orderRepository.
                        findAll(Sort.sort(Order.class).by((Function<Order, LocalDateTime>) order -> order.getPayTime())
                                .ascending());

        for (Order order : orders) {
            orderService.markToPaid(order);
        }

        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();

        List<WechatUser> users = wechatUserRepository.findAll();
        for (WechatUser user : users) {
            hashOperations.put(CacheKeyConstEnum.user_list_key.getKey(), user.getOpenId(),
                    JSON.toJSONString(user));
        }

        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            hashOperations.put(CacheKeyConstEnum.product_list_key.getKey(), product.getCode(),
                    JSON.toJSONString(product));
        }
    }
}
