package com.example;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * GitHub Contribution Checker
 * 
 * GitHubのContribution（草）をチェックするクラス。
 * GitHub GraphQL APIを使用して、指定されたユーザーのContribution状況を取得します。
 * 
 * <h3>重要な背景</h3>
 * <ul>
 *   <li>GitHubのGraphQL APIでは、リポジトリ作成（totalRepositoryContributions）が
 *       作成日とその翌日両方でカウントされることがある</li>
 *   <li>しかし、GitHubカレンダー上では作成日のみ草が生えるのが正しい挙動</li>
 *   <li>このAPIの仕様による誤差を吸収するため、repositoryContributions.occurredAtで
 *       厳密な日付判定を行う</li>
 * </ul>
 * 
 * <h3>Contributionの種類</h3>
 * <ul>
 *   <li>コミット（commitContributionsByRepository）</li>
 *   <li>Issue作成・コメント（issueContributionsByRepository）</li>
 *   <li>プルリクエスト作成・レビュー（pullRequestContributionsByRepository）</li>
 *   <li>リポジトリ作成（repositoryContributions）</li>
 * </ul>
 * 
 * @author GitHub Grass Checker Team
 * @since 1.0
 */
public class GitHubContributionChecker {
    private static final Logger logger = LoggerFactory.getLogger(GitHubContributionChecker.class);
    private static final String GITHUB_GRAPHQL_URL = "https://api.github.com/graphql";
    
    /**
     * GitHub GraphQL APIのクエリ
     * 
     * 指定された期間のContribution情報を取得します。
     * リポジトリ作成の場合はoccurredAtフィールドも取得し、
     * 厳密な日付判定を行うために使用します。
     */
    private static final String CONTRIBUTION_QUERY = 
            "query($username: String!, $from: DateTime!, $to: DateTime!) {\n" +
            "  user(login: $username) {\n" +
            "    contributionsCollection(from: $from, to: $to) {\n" +
            "      totalCommitContributions\n" +
            "      totalIssueContributions\n" +
            "      totalPullRequestContributions\n" +
            "      totalPullRequestReviewContributions\n" +
            "      totalRepositoryContributions\n" +
            "      commitContributionsByRepository(maxRepositories: 10) {\n" +
            "        repository {\n" +
            "          name\n" +
            "        }\n" +
            "        contributions {\n" +
            "          totalCount\n" +
            "        }\n" +
            "      }\n" +
            "      issueContributionsByRepository(maxRepositories: 10) {\n" +
            "        repository {\n" +
            "          name\n" +
            "        }\n" +
            "        contributions {\n" +
            "          totalCount\n" +
            "        }\n" +
            "      }\n" +
            "      pullRequestContributionsByRepository(maxRepositories: 10) {\n" +
            "        repository {\n" +
            "          name\n" +
            "        }\n" +
            "        contributions {\n" +
            "          totalCount\n" +
            "        }\n" +
            "      }\n" +
            "      repositoryContributions(first: 10) {\n" +
            "        nodes {\n" +
            "          occurredAt\n" +
            "          repository {\n" +
            "            name\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
    
    // 1年分の草データを取得するGraphQLクエリ
    private static final String CALENDAR_QUERY =
            "query($username: String!) {\n" +
            "  user(login: $username) {\n" +
            "    contributionsCollection {\n" +
            "      contributionCalendar {\n" +
            "        weeks {\n" +
            "          contributionDays {\n" +
            "            date\n" +
            "            contributionCount\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String githubToken;
    
    /**
     * GitHubContributionCheckerを初期化します
     * 
     * @param githubToken GitHub Personal Access Token
     */
    public GitHubContributionChecker(String githubToken) {
        this.githubToken = githubToken;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 指定されたユーザーの今日のContributionがあるかどうかをチェックします
     * 
     * @param username GitHubのユーザー名
     * @return 今日Contributionがある場合はtrue、ない場合はfalse
     */
    public boolean hasTodayContribution(String username) {
        try {
            ZonedDateTime today = LocalDate.now().atStartOfDay(ZoneOffset.UTC);
            ZonedDateTime tomorrow = today.plusDays(1);

            String fromDate = today.format(DateTimeFormatter.ISO_INSTANT);
            String toDate = tomorrow.format(DateTimeFormatter.ISO_INSTANT);
            
            String query = buildGraphQLQuery(username, fromDate, toDate);
            JsonNode response = executeGraphQLQuery(query);
            
            return parseContributionResponse(response);
            
        } catch (GitHubTokenException e) {
            if (logger.isErrorEnabled()) {
                logger.error("GitHubトークンエラー: {}", e.getMessage());
            }
            throw new RuntimeException("GitHubトークンが無効です", e);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Contributionチェック中にエラーが発生しました: {}", e.getMessage(), e);
            }
            return false;
        }
    }
    
