# Reactor Android Sample

A sample Android application that make use of following modules:

- Reactor: [reactor-core](https://github.com/reactor/reactor-core)
- Reactor OkHttp HttpClient: [reactor-okhttp](https://github.com/anuchandy/reactor-okhttp)
- Reactor Android Scheduler: [reactor-android](https://github.com/anuchandy/reactor-android)

Application min SDK version is 26.

Sample demonstrates using Reactor to make asynchronous HTTP call (GitHub v3 REST endpoint in this case) and display the result in UI.
 
GitHub expose an API to retrieve contributors of any public repo anonymously. The sample uses this API to retrieve top 5 contributors of atmost 3 repos.

![alt text](https://github.com/anuchandy/reactor-android-sample/blob/master/img/Reactor-Android-Sample.png)

## Using reactor-okhttp

[reactor-okhttp](https://github.com/anuchandy/reactor-okhttp) includes a Reactor based light weight `HttpClient` that uses [Square OkHttp](https://github.com/square/okhttp) underneath.

### Configuring OkHttp HttpClient for GitHub API

The `HttpClientBuilder` is used to configure and build an reactor-okhttp `HttpClient`.

```java
// import reactor.okhttp.http.client.HttpClient;

final String accessToken = null; // github token, null for anonymous access.
final String githubApiHost = "api.github.com";

HttpClient httpClient = new HttpClientBuilder()
    .addInterceptor((httpRequest, nextInterceptor) -> {
        if (accessToken != null) {
            httpRequest.setHeader("Authorization", "token " + accessToken);
        }
        httpRequest.setHeader("Accept", "application/vnd.github.v3+json");
        httpRequest.setHeader("Host", githubApiHost);
        return nextInterceptor.intercept(httpRequest);
    })
    .build();
```

`addInterceptor` is used for registering an interceptor that sets necessary HTTP headers needed for calling GitHub API, this interceptor gets called for all http calls via HttpClient.


### Calling GitHub API for list contributor

An Http call can be made using `HttpClient`'s `send` method.

```java
// import reactor.okhttp.http.client.HttpMethod;
// import reactor.okhttp.http.client.HttpRequest;
// import reactor.okhttp.http.client.HttpResponse;
// import com.google.gson.Gson;
// import com.google.gson.reflect.TypeToken;

final String githubOrganization = "reactor";
final String githubRepo = "reactor-core";
final String contributorsEndpoint = "https://api.github.com/repos/" + githubOrganization + "/" + githubRepo + "/contributors";
HttpRequest request = new HttpRequest(HttpMethod.GET, toUrl(contributorsEndpoint)));
Mono<List<GitHub.Contributor>> contributorsMono = httpClient.send(request)
    .flatMap((Function<HttpResponse, Mono<String>>) httpResponse -> {
        if (httpResponse.getStatusCode() >= 400) {
            return httpResponse.getBodyAsString()
                .flatMap(errorContent -> Mono.<String>error(toThrowable(httpResponse.getStatusCode(), errorContent)))
                .switchIfEmpty(Mono.defer(() -> Mono.error(toThrowable(httpResponse.getStatusCode(), null)))));
        } else {
            return httpResponse.getBodyAsString();
        }
    })
    .map(content -> (new Gson()).fromJson(content, new TypeToken<ArrayList<Contributor>>(){}.getType()));

private static Throwable toThrowable(int statusCode, String errorContent) {
    if (errorContent != null) {
        return new Throwable("StatusCode:" + httpResponse.getStatusCode() + " Content: " + errorContent);
    } else {
      return new Throwable("StatusCode:" + httpResponse.getStatusCode());
    }
}
```

A subscription to `contributorsMono` initate the call to GitHub list contributors endpoint.

```java
contributorsMono.subscribe(contributors -> {
    // process contributors
}, throwable -> {
    // log error
});
```

## Using reactor-android

[reactor-android](https://github.com/anuchandy/reactor-android) includes a Reactor [Scheduler](https://projectreactor.io/docs/core/release/api/reactor/core/scheduler/Scheduler.html) implementation backed by Android main thread (UI thread). This scheduler is used to run Reactor subscription callbacks on Android main thread, this enables callbacks to access applications UI elements.

```java
// import reactor.android.schedulers.AndroidSchedulers;
final android.view.View view = .. // The main view
final int resultTextViewId = .. // The resource id of TextView to display result
Disposable disposable = contributorsMono
    .subscribeOn(Schedulers.elastic())
    .publishOn(AndroidSchedulers.mainThread())
    .doOnNext(contributors -> {
        String contributorsStr = String.join("\n", contributors.stream().map(c -> c.getLogin()).collect(Collectors.toList()));
        TextView resultTextView = view.findViewById(resultTextViewId);
        resultTextView.setText(contributorsStr);
    })
    .doOnError(throwable -> {
        TextView resultTextView = view.findViewById(resultTextViewId);
        resultTextView.setText(throwable.getMessage());
    });
```

The operator `publishOn` is used here to set Reactor Android scheduler as the scheduler to run call backs.
