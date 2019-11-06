package com.meng.api.service;

import com.meng.api.core.APIMapping;
import com.meng.api.model.UserInfo;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl {

    @APIMapping(value = "api.user.getUser")
    public UserInfo getUserInfo(Long userId){
        UserInfo userInfo = new UserInfo();
        userInfo.setName("mmm");
        userInfo.setUserId(userId);
        return userInfo;
    }

}
