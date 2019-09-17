package reactor.android.sample.reactordemoactivity;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;
import java.util.stream.Collectors;

import reactor.android.schedulers.AndroidSchedulers;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ReactorDemoFragment extends Fragment implements View.OnClickListener {
    private final Disposable.Composite disposables = Disposables.composite();

    public static ReactorDemoFragment newInstance() {
        return new ReactorDemoFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.reactor_demo_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        Button fetchContributorsBtn = rootView.findViewById(R.id.fetchContributorsBtn);
        fetchContributorsBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View buttonView) {
        setHeaderValue(buttonView.getRootView(), R.id.contributors1HdrTextView,  null);
        setHeaderValue(buttonView.getRootView(), R.id.contributors2HdrTextView,  null);
        setHeaderValue(buttonView.getRootView(), R.id.contributors3HdrTextView,  null);

        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        String token = preference.getString("github_token", null);
        GitHub gitHub = new GitHub(token);
        //
        Mono<List<GitHub.Contributor>> contributors1Mono = configureListContributorMono(gitHub,
                buttonView.getRootView(),
                R.id.orgName1EditView,
                R.id.repoName1EditView,
                R.id.contributors1HdrTextView,
                R.id.contributors1TextView);

        Mono<List<GitHub.Contributor>> contributors2Mono = configureListContributorMono(gitHub,
                buttonView.getRootView(),
                R.id.orgName2EditView,
                R.id.repoName2EditView,
                R.id.contributors2HdrTextView,
                R.id.contributors2TextView);

        Mono<List<GitHub.Contributor>> contributors3Mono = configureListContributorMono(gitHub,
                buttonView.getRootView(),
                R.id.orgName3EditView,
                R.id.repoName3EditView,
                R.id.contributors3HdrTextView,
                R.id.contributors3TextView);

        Flux<List<GitHub.Contributor>> mergedFlux = Flux.mergeDelayError(1, contributors1Mono,
                contributors2Mono,
                contributors3Mono);

        Disposable disposable = mergedFlux.subscribe(contributors -> {}, throwable -> {});
        disposables.add(disposable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }

    private Mono<List<GitHub.Contributor>> configureListContributorMono(GitHub github,
                                                                            View view,
                                                                           int gitOrganizationEditViewId,
                                                                           int gitRepoEditViewId,
                                                                           int gitContributorsHeaderTextView,
                                                                           int gitContributorsResultTextView) {
        EditText gitOrgEditText = view.findViewById(gitOrganizationEditViewId);
        String gitOrgName = gitOrgEditText.getText().toString();
        EditText gitRepoEditText = view.findViewById(gitRepoEditViewId);
        String gitRepoName = gitRepoEditText.getText().toString();
        //
        if (!Util.isNullOrEmpty(gitOrgName) && !Util.isNullOrEmpty(gitRepoName)) {
            setHeaderValue(view, gitContributorsHeaderTextView, gitOrgName + ":" + gitRepoName);
            return github.listContributors(gitOrgName, gitRepoName)
                    .subscribeOn(Schedulers.elastic())
                    .publishOn(AndroidSchedulers.mainThread())
                    .doOnNext(contributors -> {
                        TextView contributorsTextView = view.findViewById(gitContributorsResultTextView);
                        contributorsTextView.setText(contributorNamesString(contributors, 5));
                    })
                    .doOnError(throwable -> {
                        TextView contributorsTextView = view.findViewById(gitContributorsResultTextView);
                        contributorsTextView.setText(throwable.getMessage());
                    });
        } else {
            return Mono.empty();
        }
    }

    private static void setHeaderValue(View view, int id, String value) {
        if (value == null) {
            TextView hdrTextView = view.findViewById(id);
            hdrTextView.setVisibility(View.GONE);
        } else {
            TextView hdrTextView = view.findViewById(id);
            hdrTextView.setVisibility(View.VISIBLE);
            hdrTextView.setText(value);
        }
    }

    private static String contributorNamesString(List<GitHub.Contributor> contributors, int limit) {
        limit = contributors.size() < limit ? contributors.size() : limit;
        return String.join("\n", contributors.stream().map(c -> c.getLogin()).limit(limit).collect(Collectors.toList()));
    }
}
