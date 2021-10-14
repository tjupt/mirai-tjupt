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
            TEMP_GROUP = Long.parseLong(prop.getProperty("qq.temporary_group"));

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
                    if (event.getGroup().getId() == TEMP_GROUP) {
                        String msgString = Main.toString(event.getMessage());
                        msgString = msgString.replace(" ", "");
                        if (msgString.contains("不到邮件") || msgString.contains("没有邮件")) || msgString.contains("验证邮件")) {
                            final QuoteReply quote = new QuoteReply(event.getSource());
                            event.getGroup().sendMessage(quote.plus("天大edu邮箱，请在邮箱网页中点击“自助查询-收信查询”来查看，如果没有记录，请再通过前缀为学号的邮箱地址申请试试。\n\n
                            西电edu邮箱，请在“西电邮箱网关”取回邮件：http://smg.stumail.xidian.edu.cn/gw/user/ \n\n
                            上交大edu邮箱，大概率收不到邮件，请在群内给出你的个人非edu邮箱，管理员手动发放。\n\n
                            其他高校edu邮箱，请查看邮箱的垃圾箱以及邮箱网关拦截记录。如果没有，请把postmaster@tjupt.org加入收信白名单之后重新获取一次邮件。\n\n
                            如果经过上述操作仍找不到邮件，请在群里给出你的edu邮箱。"));
                        } else if (msgString.contains("申述") || msgString.contains("驳回")) || msgString.contains("未通过")) || msgString.contains("没通过")) {
                            final QuoteReply quote = new QuoteReply(event.getSource());
                            event.getGroup().sendMessage(quote.plus("申请被驳回的原因是什么？"));
                        } else if (msgString.contains("补充") || msgString.contains("修正")) || msgString.contains("纸条")) {
                            final QuoteReply quote = new QuoteReply(event.getSource());
                            event.getGroup().sendMessage(quote.plus("1、请使用写有「北洋园PT注册专用+你申请的用户名」的纸条遮盖证件的隐私信息（照片姓名等）。\n
                            2、请露出证件的学校名称和证件有效期。若无有效期，请露出学号并注明学号格式和入学时间。\n
                            3、拍下照片发送在群内，管理员看到后会重新审核。如重新审核通过，管理员会@通知你，然后就可以直接登录了。"));
                        } else if (msgString.contains("如何注册") || msgString.contains("怎么注册")) || msgString.contains("想注册")) {
                            final QuoteReply quote = new QuoteReply(event.getSource());
                            event.getGroup().sendMessage(quote.plus("可以通过以下方式注册北洋园PT：\n
                            1、(推荐) 如果你是天津大学或其他高校的在校师生，可以通过自助注册系统https://tjupt.org/signup/self_invite 获得一个邀请码。\n
                            2、如果你是天大在校生，可以选择转发一段话到你的班级群帮北洋园PT宣传一下，发送2分钟后截图反馈给我们，然后就能立刻给你发邀请。（转发宣传的文案请在群内回复“转发宣传”）\n
                            3、如果你是天大毕业校友，可在每年校庆时进行申请注册。\n
                            4、找你已经注册本站的朋友，让他发邀请给你。北洋园PT站内用户可以通过升级或者使用魔力值购买得到邀请码。\n
                            5、在其他PT站点的北洋园PT官方邀请楼中回帖申请。"));
                        } else if (msgString.contains("转发宣传") {
                            final QuoteReply quote = new QuoteReply(event.getSource());
                            event.getGroup().sendMessage(quote.plus("北洋园PT（tjupt.org）是由天大在校师生和校友共同建立的一个资源共享平台，有高清影视、学习资料、科研软件、大型游戏、动漫音乐等各种丰富的资源，无论在天大校内校外均可访问和高速下载。访问tjupt.org即可进行自助注册，如有问题欢迎加QQ群637597613咨询~"));
                        } else if (msgString.contains("找回") || msgString.contains("忘记")) {
                            final QuoteReply quote = new QuoteReply(event.getSource());
                            event.getGroup().sendMessage(quote.plus("访问https://tjupt.org/recover.php 可找回用户名或密码。
                            1、通过自助邀请注册的用户，请回想是否在注册时更换了邮箱，请使用注册时的邮箱进行找回密码。\n
                            2、如果不记得是哪个邮箱，请在你所有邮箱中搜索含有关键词“指定此邮箱”的邮件，并通过该邮件的收件邮箱找回密码。\n
                            3、如通过前两步无法找回，请在群内提供你的注册用户名、注册邮箱以及注册时间。\n
                            【找回密码的邮件中包含有你的用户名和密码，使用新的密码登录时，请注意不要复制上密码后的空格】"));
                        } else if (msgString.contains("没有反应") || msgString.contains("无反应")) || msgString.contains("点击登录")) {
                            final QuoteReply quote = new QuoteReply(event.getSource());
                            event.getGroup().sendMessage(quote.plus("微软即将停止支持IE浏览器，很多新Web网页技术无法在IE浏览器上正常工作，请使用Chrome、Firefox或者新版Edge浏览器。\n
                            如是360、搜狗等浏览器，请切换浏览器模式至极速模式再次尝试。"));
                        }
                    } else {
                        if (!event.getSenderName().startsWith("TJUPT-") && event.getSender().getPermission() == MemberPermission.MEMBER && !EXCLUDED_USERS.contains(event.getSender().getId())) {
                            event.getSender().mute(30);
                            event.getGroup().sendMessage(new At(event.getSender().getId()).plus("为方便管理，请将群名片改为 TJUPT-你的站内用户名"));
                        }
    
                        String msgString = Main.toString(event.getMessage());
                        msgString = msgString.replace(" ", "");
                        if (msgString.contains("谢") || msgString.contains("蟹蟹") || msgString.contains("鞋鞋")
                                || msgString.contains("謝") || msgString.contains("身寸") || msgString.contains("三克")
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