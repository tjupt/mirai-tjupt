package me.tongyifan.util;

import com.google.gson.Gson;
import me.tongyifan.entity.BaseResponse;
import me.tongyifan.entity.BindUserResponse;
import me.tongyifan.entity.JoinGroupEventResponseAction;
import okhttp3.*;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @author tongyifan
 */
public class Request {
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Config config;

    public Request(Config config) {
        this.config = config;
    }

    public BindUserResponse bindUser(String username, String passkey, long qq) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "link_by_uid");
        params.put("platform", "qq");
        params.put("platform_id", Long.toString(qq));
        params.put("username", username);
        params.put("passkey", passkey);

        final Map<String, String> finalParams = prepareRequest(params);

        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(config.getBaseUrl() + "api_social_media.php")).newBuilder();

        okhttp3.Request request = new okhttp3.Request.Builder().url(httpBuilder.build()).post(RequestBody.create(new Gson().toJson(finalParams), MediaType.parse("application/json; charset=utf-8"))).build();

        BaseResponse responseObject;
        try {
            Response response = okHttpClient.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            responseObject = new Gson().fromJson(responseBody, BaseResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new BindUserResponse(JoinGroupEventResponseAction.REJECT, "网络错误，请稍后重试", null);
        }

        if (responseObject.getStatus() == 0) {
            return new BindUserResponse(JoinGroupEventResponseAction.ACCEPT, null, null);
        } else if (responseObject.getStatus() == 1) {
            // 绑定成功，但存在警告
            return new BindUserResponse(JoinGroupEventResponseAction.ACCEPT, null, "用户的加群请求可能存在违规行为，建议进行检查\n" + String.join("\n", castList(responseObject.getData(), String.class)));
        } else if (responseObject.getStatus() == -1) {
            return new BindUserResponse(JoinGroupEventResponseAction.REJECT, "未查询到信息，请检查用户名是否正确", null);
        } else {
            return new BindUserResponse(JoinGroupEventResponseAction.IGNORE, null, "用户的加群请求触发了警告规则，需要手动处理\n" + responseObject.getMsg());
        }
    }

    public String getLoginSecret(long qq) {
        String role = "member";
        if (config.getLoginAdministrator().contains(qq)) {
            role = "admin";
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("platform", "qq");
        params.put("platform_id", Long.toString(qq));
        params.put("role", role);

        final Map<String, String> finalParams = prepareRequest(params);

        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(config.getBaseUrl() + "api_letmelogin.php")).newBuilder();

        okhttp3.Request request = new okhttp3.Request.Builder().url(httpBuilder.build()).post(RequestBody.create(new Gson().toJson(finalParams), MediaType.parse("application/json; charset=utf-8"))).build();

        BaseResponse responseObject;
        try {
            Response response = okHttpClient.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            responseObject = new Gson().fromJson(responseBody, BaseResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return "获取登录链接时出现错误，请重试或联系管理员";
        }

        if (responseObject.getStatus() == 0) {
            return "https://tjupt.org/letmelogin.php?secret=" + responseObject.getData().toString() + "\n有效期5分钟，尝试次数1次，请尽快登录。此链接仅可本人使用，其他账户登录无效。\n若曾经登录失败，请重启浏览器后再打开链接以清除登录记录。对于Chrome内核浏览器，你可以在浏览器地址栏输入「chrome://restart」来重启浏览器。";
        } else if (responseObject.getStatus() == 1) {
            return "当前站点可直接登录，无需获取登录链接";
        } else if (responseObject.getStatus() == -1) {
            return "此QQ尚未关联北洋园PT账号，请联系管理员";
        } else if (responseObject.getStatus() == -2) {
            return "30分钟内仅可获取一次临时链接";
        } else {
            return "获取登录链接时出现错误，请重试或联系管理员";
        }
    }

    private Map<String, String> prepareRequest(final Map<String, String> params) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(config.getToken());
        params.keySet().forEach(key -> stringBuilder.append(params.get(key)));
        stringBuilder.append(config.getSecret());

        byte[] calcParams = stringBuilder.toString().getBytes(StandardCharsets.UTF_8);

        String sign = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            sign = String.format("%032x", new BigInteger(1, md.digest(calcParams)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        params.put("token", config.getToken());
        params.put("sign", sign);

        return params;
    }

    public static <T> List<T> castList(Object obj, Class<T> clazz) {
        List<T> result = new ArrayList<T>();
        if (obj instanceof List<?>) {
            for (Object o : (List<?>) obj) {
                result.add(clazz.cast(o));
            }
            return result;
        }
        return null;
    }
}
