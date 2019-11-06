package com.meng.api.service;

import com.meng.api.core.APIMapping;
import com.meng.api.model.GoodsInfo;
import org.springframework.stereotype.Service;

@Service
public class GoodsServiceImpl {


    @APIMapping("api.goods.get")
    public GoodsInfo getGoodsInfo(String id){
        return new GoodsInfo("sdjskdjsk",id);
    }

}
