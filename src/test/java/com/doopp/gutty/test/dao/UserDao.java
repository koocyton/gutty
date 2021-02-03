package com.doopp.gutty.test.dao;

import com.doopp.gutty.test.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserDao {

    @Select({
            "<script>",
            "SELECT * FROM `auth_user`",
            "</script>"
    })
    List<User> selectAll();

    @Select("SELECT * FROM `auth_user` WHERE `id`=#{id}")
    User selectById(@Param("id") Long id);
}
