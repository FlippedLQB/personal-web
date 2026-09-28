package com.blog.rbac.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单节点VO（树形结构）
 */
@Data
public class MenuVO {

    /**
     * 权限/菜单ID
     */
    private Long id;

    /**
     * 权限编码
     */
    private String permCode;

    /**
     * 菜单名称
     */
    private String permName;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 子菜单
     */
    private List<MenuVO> children = new ArrayList<>();
}
