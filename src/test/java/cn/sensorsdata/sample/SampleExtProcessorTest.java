package cn.sensorsdata.sample;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

/**
 * Created by fengjiajie on 17/4/10.
 */
public class SampleExtProcessorTest {

  @Test public void testProcess() throws Exception {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(" {");
    stringBuilder.append("     \"distinct_id\":\"2b0a6f51a3cd6775\",");
    stringBuilder.append("     \"time\":1434556935000,");
    stringBuilder.append("     \"type\":\"track\",");
    stringBuilder.append("     \"event\":\"ViewProduct\",");
    stringBuilder.append("     \"properties\":{");
    stringBuilder.append("         \"product_name\":\"苹果\"");
    stringBuilder.append("     }");
    stringBuilder.append(" }");

    SampleExtProcessor sampleExtProcessor = new SampleExtProcessor();
    ObjectMapper objectMapper = new ObjectMapper();

    String processResult = sampleExtProcessor.process(stringBuilder.toString());
    JsonNode recordNode = objectMapper.readTree(processResult);

    assertEquals("添加的字段应该是水果", "水果", recordNode.get("properties").get("product_classify").asText());
  }
}