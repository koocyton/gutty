package com.doopp.gutty.test.pojo;

import java.io.Serializable;

public class User implements Serializable {

    private static final long serialVersionUID = 5163L;

    private Long id;

    private String nickName;

    private String account;

    private String password;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccount() {
        return account;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