    /**
     * 指定されたユーザーのContribution情報を取得します（継続日数含む）
     * 
     * @param username GitHubのユーザー名
     * @return Contribution情報（Contributionの有無、件数、継続日数）
     */
    public ContributionInfo getContributionInfo(String username) {
        try {
            ZonedDateTime today = LocalDate.now().atStartOfDay(ZoneOffset.UTC);
            ZonedDateTime tomorrow = today.plusDays(1);

            String fromDate = today.format(DateTimeFormatter.ISO_INSTANT);
            String toDate = tomorrow.format(DateTimeFormatter.ISO_INSTANT);
            
            String query = buildGraphQLQuery(username, fromDate, toDate);
            JsonNode response = executeGraphQLQuery(query);
            
            return parseContributionInfo(response, username);
            
        } catch (GitHubTokenException e) {
            if (logger.isErrorEnabled()) {
                logger.error("GitHubトークンエラー: {}", e.getMessage());
            }
            throw new RuntimeException("GitHubトークンが無効です", e);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Contribution情報取得中にエラーが発生しました: {}", e.getMessage(), e);
            }
            return new ContributionInfo(false, 0, 0);
        }
    }
    
    /**
     * 1年分の草データを取得し、継続日数を計算する
     * @param username GitHubユーザー名
     * @return 継続日数
     */
    public int getStreakDays(String username) {
        try {
            String query = String.format("{\"query\": \"%s\", \"variables\": {\"username\": \"%s\"}}",
                    CALENDAR_QUERY.replace("\n", "\\n").replace("\"", "\\\""), username);
            JsonNode response = executeGraphQLQuery(query);
            JsonNode days = response
                .path("data").path("user")
                .path("contributionsCollection")
                .path("contributionCalendar")
                .path("weeks");
            if (days.isMissingNode() || !days.isArray()) return 0;
            // contributionDaysを新しい順に平坦化
            java.util.List<JsonNode> allDays = new java.util.ArrayList<>();
            for (JsonNode week : days) {
                for (JsonNode day : week.path("contributionDays")) {
                    allDays.add(day);
                }
            }
            // 日付降順にソート
            allDays.sort((a, b) -> b.path("date").asText().compareTo(a.path("date").asText()));
            
            // 今日の日付を取得
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            int streak = 0;
            boolean foundToday = false;
            
            for (JsonNode day : allDays) {
                String date = day.path("date").asText();
                int contributionCount = day.path("contributionCount").asInt();
                
                // 今日の日付を見つけたかチェック
                if (date.equals(today)) {
                    foundToday = true;
                    // 今日Contributionがある場合はカウント
                    if (contributionCount > 0) {
                        streak++;
                    }
                } else if (foundToday) {
                    // 今日の日付を過ぎた後（昨日以前）は、Contributionがある日をカウント
                    if (contributionCount > 0) {
                        streak++;
                    } else {
                        // Contributionがない日が見つかったら終了
                        break;
                    }
                }
            }
            
            // 今日Contributionがない場合は、昨日までの継続日数を返す
            if (foundToday) {
                // 今日の日付を見つけた場合、今日のContributionは既にカウント済み
                // または今日Contributionがない場合は0のまま
            } else {
                // 今日の日付が見つからない場合（データが古い場合）は、
                // 最新の日付から連続日数を計算
                for (JsonNode day : allDays) {
                    int contributionCount = day.path("contributionCount").asInt();
                    if (contributionCount > 0) {
                        streak++;
                    } else {
                        break;
                    }
                }
            }
            
            if (logger.isInfoEnabled()) {
                logger.info("継続日数(カレンダーAPI): {}日 (今日の日付: {})", streak, today);
                // デバッグ用: 最新の5日間のContribution状況をログ出力
                if (logger.isDebugEnabled()) {
                    logger.debug("最新5日間のContribution状況:");
                    int count = 0;
                    for (JsonNode day : allDays) {
                        if (count >= 5) break;
                        String date = day.path("date").asText();
                        int contributionCount = day.path("contributionCount").asInt();
                        logger.debug("  {}: {}件", date, contributionCount);
                        count++;
                    }
                }
            }
            return streak;
        } catch (GitHubTokenException e) {
            if (logger.isErrorEnabled()) {
                logger.error("GitHubトークンエラー: {}", e.getMessage());
            }
            throw new RuntimeException("GitHubトークンが無効です", e);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("継続日数計算中にエラーが発生しました: {}", e.getMessage(), e);
            }
            return 0;
        }
    }
    
    /**
     * GraphQLレスポンスからContributionの有無を判定します（より正確な判定）
     * 
     * <p>リポジトリ作成の場合は、occurredAtフィールドを使用して厳密な日付判定を行います。
     * これにより、GitHub APIの仕様による誤カウント（リポジトリ作成が複数日にカウントされる）を防ぎます。</p>
     * 
     * @param response GraphQL APIのレスポンス
     * @return Contributionがある場合はtrue、ない場合はfalse
     */
    private boolean parseContributionResponse(JsonNode response) {
        JsonNode data = response.get("data");
        if (data == null || data.isNull()) {
            JsonNode errors = response.get("errors");
            if (errors != null && errors.isArray() && errors.size() > 0) {
                if (logger.isErrorEnabled()) {
                    logger.error("GraphQLエラー: {}", errors.toString());
                }
            }
            return false;
        }
        JsonNode user = data.get("user");
        if (user == null || user.isNull()) {
            if (logger.isWarnEnabled()) {
                logger.warn("ユーザーが見つかりません");
            }
            return false;
        }
        JsonNode contributions = user.get("contributionsCollection");
        if (contributions == null || contributions.isNull()) {
            return false;
        }
        
        // 日付範囲を取得（GraphQLクエリのvariablesから取得、なければ今日のUTC範囲を使用）
        String fromDate = null;
        String toDate = null;
        try {
            JsonNode variables = response.get("variables");
            if (variables != null) {
                fromDate = variables.get("from").asText();
                toDate = variables.get("to").asText();
            }
        } catch (Exception e) {
            // GraphQLクエリのvariablesが存在しない場合は無視（fallback処理で対応）
        }
        // fallback: 今日のUTC範囲
        if (fromDate == null || toDate == null) {
            ZonedDateTime today = LocalDate.now().atStartOfDay(ZoneOffset.UTC);
            ZonedDateTime tomorrow = today.plusDays(1);
            fromDate = today.format(DateTimeFormatter.ISO_INSTANT);
            toDate = tomorrow.format(DateTimeFormatter.ISO_INSTANT);
        }
        
        // 実際のアクティビティがあるかチェック（リポジトリ作成はoccurredAtで厳密判定）
        boolean hasActualActivity = hasActualContributions(contributions, fromDate, toDate);
        
        // 基本的なContribution数を計算（デバッグ用）
        int totalContributions = 
                getIntValue(contributions, "totalCommitContributions") +
                getIntValue(contributions, "totalIssueContributions") +
                getIntValue(contributions, "totalPullRequestContributions") +
                getIntValue(contributions, "totalPullRequestReviewContributions") +
                getIntValue(contributions, "totalRepositoryContributions");
        
        if (logger.isInfoEnabled()) {
            logger.info("今日のContribution数: {} (実際のアクティビティ: {})", totalContributions, hasActualActivity);
        }
        
        return hasActualActivity;
    }
    
    /**
     * 実際のアクティビティがあるかチェックします
     * 
     * <p>以下のContributionをチェックします：</p>
     * <ul>
     *   <li>コミット（commitContributionsByRepository）</li>
     *   <li>Issue作成・コメント（issueContributionsByRepository）</li>
     *   <li>プルリクエスト作成・レビュー（pullRequestContributionsByRepository）</li>
     *   <li>リポジトリ作成（repositoryContributions）- occurredAtで厳密な日付判定</li>
     * </ul>
     * 
     * <p>リポジトリ作成の場合は、occurredAtが指定された日付範囲内にある場合のみ
     * アクティビティとしてカウントします。これにより、GitHub APIの仕様による
     * 誤カウント（リポジトリ作成が複数日にカウントされる）を防ぎます。</p>
     * 
     * @param contributions contributionsCollectionノード
     * @param fromDate 開始日時（ISO8601形式）
     * @param toDate 終了日時（ISO8601形式）
     * @return 実際のアクティビティがある場合はtrue、ない場合はfalse
     */
    private boolean hasActualContributions(JsonNode contributions, String fromDate, String toDate) {
        // コミットがあるかチェック
        JsonNode commitContributions = contributions.get("commitContributionsByRepository");
        if (commitContributions != null && commitContributions.isArray()) {
            for (JsonNode repo : commitContributions) {
                JsonNode contributionsNode = repo.get("contributions");
                if (contributionsNode != null) {
                    int count = getIntValue(contributionsNode, "totalCount");
                    if (count > 0) {
                        return true;
                    }
                }
            }
        }
        
        // Issueがあるかチェック
        JsonNode issueContributions = contributions.get("issueContributionsByRepository");
        if (issueContributions != null && issueContributions.isArray()) {
            for (JsonNode repo : issueContributions) {
                JsonNode contributionsNode = repo.get("contributions");
                if (contributionsNode != null) {
                    int count = getIntValue(contributionsNode, "totalCount");
                    if (count > 0) {
                        return true;
                    }
                }
            }
        }
        
        // プルリクがあるかチェック
        JsonNode prContributions = contributions.get("pullRequestContributionsByRepository");
        if (prContributions != null && prContributions.isArray()) {
            for (JsonNode repo : prContributions) {
                JsonNode contributionsNode = repo.get("contributions");
                if (contributionsNode != null) {
                    int count = getIntValue(contributionsNode, "totalCount");
                    if (count > 0) {
                        return true;
                    }
                }
            }
        }
        
        // リポジトリ作成: occurredAtがfromDate～toDateの範囲内ならtrue
        // これにより、GitHub APIの仕様による誤カウント（リポジトリ作成が複数日にカウントされる）を防ぐ
        JsonNode repoCreations = contributions.get("repositoryContributions");
        if (repoCreations != null && repoCreations.has("nodes")) {
            for (JsonNode node : repoCreations.get("nodes")) {
                String occurredAt = node.get("occurredAt").asText();
                if (isWithinRange(occurredAt, fromDate, toDate)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * ISO8601文字列の範囲判定を行います
     * 
     * @param occurredAt 発生日時（ISO8601形式）
     * @param fromDate 開始日時（ISO8601形式）
     * @param toDate 終了日時（ISO8601形式）
     * @return occurredAtがfromDate以上かつtoDate未満の場合はtrue、そうでなければfalse
     */
    private boolean isWithinRange(String occurredAt, String fromDate, String toDate) {
        ZonedDateTime occurred = ZonedDateTime.parse(occurredAt);
        ZonedDateTime from = ZonedDateTime.parse(fromDate);
        ZonedDateTime to = ZonedDateTime.parse(toDate);
        return (occurred.compareTo(from) >= 0) && (occurred.compareTo(to) < 0);
    }
    
    /**
     * GraphQLレスポンスからContribution情報を解析します
     * 
     * <p>parseContributionResponseと同様に、リポジトリ作成の場合はoccurredAtフィールドを
     * 使用して厳密な日付判定を行います。</p>
     * 
     * @param response GraphQL APIのレスポンス
     * @param username GitHubのユーザー名（継続日数計算用）
     * @return Contribution情報
     */
    private ContributionInfo parseContributionInfo(JsonNode response, String username) {
        JsonNode data = response.get("data");
        if (data == null || data.isNull()) {
            JsonNode errors = response.get("errors");
            if (errors != null && errors.isArray() && errors.size() > 0) {
                if (logger.isErrorEnabled()) {
                    logger.error("GraphQLエラー: {}", errors.toString());
                }
            }
            return new ContributionInfo(false, 0, 0);
        }
        JsonNode user = data.get("user");
        if (user == null || user.isNull()) {
            if (logger.isWarnEnabled()) {
                logger.warn("ユーザーが見つかりません");
            }
            return new ContributionInfo(false, 0, 0);
        }
        JsonNode contributions = user.get("contributionsCollection");
        if (contributions == null || contributions.isNull()) {
            return new ContributionInfo(false, 0, 0);
        }
        
        // 基本的なContribution数を計算
        int totalContributions = 
                getIntValue(contributions, "totalCommitContributions") +
                getIntValue(contributions, "totalIssueContributions") +
                getIntValue(contributions, "totalPullRequestContributions") +
                getIntValue(contributions, "totalPullRequestReviewContributions") +
                getIntValue(contributions, "totalRepositoryContributions");
        
        // 日付範囲を取得（GraphQLクエリのvariablesから取得、なければ今日のUTC範囲を使用）
        String fromDate = null;
        String toDate = null;
        try {
            JsonNode variables = response.get("variables");
            if (variables != null) {
                fromDate = variables.get("from").asText();
                toDate = variables.get("to").asText();
            }
        } catch (Exception e) {
            // GraphQLクエリのvariablesが存在しない場合は無視（fallback処理で対応）
        }
        if (fromDate == null || toDate == null) {
            ZonedDateTime today = LocalDate.now().atStartOfDay(ZoneOffset.UTC);
            ZonedDateTime tomorrow = today.plusDays(1);
            fromDate = today.format(DateTimeFormatter.ISO_INSTANT);
            toDate = tomorrow.format(DateTimeFormatter.ISO_INSTANT);
        }
        
        // 実際のアクティビティがあるかチェック（リポジトリ作成はoccurredAtで厳密判定）
        boolean hasActualActivity = hasActualContributions(contributions, fromDate, toDate);
        
        // カレンダーAPIで今日のContributionがあるかチェック
        int calendarContributionCount = getCalendarContributionCount(username);
        boolean hasCalendarContribution = calendarContributionCount > 0;
        
        if (logger.isInfoEnabled()) {
            logger.info("今日のContribution数: {} (実際のアクティビティ: {}, カレンダー: {}件)", 
                totalContributions, hasActualActivity, calendarContributionCount);
        }
        
        // 実際のアクティビティがある場合、またはカレンダーAPIでContributionが表示されている場合はContributionとしてカウント
        boolean hasContribution = hasActualActivity || hasCalendarContribution;
        // カレンダーAPIでContributionが検出された場合はその件数を使用、そうでなければ実際のアクティビティの件数を使用
        int actualContributionCount = hasCalendarContribution ? calendarContributionCount : (hasActualActivity ? totalContributions : 0);
        // 継続日数は常に計算する（今日Contributionがない場合でも昨日までの継続日数を表示）
        int streakDays = getStreakDays(username);
        
        return new ContributionInfo(hasContribution, actualContributionCount, streakDays);
    }

    /**
     * カレンダーAPIで今日のContribution件数を取得します
     * 
     * @param username GitHubのユーザー名
     * @return カレンダーAPIで今日のContribution件数
     */
    private int getCalendarContributionCount(String username) {
        try {
            String query = String.format("{\"query\": \"%s\", \"variables\": {\"username\": \"%s\"}}",
                    CALENDAR_QUERY.replace("\n", "\\n").replace("\"", "\\\""), username);
            JsonNode response = executeGraphQLQuery(query);
            JsonNode days = response
                .path("data").path("user")
                .path("contributionsCollection")
                .path("contributionCalendar")
                .path("weeks");
            if (days.isMissingNode() || !days.isArray()) return 0;
            
            // 今日の日付を取得
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            // 最新の週から今日のContributionを探す
            for (JsonNode week : days) {
                for (JsonNode day : week.path("contributionDays")) {
                    String date = day.path("date").asText();
                    int count = day.path("contributionCount").asInt();
                    if (date.equals(today) && count > 0) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("カレンダーAPIで今日({})のContributionを検出: {}件", today, count);
                        }
                        return count;
                    }
                }
            }
            
            return 0;
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("カレンダーAPIチェック中にエラーが発生しました: {}", e.getMessage(), e);
            }
            return 0;
        }
    }

    /**
     * GraphQLクエリを構築します
     * 
     * @param username GitHubのユーザー名
     * @param fromDate 開始日時（ISO8601形式）
     * @param toDate 終了日時（ISO8601形式）
     * @return GraphQLクエリのJSON文字列
     */
    private String buildGraphQLQuery(String username, String fromDate, String toDate) {
        return String.format("""
                {
                  "query": "%s",
                  "variables": {
                    "username": "%s",
                    "from": "%s",
                    "to": "%s"
                  }
                }
                """, 
                CONTRIBUTION_QUERY.replace("\n", "\\n").replace("\"", "\\\""),
                username,
                fromDate,
                toDate
        );
    }
    
    /**
     * GraphQLクエリを実行します
     * 
     * @param query GraphQLクエリのJSON文字列
     * @return GraphQL APIのレスポンス
     * @throws IOException HTTP通信エラーが発生した場合
     * @throws GitHubTokenException GitHubトークンが無効な場合
     */
    private JsonNode executeGraphQLQuery(String query) throws IOException, GitHubTokenException {
        RequestBody body = RequestBody.create(query, MediaType.parse("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(GITHUB_GRAPHQL_URL)
                .addHeader("Authorization", "Bearer " + githubToken)
                .addHeader("User-Agent", "GitHub-Grass-Checker")
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                // 401エラーの場合はトークン無効と判定
                if (response.code() == 401) {
                    if (logger.isErrorEnabled()) {
                        logger.error("GitHub API認証エラー (401): {}", responseBody);
                    }
                    throw new GitHubTokenException("GitHub Personal Access Tokenが無効または期限切れです。トークンをリジェネレートしてください。");
                }
                
                throw new IOException("GraphQL API呼び出しが失敗しました: " + response.code());
            }
            
            String responseBody = response.body().string();
            if (logger.isDebugEnabled()) {
                logger.debug("GraphQL Response: {}", responseBody);
            }
            
            return objectMapper.readTree(responseBody);
        }
    }
    
    /**
     * JsonNodeから整数値を安全に取得します
     * 
     * @param node JsonNode
     * @param fieldName フィールド名
     * @return フィールドの値（nullまたは数値以外の場合は0）
     */
    private int getIntValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asInt() : 0;
    }

    /**
     * Contribution情報を保持するクラス
     * 
     * <p>GitHubのContribution状況を表現するイミュータブルなクラスです。</p>
     */
    public static class ContributionInfo {
        private final boolean hasContribution;
        private final int contributionCount;
        private final int streakDays;
        
        /**
         * ContributionInfoを初期化します
         * 
         * @param hasContribution Contributionがあるかどうか
         * @param contributionCount Contributionの件数
         * @param streakDays 継続日数
         */
        public ContributionInfo(boolean hasContribution, int contributionCount, int streakDays) {
            this.hasContribution = hasContribution;
            this.contributionCount = contributionCount;
            this.streakDays = streakDays;
        }
        
        /**
         * Contributionがあるかどうかを取得します
         * 
         * @return Contributionがある場合はtrue、ない場合はfalse
         */
        public boolean hasContribution() { return hasContribution; }
        
        /**
         * Contributionの件数を取得します
         * 
         * @return Contributionの件数
         */
        public int getContributionCount() { return contributionCount; }
        
        /**
         * 継続日数を取得します
         * 
         * @return 継続日数（0以上の整数）
         */
        public int getStreakDays() { return streakDays; }
        
        @Override
        public String toString() {
            return String.format("ContributionInfo{hasContribution=%s, count=%d, streak=%d}", 
                hasContribution, contributionCount, streakDays);
        }
    }

    /**
     * GitHub Personal Access Tokenが無効または期限切れの場合にスローされる例外
     */
    public static class GitHubTokenException extends Exception {
        public GitHubTokenException(String message) {
            super(message);
        }
        
        public GitHubTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 