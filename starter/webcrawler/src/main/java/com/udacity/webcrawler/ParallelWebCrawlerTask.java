package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

// Using RecursiveAction because Set, List and Map can be modified if passed down
public final class ParallelWebCrawlerTask extends RecursiveAction {

    private final String startingUrl;
    private final Set<String> visitedUrls;
    private final List<Pattern> ignoredUrls;
    private final PageParserFactory parserFactory;
    private final Map<String, Integer> counts;
    private final int maxDepth;
    private final Instant deadline;
    private final ParallelWebCrawlerTask.Builder builder;
    private final List<String> disallowedUrls;

    // I tried injecting the clock but it didn't work, got null pointers
    private final Clock clock;

    private ParallelWebCrawlerTask(String startingUrl,
                                   Set<String> visitedUrls,
                                   List<Pattern> ignoredUrls,
                                   PageParserFactory parserFactory,
                                   Map<String, Integer> counts,
                                   int maxDepth,
                                   Instant deadline,
                                   Clock clock,
                                   ParallelWebCrawlerTask.Builder builder,
                                   List<String> disallowedUrls) {
        this.startingUrl = startingUrl;
        this.visitedUrls = visitedUrls;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
        this.counts = counts;
        this.maxDepth = maxDepth;
        this.deadline = deadline;
        this.clock = clock;
        this.builder = builder;
        this.disallowedUrls = disallowedUrls;
    }

    static final class Builder {
        private String startingUrl;
        private Set<String> visitedUrls;
        private List<Pattern> ignoredUrls;
        private PageParserFactory parserFactory;
        private Map<String, Integer> counts;
        private int maxDepth;
        private Instant deadline;
        private Clock clock;
        private ParallelWebCrawlerTask.Builder builder;
        private List<String> disallowedUrls;

        public Builder setStartingUrl(String startingUrl) {
            this.startingUrl = startingUrl;
            return this;
        }

        public Builder setVisitedUrls(Set<String> visitedUrls) {
            this.visitedUrls = visitedUrls;
            return this;
        }

        public Builder setIgnoredUrls(List<Pattern> ignoredUrls) {
            this.ignoredUrls = ignoredUrls;
            return this;
        }

        public Builder setParserFactory(PageParserFactory parserFactory) {
            this.parserFactory = parserFactory;
            return this;
        }

        public Builder setCounts(Map<String, Integer> counts) {
            this.counts = counts;
            return this;
        }

        public Builder setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder setDeadline(Instant deadline) {
            this.deadline = deadline;
            return this;
        }

        public Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder setBuilder(Builder builder) {
            this.builder = builder;
            return this;
        }

        public Builder setDisallowedUrls(List<String> disallowedUrls) {
            this.disallowedUrls = disallowedUrls;
            return this;
        }

        public ParallelWebCrawlerTask build() {
            return new ParallelWebCrawlerTask(
                    startingUrl,
                    visitedUrls,
                    ignoredUrls,
                    parserFactory,
                    counts,
                    maxDepth,
                    deadline,
                    clock,
                    builder,
                    disallowedUrls
            );
        }
    }

    @Override
    protected void compute() {
        crawlInternal(startingUrl, deadline, maxDepth, counts, visitedUrls, disallowedUrls);
    }

    private void crawlInternal(
            String url,
            Instant deadline,
            int maxDepth,
            Map<String, Integer> counts,
            Set<String> visitedUrls,
            List<String> disallowedUrls) {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }

        for (String disallowedUrl : disallowedUrls) {
            if (url.contains(disallowedUrl)) {
                return;
            }
        }

        if (visitedUrls.contains(url)) {
            return;
        }
        visitedUrls.add(url);
        // This handles the downloading/loading local html and parsing for words and links
        PageParser.Result result = parserFactory.get(url).parse();
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            counts.compute(e.getKey(), (k, v) -> v != null ? e.getValue() + counts.get(e.getKey()) : e.getValue());
        }

        List<ParallelWebCrawlerTask> subtasks = new ArrayList<>();

        for (String link : result.getLinks()) {
            subtasks.add(
                    builder.setMaxDepth(maxDepth - 1).setStartingUrl(link).build()
            );
        }

        if (!subtasks.isEmpty()) {
            invokeAll(subtasks);
        }
    }

}
