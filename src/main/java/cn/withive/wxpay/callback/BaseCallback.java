package cn.withive.wxpay.callback;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseCallback {
    public void failure(String rawData) {
        log.error(rawData);
    }
}
