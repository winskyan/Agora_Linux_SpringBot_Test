package com.example.demo.service;

import io.agora.rtc.AgoraRtcConn;
import io.agora.rtc.DefaultRtcConnObserver;
import io.agora.rtc.RtcConnInfo;

public class ConnObserver extends DefaultRtcConnObserver {
    @Override
    public void onConnected(AgoraRtcConn conn, RtcConnInfo rtcConnInfo, int reason) {
        SampleLogger.log("join success");
    }

    @Override
    public void onUserLeft(AgoraRtcConn agora_rtc_conn, String user_id, int reason) {
        SampleLogger.log("onUserLeft");
    }
}