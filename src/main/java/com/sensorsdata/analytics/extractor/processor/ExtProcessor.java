package com.sensorsdata.analytics.extractor.processor;

public interface ExtProcessor {
  String process(String record) throws Exception;
}