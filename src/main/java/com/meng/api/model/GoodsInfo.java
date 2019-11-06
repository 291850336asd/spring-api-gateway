package com.meng.api.model;

public class GoodsInfo {

    private String goodsName;
    private String goodsId;

    public GoodsInfo() {
    }


    public GoodsInfo(String goodsName, String goodsId) {
        this.goodsName = goodsName;
        this.goodsId = goodsId;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(String goodsId) {
        this.goodsId = goodsId;
    }
}
