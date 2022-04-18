package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final ForkJoinPool pool;
    private final PageParserFactory parserFactory;
    private final int maxDepth;
    private final List<Pattern> ignoredUrls;

    @Inject
    ParallelWebCrawler(
            Clock clock,
            PageParserFactory parserFactory,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @TargetParallelism int threadCount,
            @MaxDepth int maxDepth,
            @IgnoredUrls List<Pattern> ignoredUrls) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
        this.parserFactory = parserFactory;
        this.maxDepth = maxDepth;
        this.ignoredUrls = ignoredUrls;
    }

    @Override
    public CrawlResult crawl(List<String> startingUrls) {
        Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());
        Map<String, Integer> counts = Collections.synchronizedMap(new HashMap<>());


        ParallelWebCrawlerTask.Builder taskBuilder = new ParallelWebCrawlerTask.Builder();
        taskBuilder.setBuilder(taskBuilder)
                .setCounts(counts)
                .setDeadline(clock.instant().plus(timeout))
                .setMaxDepth(maxDepth)
                .setIgnoredUrls(ignoredUrls)
                .setParserFactory(parserFactory)
                .setVisitedUrls(visitedUrls)
                .setClock(clock);

        List<Future<Void>> futures = new ArrayList<>();

        for (String url : startingUrls) {
            futures.add(pool.submit(taskBuilder
                    .setStartingUrl(url)
                    .setDisallowedUrls(getDisallowedUrls(url))
                    .build()));
        }

        try {

            for (Future<Void> future : futures) {
                future.get();
            }

            pool.shutdown();

            int visitedUrlsSize = visitedUrls.size();
            Map<String, Integer> countsRes = counts.isEmpty() ?
                    counts : WordCounts.sort(counts, popularWordCount);

            return new CrawlResult.Builder()
                    .setWordCounts(countsRes)
                    .setUrlsVisited(visitedUrlsSize)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return new CrawlResult.Builder().build();
        }
    }

    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }

}
