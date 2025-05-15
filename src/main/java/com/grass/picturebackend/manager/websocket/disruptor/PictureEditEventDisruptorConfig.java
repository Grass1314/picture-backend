package com.grass.picturebackend.manager.websocket.disruptor;


import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
public class PictureEditEventDisruptorConfig {

    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;

    @Bean(name = "pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModeRingBuffer() {
        int bufferSize = 1024 * 256;
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(PictureEditEvent::new, bufferSize,
                ThreadFactoryBuilder.create().setNamePrefix("pictureEditEventDisruptor").build());
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);
        disruptor.start();
        return disruptor;
    }
}
