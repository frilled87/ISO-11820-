package com.iso11820.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 操作员实体 —— 对应数据库 {@code operators} 表。
 * <p>
 * 存储系统登录账号信息，密码为明文存储。
 * 此表无主键约束，登录校验按 {@code username + pwd} 进行。
 * </p>
 *
 * <h3>字段说明</h3>
 * <table>
 *   <tr><th>字段</th><th>类型</th><th>说明</th></tr>
 *   <tr><td>userid</td><td>String</td><td>用户ID（1=管理员, 2=试验员）</td></tr>
 *   <tr><td>username</td><td>String</td><td>登录用户名（admin / experimenter）</td></tr>
 *   <tr><td>pwd</td><td>String</td><td>明文密码</td></tr>
 *   <tr><td>usertype</td><td>String</td><td>角色：admin 或 operator</td></tr>
 * </table>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class Operators implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID（1=管理员, 2=试验员） */
    private String userid;

    /** 登录用户名（admin / experimenter） */
    private String username;

    /** 明文密码 */
    private String pwd;

    /** 角色：admin 或 operator */
    private String usertype;

    // ==================== 构造方法 ====================

    /** 无参构造 */
    public Operators() {
    }

    /**
     * 全参构造。
     *
     * @param userid   用户ID
     * @param username 登录用户名
     * @param pwd      明文密码
     * @param usertype 角色
     */
    public Operators(String userid, String username, String pwd, String usertype) {
        this.userid = userid;
        this.username = username;
        this.pwd = pwd;
        this.usertype = usertype;
    }

    // ==================== Getter / Setter ====================

    public String getUserid() { return userid; }

    public void setUserid(String userid) { this.userid = userid; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getPwd() { return pwd; }

    public void setPwd(String pwd) { this.pwd = pwd; }

    public String getUsertype() { return usertype; }

    public void setUsertype(String usertype) { this.usertype = usertype; }

    // ==================== Object 重写 ====================

    @Override
    public String toString() {
        return new StringJoiner(", ", "Operators[", "]")
                .add("userid='" + userid + "'")
                .add("username='" + username + "'")
                .add("pwd='" + pwd + "'")
                .add("usertype='" + usertype + "'")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Operators that)) return false;
        return Objects.equals(userid, that.userid)
            && Objects.equals(username, that.username)
            && Objects.equals(pwd, that.pwd)
            && Objects.equals(usertype, that.usertype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userid, username, pwd, usertype);
    }
}