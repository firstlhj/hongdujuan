package cn.withive.wxpay.constant;

public enum CacheKeyConstEnum {

    count_tree_key("wxpay:count:tree"),
    count_paid_key("wxpay:count:paid"),
    order_top_key("wxpay:count:top"),
    order_secondtop_key("wxpay:count:secondtop"),
    order_list_key("wxpay:order:%s"),
    product_list_key("wxpay:product"),
    area_list_key("wxpay:area"),
    user_list_key("wxpay:wechatUser"),
    user_rank_key("wxpay:wechatUser:rank"),
    user_tree_key("wxpay:wechatUser:tree"),
    user_order_paid_key("wxpay:wechatUser:order:paid:%s"),
    user_order_created_key("wxpay:wechatUser:order:created:%s"),
    user_order_amount_key("wxpay:wechatUser:order:amount:%s"),
    user_pay_params_key("wxpay:wechatUser:pay_params:%s"),
    token_global_key("wxpay:token:global"),
    token_ticket_key("wxpay:token:ticket"),
    token_user_key("wxpay:token:user:%s"),
    config_storage_key("wxpay:config:storage"),
    bak_order_list_key("wxpay:bak:order:%s"),
    bak_product_list_key("wxpay:bak:product"),
    bak_user_list_key("wxpay:bak:wechatUser");

    private String keyName;

    CacheKeyConstEnum(String keyName) {
        this.keyName = keyName;
    }

    public String getKey() {
        return keyName;
    }

    public String getKey(String... args) {
        return String.format(keyName, args);
    }

}
