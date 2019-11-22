package cn.withive.wxpay.constant;

public class CacheKeyConst {

    /**
     * 已支付订单总数 String结构
     */
    public static final String order_count_key = "wxpay:order:count";

    /**
     * 用户排名 hash结构
     */
    public static final String order_rank_key = "wxpay:order:rank";

    /**
     * 订单列表 hash结构
     */
    public static final String order_list_key = "wxpay:order";

    /**
     * 订单最新排名 list结构
     */
    public static final String order_top_key = "wxpay:order:top";

    /**
     * 商品列表 hash结构
     */
    public static final String product_list_key = "wxpay:product";

    /**
     * 用户列表 hash结构
     */
    public static final String user_list_key = "wxpay:wechatUser";

    /**
     * 已支付用户集合 set结构
     */
    public static final String user_paid_set_key = "wxpay:wechatUser:paid";

    /**
     * 微信全局token String结构，数据两小时自动过期
     */
    public static final String wx_global_token_key = "wxpay:global_token";

    /**
     * 微信临时票据 String结构，数据两小时自动过期
     */
    public static final String wx_ticket_key = "wxpay:ticket";

    /**
     * 微信用户token，用于获取用户信息 String结构，数据两小时自动过期
     */
    public static final String wx_user_token_key = "wxpay:user_token:";

    /**
     * 微信统一下单后的预支付标识 String结构，数据两小时自动过期
     */
    public static final String wx_prepay_key = "wxpay:prepay:";

    /**
     * 数据存储策略 String结构
     * 详见 StorageStrategyEnum
     */
    public static final String storage_strategy_key = "wxpay:storageStrategy";

    /*备份数据key*/
    public static final String bak_order_list_key = "wxpay:bak:order";
    public static final String bak_product_list_key = "wxpay:bak:product";
    public static final String bak_user_list_key = "wxpay:bak:wechatUser";
}
