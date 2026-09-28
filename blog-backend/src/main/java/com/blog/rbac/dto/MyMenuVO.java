package com.blog.rbac.dto;

import lombok.Data;

import java.util.List;

/**
 * 当前用户菜单+权限码返回VO
 * blog-admin登录后第一个调用的接口返回数据
 */
@Data
public class MyMenuVO {

    /**
     * 菜单树
     */
    private List<MenuVO> menus;

    /**
     * 接口权限码列表
     */
    private List<String> perms;
}
