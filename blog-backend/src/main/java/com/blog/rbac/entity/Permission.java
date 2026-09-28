package com.blog.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("permission")
public class Permission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String permCode;
    private String permName;
    /**
     * 1=接口权限 2=菜单权限
     */
    private Integer type;
    private Long parentId;
    private String path;
    private String icon;
    private Integer sort;
    private LocalDateTime createTime;
}
