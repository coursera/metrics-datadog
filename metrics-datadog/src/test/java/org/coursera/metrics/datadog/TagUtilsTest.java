package org.coursera.metrics.datadog;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;


public class TagUtilsTest {

  @Test
  public void mergeTagsWithoutDupKey() throws Exception {
    List<String> tags1 = new ArrayList<String>();
    tags1.add("key1:v1");
    tags1.add("key2:v2");
    List<String> tags2 = new ArrayList<String>();
    tags2.add("key3:v3");
    tags2.add("key4:v4");


    List<String> expected = new ArrayList<String>();
    expected.addAll(tags1);
    expected.addAll(tags2);
    assert(new TreeSet<String>(TagUtils.mergeTags(tags1, tags2)).equals(
            new TreeSet<String>(expected)));
  }

  @Test
  public void mergeTagsWithDupKey() throws Exception {
    List<String> tags1 = new ArrayList<String>();
    tags1.add("key1:v1");
    tags1.add("key2:v2");
    List<String> tags2 = new ArrayList<String>();
    tags2.add("key2:v3");
    tags2.add("key4:v4");


    List<String> expected = new ArrayList<String>();
    expected.add("key1:v1");
    expected.addAll(tags2);
    assert(new TreeSet<String>(TagUtils.mergeTags(tags1, tags2)).equals(
            new TreeSet<String>(expected)));
  }

  @Test
  public void mergeTagsWithMultipleDelimiters() throws Exception {
    List<String> tags1 = new ArrayList<String>();
    tags1.add("key1:value_with_:_in_the_middle");
    List<String> tags2 = new ArrayList<String>();
    tags2.add("key2:value");


    List<String> expected = new ArrayList<String>();
    expected.addAll(tags1);
    expected.addAll(tags2);
    assert(new TreeSet<String>(TagUtils.mergeTags(tags1, tags2)).equals(
            new TreeSet<String>(expected)));
  }


  @Test
  public void mergeTagsWithNoValue() throws Exception {
    List<String> tags1 = new ArrayList<String>();
    tags1.add("no_value:");
    List<String> tags2 = new ArrayList<String>();
    tags2.add("key:value");


    List<String> expected = new ArrayList<String>();
    expected.addAll(tags2);
    assert(new TreeSet<String>(TagUtils.mergeTags(tags1, tags2)).equals(
            new TreeSet<String>(expected)));
  }

  @Test
  public void mergeTagsWithNoKey() throws Exception {
    List<String> tags1 = new ArrayList<String>();
    tags1.add(":value");
    List<String> tags2 = new ArrayList<String>();
    tags2.add("key:value");


    List<String> expected = new ArrayList<String>();
    expected.addAll(tags2);
    assert(new TreeSet<String>(TagUtils.mergeTags(tags1, tags2)).equals(
            new TreeSet<String>(expected)));
  }

  @Test
  public void mergeTagsWithNoDelimiter() throws Exception {
    List<String> tags1 = new ArrayList<String>();
    tags1.add("discouraged_value");
    List<String> tags2 = new ArrayList<String>();
    tags2.add("key:value");


    List<String> expected = new ArrayList<String>();
    expected.addAll(tags2);
    assert(new TreeSet<String>(TagUtils.mergeTags(tags1, tags2)).equals(
            new TreeSet<String>(expected)));
  }
}
