package com.doopp.gutty.demo.dao;

import com.doopp.gutty.demo.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserDao {

    @Select({
            "<script>",
            "SELECT * FROM `user`",
            "</script>"
    })
    List<User> selectAll();

    @Select("SELECT * FROM `user` WHERE `id`=#{id}")
    User selectById(@Param("id") Long id);
}
