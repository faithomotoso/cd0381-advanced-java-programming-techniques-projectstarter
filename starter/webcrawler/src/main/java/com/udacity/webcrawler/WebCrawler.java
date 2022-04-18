package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.profiler.Profiled;
import org.checkerframework.checker.units.qual.A;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

/**
 * The main interface that defines the web crawler API.
 */
public interface WebCrawler {

  /**
   * Starts a crawl at the given URLs.
   *
   * @param startingUrls the starting points of the crawl.
   * @return the {@link CrawlResult} of the crawl.
   */
  @Profiled
  CrawlResult crawl(List<String> startingUrls);

  /**
   * Returns the maximum amount of parallelism (number of CPU cores) supported by this web crawler.
   */
  default int getMaxParallelism() {
    return 1;
  }

  /**
   * Reads the robot.txt file from the starting url if available
   * @param url
   * @return a list of urls to ignore
   */
  default List<String> getDisallowedUrls(String url) {
    try (BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(new URL(url + "robots.txt").openStream())
    )) {
      List<String> disallowedUrls = new ArrayList<>();

      String line;
      while ((line = bufferedReader.readLine()) != null) {
        if (line.startsWith("Disallow")) {
          String[] arr = line.split(":");
          disallowedUrls.add(arr[1]);
        }
      }
      return disallowedUrls;
    } catch (Exception e) {
      return List.of();
    }
  }
}
