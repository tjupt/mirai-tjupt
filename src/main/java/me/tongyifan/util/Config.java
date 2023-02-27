package me.tongyifan.util;

import net.mamoe.mirai.utils.BotConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author tongyifan
 */
public class Config {
    private long account;
    private String password;
    private BotConfiguration.MiraiProtocol protocol;

    private String baseUrl;
    private String token;
    private String secret;
    private List<Long> loginAdministrator;

    private long adminGroupId;
    private long userGroup0Id;
    private long userGroup1Id;
    private long tempGroupId;

    /**
     * Q群助手等一系列官方bot
     */
    private List<Long> excludedUserIds;

    public Config() throws IOException {
        loadConfig();
    }

    public long getAccount() {
        return account;
    }

    public void setAccount(long account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public BotConfiguration.MiraiProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = BotConfiguration.MiraiProtocol.valueOf(protocol);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<Long> getLoginAdministrator() {
        return loginAdministrator;
    }

    public void setLoginAdministrator(@NotNull String loginAdministrator) {
        this.loginAdministrator = Arrays.stream(loginAdministrator.split(",")).map(Long::parseLong).collect(Collectors.toList());

    }

    public long getAdminGroupId() {
        return adminGroupId;
    }

    public void setAdminGroupId(long adminGroupId) {
        this.adminGroupId = adminGroupId;
    }

    public long getUserGroup0Id() {
        return userGroup0Id;
    }

    public void setUserGroup0Id(long userGroup0Id) {
        this.userGroup0Id = userGroup0Id;
    }

    public long getUserGroup1Id() {
        return userGroup1Id;
    }

    public void setUserGroup1Id(long userGroup1Id) {
        this.userGroup1Id = userGroup1Id;
    }

    public long getTempGroupId() {
        return tempGroupId;
    }

    public void setTempGroupId(long tempGroupId) {
        this.tempGroupId = tempGroupId;
    }

    public List<Long> getExcludedUserIds() {
        return excludedUserIds;
    }

    public void setExcludedUserIds(@NotNull String excludedUserIds) {
        this.excludedUserIds = Arrays.stream(excludedUserIds.split(",")).map(Long::parseLong).collect(Collectors.toList());
    }

    private void loadConfig() throws IOException {
        InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties");
        if (input == null) {
            throw new FileNotFoundException("Missing config.properties");
        }

        Properties prop = new Properties();
        prop.load(input);

        setAccount(Long.parseLong(prop.getProperty("qq.account_id")));
        setPassword(prop.getProperty("qq.account_password"));
        setProtocol(prop.getProperty("qq.protocol"));

        setBaseUrl(prop.getProperty("tjupt.base_url"));
        setToken(prop.getProperty("tjupt.api_token"));
        setSecret(prop.getProperty("tjupt.api_secret"));
        setAdminGroupId(Long.parseLong(prop.getProperty("qq.admin_group")));
        setUserGroup0Id(Long.parseLong(prop.getProperty("qq.user_group_0")));
        setUserGroup1Id(Long.parseLong(prop.getProperty("qq.user_group_1")));
        setTempGroupId(Long.parseLong(prop.getProperty("qq.temporary_group")));

        setExcludedUserIds(prop.getProperty("qq.excluded_users"));
        setLoginAdministrator(prop.getProperty("qq.login_administrator"));
    }
}
