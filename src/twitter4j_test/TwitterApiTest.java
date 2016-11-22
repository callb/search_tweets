package twitter4j_test;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.*;
import java.util.*;

/**
 * Created by ben on 11/19/16.
 */
public class TwitterApiTest {

    static String consumerKey = "";
    static String consumerSecret = "";
    static String accessToken = "";
    static String accessTokenSecret = "";
    static Twitter twitter = getConfig();
    static String queriedUser;

    /**
     * These are the settings. Here you can set the number of tweets to get,
     * the users you want to get tweets from, and the search terms you are
     * looking for in each tweet
     */
    // Must be a multiple of 100
    private static final int MAX_TWEETS = 5000;
    private static final ArrayList<String> users =
            new ArrayList<>(Arrays.asList("melanie_kc", "cfkargentina"));
    private static final ArrayList<String> search_terms =
            new ArrayList<>(Arrays.asList("Argentina", "Kirchner", "Peron"));

    // Key: Username, Value: List of that user's tweets
    private static Map<String, List<Status>> userTweetsMap = new HashMap<>();
    // Key: Term to search for in tweets, Value: List of
    // tweets across all users that contain the associated keyword
    private static Map<String, List<Status>> tweetsWithSearchTerms = initTermsMap();

    public static void main(String[] args) throws TwitterException, InterruptedException, IOException {
        getTwitterApiData(users);
        printTweetsToFile();
        System.out.println("Twitter Search Completed! View the results in the tweets.txt file.");
        //TODO: Make the GUI for easy viewing of settings and data
    }

    private static void printTweetsToFile() throws IOException {

        final FileWriter writer = new FileWriter("tweets.txt");
        tweetsWithSearchTerms.forEach((keyword, tweets) -> {
            try {
                writer.write("TWEETS CONTAINING THE WORD '" + keyword + "'\n\n");
                for (Status tweet : tweets) {
                    writer.write("User: " + tweet.getUser().getName() + "\n");
                    writer.write(tweet.getText() + "\n\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        writer.close();
    }

    private static Map<String, List<Status>> initTermsMap() {
        Map<String, List<Status>> termsMap = new HashMap<>();
        search_terms.forEach((keyword) -> {
            termsMap.put(keyword, new ArrayList<>());
        });
        return termsMap;
    }

    // creates a map of users to tweets and the map of search terms to tweets
    private static void getTwitterApiData(List<String> users) throws InterruptedException {
        for (String user : users) {
            userTweetsMap.put(user, getTweetsFromUser(user));
        }
    }

    private static void checkApiLimit() throws InterruptedException {
        try {
            Map<String, RateLimitStatus> rateLimitStatus = twitter.getRateLimitStatus("search");
            RateLimitStatus searchTweetsRateLimit = rateLimitStatus.get("/search/tweets");
            if (searchTweetsRateLimit.getRemaining() == 0) {
                System.out.printf("WARNING: Sleeping for %d seconds due to rate limits...\n",
                        searchTweetsRateLimit.getSecondsUntilReset());
                Thread.sleep((searchTweetsRateLimit.getSecondsUntilReset()+2) * 1000l);
                System.out.print("API calls now available, twitter search resumed.");
            }
        } catch(TwitterException e) {
            System.out.printf("WARNING: Sleeping for %d seconds due to rate limits\n",
                    e.getRateLimitStatus().getSecondsUntilReset());
            Thread.sleep((e.getRateLimitStatus().getSecondsUntilReset() + 2) * 1000l);
            System.out.print("API calls now available, twitter search resumed.");
        }
    }

    private static List<Status> getTweetsFromUser(String username) throws InterruptedException {
        System.out.println("Retrieving tweets for " + username + "...");
        queriedUser = username;
        int pageNum = 1;
        try {
            List<Status> statuses = new ArrayList<>();
            for (int queryNumber = 0; queryNumber < MAX_TWEETS / 100; queryNumber++) {
                // always call checkApiLimit() before making a twitter api call
                checkApiLimit();
                int size = statuses.size();
                Paging page = new Paging(pageNum++, 100);

                //Make the API call
                List<Status> result = twitter.getUserTimeline(username, page);
                statuses.addAll(result);

                if (statuses.size() == size)
                    break;

                for (Status status : result) {

                    //sorts tweets containing search terms
                    search_terms.forEach((keyword) -> {
                        if (status.getText().contains(keyword)) {
                            tweetsWithSearchTerms.get(keyword).add(status);
                        }
                    });
                }
            }

            return statuses;
        } catch (TwitterException e) {
            e.printStackTrace();
        }

        //when twitter exception occurs, return an empty list
        return new ArrayList<>();
    }

    private static Twitter getConfig() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(consumerKey)
                .setOAuthConsumerSecret(consumerSecret)
                .setOAuthAccessToken(accessToken)
                .setOAuthAccessTokenSecret(accessTokenSecret);
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }
}