package com.example.demo.service;

import io.agora.media.RtcTokenBuilder2;
import io.agora.media.RtcTokenBuilder2.Role;
import java.lang.String;

public class Token {

    private String appId;
    private String appToken;
    private int tokenExpire;
    private int privilegeExpire;

    public Token(String appId, String appToken, int tokenExpire, int privilegeExpire) {
        this.appId = appId;
        this.appToken = appToken;
        this.tokenExpire = tokenExpire;
        this.privilegeExpire = privilegeExpire;
    }

    public String buildToken(String channelName, Long userId) {
        RtcTokenBuilder2 token = new RtcTokenBuilder2();
        String result = token.buildTokenWithUid(appId, appToken, channelName, userId.intValue(), Role.ROLE_SUBSCRIBER,
                tokenExpire, privilegeExpire);
        return result;
    }

    public String buildToken(String channelName, String userId) {
        RtcTokenBuilder2 token = new RtcTokenBuilder2();
        String result = token.buildTokenWithUserAccount(appId, appToken, channelName, userId, Role.ROLE_SUBSCRIBER,
                tokenExpire, privilegeExpire);
        return result;
    }
}
