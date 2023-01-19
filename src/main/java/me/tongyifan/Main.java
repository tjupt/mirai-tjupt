package me.tongyifan;

import kotlin.coroutines.CoroutineContext;
import me.tongyifan.entity.Rule;
import me.tongyifan.entity.RuleSet;
import me.tongyifan.util.Config;
import me.tongyifan.util.Request;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.contact.ContactList;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.QuoteReply;
import net.mamoe.mirai.utils.BotConfiguration;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Main {
    private static final RuleSet ruleSet = new RuleSet();
    private static Config config;

    private Main() {
    }

    public static void main(String[] args) {
        try {
            config = new Config();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            ruleSet.loadRuleSet();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("从远端获取自动回复规则集失败，本地无可用规则，程序退出");
            return;
        }

        Request request = new Request(config);

        final Bot bot = BotFactory.INSTANCE.newBot(config.getAccount(), config.getPassword(), new BotConfiguration() {
            {
                fileBasedDeviceInfo("deviceInfo.json");
                setProtocol(MiraiProtocol.IPAD);
            }
        });

        bot.login();

        bot.getEventChannel().registerListenerHost(new SimpleListenerHost() {
            @EventHandler
            public ListeningStatus onGroupMessage(final GroupMessageEvent event) {
                final String msgString = Main.toString(event.getMessage()).replace(" ", "");
                final QuoteReply quote = new QuoteReply(event.getSource());
                final At at = new At(event.getSender().getId());

                if (event.getGroup().getId() == config.getUserGroup0Id() ||
                        event.getGroup().getId() == config.getUserGroup1Id()) {
                    if (!event.getSenderName().startsWith("TJUPT-") &&
                            event.getSender().getPermission() == MemberPermission.MEMBER &&
                            !config.getExcludedUserIds().contains(event.getSender().getId())) {
                        event.getSender().mute(30);
                        event.getGroup().sendMessage(at.plus("为方便管理，请将群名片改为 TJUPT-你的站内用户名"));
                    }
                }

                String target;
                if (event.getGroup().getId() == config.getTempGroupId()) {
                    target = "temp";
                } else if (event.getGroup().getId() == config.getAdminGroupId()) {
                    target = "admin";
                } else {
                    target = "user";
                }

                List<Rule> targetRuleset = ruleSet.getTargetRules(target);
                for (Rule rule : targetRuleset) {
                    if (rule.getKeywords().stream().anyMatch(
                            keyword -> msgString.toLowerCase().contains(keyword.toLowerCase()))
                    ) {
                        if ("at".equals(rule.getReplyType())) {
                            event.getGroup().sendMessage(at.plus(rule.getReplyText()));
                            return ListeningStatus.LISTENING;
                        } else {
                            event.getGroup().sendMessage(quote.plus(rule.getReplyText()));
                            return ListeningStatus.LISTENING;
                        }
                    }
                }

                return ListeningStatus.LISTENING;
            }

            @EventHandler
            public ListeningStatus onJoinRequestEvent(MemberJoinRequestEvent event) {
                if (event.getGroupId() != config.getUserGroup0Id() && event.getGroupId() != config.getUserGroup1Id()) {
                    event.ignore(false);
                    return ListeningStatus.LISTENING;
                }

                // 判断重复加群
                if (event.getGroupId() == config.getUserGroup0Id()) {
                    // 加新人群，判断新手群中是否有此用户
                    ContactList<NormalMember> members = Objects.requireNonNull(event.getBot().getGroup(config.getUserGroup1Id())).getMembers();
                    if (members.contains(event.getFromId())) {
                        event.reject(false, "请勿重复加群");
                        return ListeningStatus.LISTENING;
                    }
                } else if (event.getGroupId() == config.getUserGroup1Id()) {
                    // 加新手群，判断新人群中是否有此用户
                    ContactList<NormalMember> members = Objects.requireNonNull(event.getBot().getGroup(config.getUserGroup0Id())).getMembers();
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

                ImmutablePair<Boolean, List<String>> response = request.bindUser(username, passkey, event.getFromId());
                if (response.getLeft()) {
                    event.accept();

                    int retry = 5;
                    while (retry > 0) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        NormalMember member = Objects.requireNonNull(event.getGroup()).getMembers().get(event.getFromId());
                        if (member != null) {
                            member.setNameCard("TJUPT-" + username);
                            event.getGroup().sendMessage(new At(member.getId()).plus("欢迎新人，请先阅读北洋园PT新手手册：https://t.cn/AiDJIKfY ，爆照送魔力值，欢迎爆照~"));
                            break;
                        }

                        retry -= 1;
                    }
                } else {
                    event.ignore(false);
                }

                if (response.getRight() != null) {
                    Objects.requireNonNull(bot.getGroup(config.getAdminGroupId())).sendMessage(String.join("\n", response.getRight()));
                }

                return ListeningStatus.LISTENING;
            }

            @EventHandler
            public ListeningStatus onTempMessage(GroupTempMessageEvent event) {
                String msgString = Main.toString(event.getMessage());

                List<Rule> targetRuleset = ruleSet.getTargetRules("private");
                for (Rule rule : targetRuleset) {
                    if (rule.getKeywords().stream().anyMatch(msgString::contains)) {
                        event.getSender().sendMessage(rule.getReplyText());
                        return ListeningStatus.LISTENING;
                    }
                }
                event.getSender().sendMessage("QAQ");
                return ListeningStatus.LISTENING;
            }

            @EventHandler
            public ListeningStatus onFriendMessage(FriendMessageEvent event) {
                String msgString = Main.toString(event.getMessage());

                if (msgString.contains("更新规则集")) {
                    try {
                        ruleSet.pullRuleSetFromRemote();
                        event.getSender().sendMessage("更新规则集成功");
                        return ListeningStatus.LISTENING;
                    } catch (IOException e) {
                        e.printStackTrace();
                        event.getSender().sendMessage("更新规则集失败：" + e);
                        return ListeningStatus.LISTENING;
                    }
                }

                List<Rule> targetRuleset = ruleSet.getTargetRules("private");
                for (Rule rule : targetRuleset) {
                    if (rule.getKeywords().stream().anyMatch(msgString::contains)) {
                        event.getSender().sendMessage(rule.getReplyText());
                        return ListeningStatus.LISTENING;
                    }
                }
                event.getSender().sendMessage("QAQ");
                return ListeningStatus.LISTENING;
            }

            @EventHandler
            public ListeningStatus onMemberCardChangeEvent(MemberCardChangeEvent event) {
                if (event.getGroupId() == config.getUserGroup0Id() || event.getGroupId() == config.getUserGroup1Id()) {
                    if (event.getMember().getPermission() == MemberPermission.MEMBER) {
                        if (!event.getOrigin().startsWith("TJUPT-") && event.getNew().startsWith("TJUPT-")) {
                            String username = event.getNew().split("-")[1];
                            request.bindUser(username, "", event.getMember().getId());
                        } else if (!event.getNew().startsWith("TJUPT-")) {
                            event.getGroup().sendMessage(new At(event.getMember().getId()).plus("为方便管理，请将群名片改为 TJUPT-你的站内用户名"));
                        }
                    }
                }

                return ListeningStatus.LISTENING;
            }

            @EventHandler
            public ListeningStatus onNudgedEvent(NudgeEvent event) {
                if (event.getTarget().getId() == config.getAccount()) {
                    event.getFrom().nudge().sendTo(event.getSubject());
                }

                return ListeningStatus.LISTENING;
            }

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
}
