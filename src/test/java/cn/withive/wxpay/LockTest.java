package cn.withive.wxpay;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class LockTest {

    @Test
    void test1() {
        String a = "123456";
        String b = "1234567";

        Thread thread1 = new Thread(new Lock(a), "A");
        Thread thread2 = new Thread(new Lock(b), "B");

        thread1.start();
        thread2.start();
    }


    class Lock implements Runnable {
        Object obj;
        Lock(Object obj) {
            this.obj = obj;
        }

        @Override
        public void run() {
            synchronized (obj) {
                System.out.println(obj.hashCode());
                for (int i = 0; i < 5; i++) {
                    System.out.println(Thread.currentThread().getName() + ":" + i);
                }
            }
        }
    }

}
