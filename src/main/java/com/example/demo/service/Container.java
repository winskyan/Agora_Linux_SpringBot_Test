package com.example.demo.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.agora.rtc.AgoraRtcConn;
import lombok.NoArgsConstructor;

@Component
@NoArgsConstructor
public class Container {
    public ConcurrentHashMap<String, AgoraRtcConn> conns = new ConcurrentHashMap<>();
}
