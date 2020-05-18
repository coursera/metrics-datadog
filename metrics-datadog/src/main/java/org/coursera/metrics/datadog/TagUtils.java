package org.coursera.metrics.datadog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.IllegalArgumentException;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TagUtils {
  private static final Logger LOG = LoggerFactory.getLogger(TagUtils.class);

  /**
   *
   * @param tags1 list of tags, each tag should be in the format of "key:value"
   * @param tags2 list of tags, each tag should be in the format of "key:value"
   * @return merged tags list. If there is duplicated key, tags in tags2 will overwrite tags
   * in tags1, and tags in the back of the list will overwrite tags in the front of the list.
   */
  public static List<String> mergeTags(List<String> tags1, List<String> tags2) {
    if (tags1 == null || tags1.isEmpty()) {
      return tags2;
    } else if (tags2 == null || tags2.isEmpty()) {
      return tags1;
    }

    List<String> newTags = new ArrayList<String>();
    newTags.addAll(tags1);
    newTags.addAll(tags2);

    Map<String, String> map = new HashMap<String, String>();
    for (String tag : newTags) {

      int delimiterIndex = tag.indexOf(":");
      int tagLength = tag.length();
      if (tagLength < 1 || delimiterIndex <= 0 || delimiterIndex == tagLength - 1) {
        LOG.warn("Invalid tag: " + tag);
      } else {
        String key = tag.substring(0, delimiterIndex);
        String value = tag.substring(delimiterIndex + 1);
        map.put(key, value);
      }
    }

    newTags.clear();
    for (Map.Entry entry : map.entrySet()) {
      newTags.add(entry.getKey() + ":" + entry.getValue());
    }

    return newTags;
  }
}
