package com.ruoyi.xiaozhi.chat;

import com.ruoyi.xiaozhi.feign.DeviceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DeviceClientTests {

    @Autowired
    private DeviceClient deviceClient;

    @Test
    public void test() {
        deviceClient.onlineStatus(1L);
    }

}
