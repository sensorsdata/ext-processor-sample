package cn.sensorsdata.sample;

import com.sensorsdata.analytics.extractor.processor.ExtProcessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by fengjiajie on 16/9/28.
 */
public class SampleExtProcessor implements ExtProcessor {

  private ObjectMapper objectMapper = new ObjectMapper();

  public SampleExtProcessor() {
  }

  public String process(String record) throws Exception {
    // 传入参数为一条符合 Sensors Analytics 数据格式定义的 Json
    // 数据格式定义 https://www.sensorsdata.cn/manual/data_schema.html
    JsonNode recordNode = objectMapper.readTree(record);
    ObjectNode propertiesNode = (ObjectNode) recordNode.get("properties");

    // 例如传入的一条需要处理的数据是:
    //
    // {
    //     "distinct_id":"2b0a6f51a3cd6775",
    //     "time":1434556935000,
    //     "type":"track",
    //     "event":"ViewProduct",
    //     "properties":{
    //         "product_name":"苹果"
    //     }
    // }
    //
    // 如果是“苹果”或“梨”, 那么添加一个字段标记产品为“水果”;
    // 如果是“萝卜”或“白菜”, 那么标记为“蔬菜”;

    if (propertiesNode.has("product_name")) {
      String productName = propertiesNode.get("product_name").asText();
      if ("苹果".equals(productName) || "梨".equals(productName)) {
        propertiesNode.put("product_classify", "水果");
      } else if ("萝卜".equals(productName) || "白菜".equals(productName)) {
        propertiesNode.put("product_classify", "蔬菜");
      }
    }

    return recordNode.toString();
  }
}

