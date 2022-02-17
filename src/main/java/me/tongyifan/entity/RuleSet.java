package me.tongyifan.entity;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import java.io.*;
import java.util.*;

import static java.net.HttpURLConnection.HTTP_OK;

/**
 * 自动回复规则集
 *
 * @author tongyifan
 */
public class RuleSet {
    private List<Rule> rules;
    private Map<String, List<Rule>> ruleCache;

    public RuleSet() {
    }

    public RuleSet(List<Rule> ruleSet, Map<String, List<Rule>> ruleCache) {
        this.rules = ruleSet;
        this.ruleCache = ruleCache;
    }



    public void loadRuleSet() throws IOException {
        try {
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader("autoreply.json"));
            RuleSet result = gson.fromJson(reader, RuleSet.class);
            rules = result.rules;
            ruleCache = new LinkedHashMap<>();
        } catch (FileNotFoundException e) {
            pullRuleSetFromRemote();
            loadRuleSet();
        }
    }

    public List<Rule> getTargetRules(String target) {
        if (ruleCache.containsKey(target)) {
            return ruleCache.get(target);
        }
        List<Rule> result = new ArrayList<>();
        for (Rule rule : rules) {
            if (target.equals(rule.getTarget())) {
                result.add(rule);
            }
        }
        ruleCache.put(target, result);

        return result;
    }

    public void pullRuleSetFromRemote() throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder().url("https://ghproxy.com/https://raw.githubusercontent.com/tjupt/mirai-tjupt/master/autoreply.json").get().build();
        Response response = okHttpClient.newCall(request).execute();
        if (response.code() != HTTP_OK) {
            throw new IOException("HTTP状态码不为200");
        }
        String responseBody = Objects.requireNonNull(response.body()).string();
        BufferedWriter out = new BufferedWriter(new FileWriter("autoreply.json"));
        out.write(responseBody);
        out.close();

        loadRuleSet();
    }
}
