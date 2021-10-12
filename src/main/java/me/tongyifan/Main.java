package me.tongyifan;

import com.google.gson.Gson;
import kotlin.coroutines.CoroutineContext;
import me.tongyifan.entity.BaseResponse;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.contact.ContactList;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.MemberCardChangeEvent;
import net.mamoe.mirai.event.events.MemberJoinRequestEvent;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.GroupTempMessageEvent;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.BotConfiguration;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class Main {
    static long QQ;
    static String QQ_PASSWORD;

    static String TJUPT_BASE_URL;
    static String TOKEN;
    static String SECRET;

    static long ADMIN_GROUP;
    static long USER_GROUP_0;
    static long USER_GROUP_1;

    // Q群助手等一系列官方bot
    static List<Long> EXCLUDED_USERS;

    static OkHttpClient okHttpClient = new OkHttpClient();

    public static void main(String[] args) throws InterruptedException {
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties();

            if (input == null) {
                System.out.println("Missing config.properties");
                return;
            }
            prop.load(input);

            QQ = Long.parseLong(prop.getProperty("qq.account_id"));
            QQ_PASSWORD = prop.getProperty("qq.account_password");

            TJUPT_BASE_URL = prop.getProperty("tjupt.base_url");
            TOKEN = prop.getProperty("tjupt.api_token");
            SECRET = prop.getProperty("tjupt.api_secret");
            ADMIN_GROUP = Long.parseLong(prop.getProperty("qq.admin_group"));
            USER_GROUP_0 = Long.parseLong(prop.getProperty("qq.user_group_0"));
            USER_GROUP_1 = Long.parseLong(prop.getProperty("qq.user_group_1"));

            EXCLUDED_USERS = Arrays.stream(prop.getProperty("qq.excluded_users").split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        final Bot bot = BotFactory.INSTANCE.newBot(QQ, QQ_PASSWORD, new BotConfiguration() {
            {
                fileBasedDeviceInfo("deviceInfo.json");
            }
        });

        bot.login();

        bot.getEventChannel().registerListenerHost(new SimpleListenerHost() {
            @EventHandler
            public ListeningStatus onGroupMessage(GroupMessageEvent event) {
                if (event.getGroup().getId() != ADMIN_GROUP) {
                    if (!event.getSenderName().startsWith("TJUPT-") && event.getSender().getPermission() == MemberPermission.MEMBER && !EXCLUDED_USERS.contains(event.getSender().getId())) {
                        event.getSender().mute(30);
                        event.getGroup().sendMessage(new At(event.getSender().getId()).plus("为方便管理，请将群名片改为 TJUPT-你的站内用户名"));
                    }

                    String msgString = Main.toString(event.getMessage());
                    msgString = msgString.replace(" ", "");
                    if (msgString.contains("谢") || msgString.contains("蟹蟹") || msgString.contains("鞋鞋")
                            || msgString.contains("射") || msgString.contains("身寸") || msgString.contains("三克")
                            || msgString.contains("thank") || msgString.contains("3q") || msgString.contains("3Q")
                            || msgString.contains("xie") || msgString.contains("thx") || msgString.contains("xx")) {
                        event.getGroup().sendMessage(new At(event.getSender().getId()).plus("不客气，爆个照吧"));
                    } else if (msgString.contains("红种") || msgString.contains("未工作")) {
                        final QuoteReply quote = new QuoteReply(event.getSource());
                        event.getGroup().sendMessage(quote.plus("Tracker未正常工作导致“红种”无法正常下载的解决方法：https://t.cn/A6IWzpoF"));
                    } else if (msgString.contains("备用地址")) {
                        final QuoteReply quote = new QuoteReply(event.getSource());
                        event.getGroup().sendMessage(quote.plus("本站备用地址：https://tju.pt ，仅供临时使用"));
                    } else if (msgString.contains("hosts")) {
                        final QuoteReply quote = new QuoteReply(event.getSource());
                        event.getGroup().sendMessage(quote.plus("hosts修改教程：https://t.cn/A6IWzWtJ"));
                    } else if (msgString.contains("IE")) {
                        // event.getGroup().sendMessage(MessageUtils.newImage("").plus("请按照图片所示重置IE设置"));
                    } else if (msgString.contains("离校模式")) {
                        // event.getGroup().sendMessage(MessageUtils.newImage("").plus("请前往网站控制面板的个人设置页打开「离校模式」，并在BT客户端中将ipfilter设置为false"));
                    }
                }

                return ListeningStatus.LISTENING;
            }

            @EventHandler
            public ListeningStatus onJoinRequestEvent(MemberJoinRequestEvent event) {
                // 判断重复加群
                if (event.getGroupId() == USER_GROUP_0) {
                    // 加新人群，判断新手群中是否有此用户
                    ContactList<NormalMember> members = event.getBot().getGroup(USER_GROUP_1).getMembers();
                    if (members.contains(event.getFromId())) {
                        event.reject(false, "请勿重复加群");
                        return ListeningStatus.LISTENING;
                    }
                } else if (event.getGroupId() == USER_GROUP_1) {
                    // 加新手群，判断新人群中是否有此用户
                    ContactList<NormalMember> members = event.getBot().getGroup(USER_GROUP_0).getMembers();
                    if (members.contains(event.getFromId())) {
                        event.reject(false, "请勿重复加群");
                        return ListeningStatus.LISTENING;
                    }
                }
                String username, passkey = "";

                if (!event.getMessage().contains("答案：")) {
                    event.reject(false, "请通过网站内控制面板「社交媒体帐号」申请加群，帐号问题加群637597613");
                }
                String answer = event.getMessage().split("答案：")[1];

                String pattern = "(.*)[:：]([0-9a-f]{5})$";
                Pattern r = Pattern.compile(pattern);

                Matcher m = r.matcher(answer);
                if (m.find()) {
                    username = m.group(0);
                    passkey = m.group(1);
                } else {
                    username = answer;
                }

                boolean isSuccess = bindUser(username, passkey, event.getFromId(), event.getBot());
                if (isSuccess) {
                    event.accept();

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    NormalMember member = Objects.requireNonNull(event.getGroup()).getMembers().get(event.getFromId());

                    // 设置群名片
                    assert member != null;
                    member.setNameCard("TJUPT-" + username);
                    event.getGroup().sendMessage(new At(member.getId()).plus(
                            "欢迎新人，请先阅读北洋园PT新手手册：https://t.cn/AiDJIKfY ，爆照送魔力值，欢迎爆照~"));
                } else {
                    event.ignore(false);
                }

                return ListeningStatus.LISTENING;
            }

            @EventHandler
            public ListeningStatus onTempMessage(GroupTempMessageEvent event) {
                String msgString = Main.toString(event.getMessage());

                if (msgString.contains("登录") || msgString.contains("登陆")) {
                    event.getSender().sendMessage(
                            "目前网站可以正常访问，如果您依旧无法访问，请使用备用地址 https://tju.pt 或按此教程修改hosts https://share.mubu.com/doc/3xgFtqmnEym");
                } else {
                    event.getSender().sendMessage("QAQ");
                }
                return ListeningStatus.LISTENING;
            }

            @EventHandler
            public ListeningStatus onFriendMessage(FriendMessageEvent event) {
                String msgString = Main.toString(event.getMessage());

                if (msgString.contains("登录") || msgString.contains("登陆")) {
                    event.getSender().sendMessage("目前网站可以正常访问，如果您依旧无法访问，请使用备用地址 https://tju.pt");
                } else {
                    event.getSender().sendMessage("QAQ");
                }
                return ListeningStatus.LISTENING;
            }

            @EventHandler
            public ListeningStatus onMemberCardChangeEvent(MemberCardChangeEvent event) {
                if (event.getGroup().getId() != ADMIN_GROUP) {
                    if (event.getMember().getPermission() == MemberPermission.MEMBER) {
                        if (!event.getOrigin().startsWith("TJUPT-") && event.getNew().startsWith("TJUPT-")) {
                            String username = event.getNew().split("-")[1];
                            bindUser(username, "", event.getMember().getId(), event.getBot());
                        } else if (!event.getNew().startsWith("TJUPT-")) {
                            event.getGroup().sendMessage(new At(event.getMember().getId()).plus("为方便管理，请将群名片改为 TJUPT-你的站内用户名"));
                        }
                    }
                }

                return ListeningStatus.LISTENING;
            }

            // 处理在处理事件中发生的未捕获异常
            @Override
            public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception) {
                throw new RuntimeException("在事件处理中发生异常", exception);
            }
        });

        bot.join();
    }

    private static String toString(MessageChain chain) {
        return chain.contentToString();
    }

    private static boolean bindUser(String username, String passkey, long qq, Bot bot) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "link_by_uid");
        params.put("platform", "qq");
        params.put("platform_id", Long.toString(qq));
        params.put("username", username);
        params.put("passkey", passkey);

        final Map<String, String> finalParams = prepareRequest(params);

        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(TJUPT_BASE_URL + "api_social_media.php"))
                .newBuilder();

        Request request = new Request.Builder().url(httpBuilder.build()).post(
                        RequestBody.create(new Gson().toJson(finalParams), MediaType.parse("application/json; charset=utf-8")))
                .build();

        BaseResponse responseObject;
        try {
            Response response = okHttpClient.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            responseBody = responseBody.replace(":false,", ":0,").replace(":true,", ":1,");
            responseObject = new Gson().fromJson(responseBody, BaseResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (responseObject.getStatus() == 0) {
            return true;
        } else if (responseObject.getStatus() == 1) {
            // 绑定成功，但存在警告
            bot.getGroup(ADMIN_GROUP).sendMessage(String.join("\n", (List<String>) responseObject.getData()));
            return true;
        } else if (responseObject.getStatus() == -1) {
            return false;
        } else {
            bot.getGroup(ADMIN_GROUP).sendMessage(responseObject.getMsg());
            return false;
        }
    }

    private static Map<String, String> prepareRequest(Map<String, String> params) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(TOKEN);
        params.keySet().forEach(key -> stringBuilder.append(params.get(key)));
        stringBuilder.append(SECRET);

        byte[] calcParams = stringBuilder.toString().getBytes(StandardCharsets.UTF_8);

        String sign = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            sign = String.format("%032x", new BigInteger(1, md.digest(calcParams)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        params.put("token", TOKEN);
        params.put("sign", sign);

        return params;
    }
}