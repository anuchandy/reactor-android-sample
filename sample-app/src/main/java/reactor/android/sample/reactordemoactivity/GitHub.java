package reactor.android.sample.reactordemoactivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import reactor.core.publisher.Mono;
import reactor.okhttp.http.client.HttpClient;
import reactor.okhttp.http.client.HttpClientBuilder;
import reactor.okhttp.http.client.HttpMethod;
import reactor.okhttp.http.client.HttpRequest;
import reactor.okhttp.http.client.HttpResponse;

public class GitHub {
    private final String accessToken;
    private final HttpClient httpClient;
    private final Gson gson;
    private final static String githubApiHost = "api.github.com";
    private final static String githubApiBaseUrl = "https://" + githubApiHost;
    private final static String githubApiReposBaseUrl = githubApiBaseUrl + "/repos";

    public GitHub(String accessToken) {
        this.accessToken = accessToken; // Null allowed for anonymous api calls.
        this.httpClient = new HttpClientBuilder()
                .addInterceptor(requestInterceptor())
                .build();
        this.gson = new Gson();

    }

    public Mono<List<GitHub.Contributor>> listContributors(String organization, String repoName) {
        HttpRequest request = new HttpRequest(HttpMethod.GET, toUrl(String.format(githubApiReposBaseUrl + "/" + organization +"/" + repoName + "/contributors")));
        return this.httpClient.send(request)
                .flatMap((Function<HttpResponse, Mono<String>>) httpResponse -> {
                    if (httpResponse.getStatusCode() >= 400) {
                        return httpResponse.getBodyAsString()
                                .flatMap(errorContent -> Mono.<String>error(new Throwable("StatusCode:" + httpResponse.getStatusCode() + " Content: " + errorContent)))
                                .switchIfEmpty(Mono.defer(() -> Mono.error(new Throwable("StatusCode:" + httpResponse.getStatusCode()))));
                    } else {
                        return httpResponse.getBodyAsString();
                    }
                })
                .map(content -> this.gson.fromJson(content, new TypeToken<ArrayList<Contributor>>(){}.getType()));
    }

    private HttpClient.Interceptor requestInterceptor() {
        return (httpRequest, nextInterceptor) -> {
            if (accessToken != null) {
                httpRequest.setHeader("Authorization", "token " + accessToken);
            }
            httpRequest.setHeader("Accept", "application/vnd.github.v3+json");
            httpRequest.setHeader("Host", githubApiHost);
            return nextInterceptor.intercept(httpRequest);
        };
    }

    private static URL toUrl(String str) {
        try {
            return new URL(str);
        } catch (MalformedURLException mue) {
            throw new RuntimeException(mue);
        }
    }

    public class Contributor {
        private String login;
        private int id;
        private String node_id;
        private String avatar_url;
        private String gravatar_id;
        private String url;
        private String html_url;
        private String followers_url;
        private String following_url;
        private String gists_url;
        private String starred_url;
        private String subscriptions_url;
        private String organizations_url;
        private String repos_url;
        private String events_url;
        private String received_events_url;
        private String type;
        private boolean site_admin;
        private int contributions;

        public String getLogin() {
            return this.login;
        }
        public int getId() {
            return this.id;
        }
        public String getNodeId() {
            return this.node_id;
        }
        public String getAvatarUrl() {
            return this.avatar_url;
        }
        public String getGravatarId() {
            return this.gravatar_id;
        }
        public String getUrl() {
            return this.url;
        }
        public String getHtmlUrl() {
            return this.html_url;
        }
        public String getFollowersUrl() {
            return this.followers_url;
        }
        public String getFollowingUrl() {
            return this.following_url;
        }
        public String getGistsUrl() {
            return this.gists_url;
        }
        public String getStarredUrl() {
            return this.starred_url;
        }
        public String getSubscriptionsUrl() {
            return this.subscriptions_url;
        }
        public String getOrganizationsUrl() {
            return this.organizations_url;
        }
        public String getReposUrl() {
            return this.repos_url;
        }
        public String getEventsUrl() {
            return this.events_url;
        }
        public String getReceivedEventsUrl() {
            return this.received_events_url;
        }
        public String getType() {
            return this.type;
        }
        public boolean isSiteAdmin() {
            return this.site_admin;
        }
        public int getContributions() {
            return this.contributions;
        }
    }
}
