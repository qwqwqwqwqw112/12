package com.kafka.scala.demo3.subdemo3

import java.io.{ByteArrayInputStream, IOException}
import java.time.Duration
import java.util
import java.util.Properties
import java.io.ByteArrayInputStream

import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.avro.io.DecoderFactory
import org.apache.kafka.clients.consumer.{KafkaConsumer, OffsetAndMetadata}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException

import scala.collection.JavaConverters._

object KafkaConsumer {
  var consumer: KafkaConsumer[String, Array[Byte]] = null;

  def main(args: Array[String]): Unit = {
    val topic = "demo_topic_3";
    consumeFromKafka(topic)
  }

  def consumeFromKafka(topic: String) = {
    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("auto.offset.reset", "latest")
    props.put("group.id", "consumer-group")


    val consumerOffset = new util.HashMap[TopicPartition, OffsetAndMetadata]();
    consumer = new KafkaConsumer[String, Array[Byte]](props);
    consumer.subscribe(util.Arrays.asList(topic))

    //Read avro schema file
    val schema: Schema = new Parser().parse("{\"namespace\":\"student.avro \",\"type\":\"record\",\"name\":\"Student\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"age\",\"type\":\"int\"}]}")
    try {
      while (true) {
        val record = consumer.poll(Duration.ofMillis(1000)).asScala
        for (data <- record.iterator) {
          consumerOffset.put(new TopicPartition(data.topic(), data.partition()), new OffsetAndMetadata(data.offset()));
          val messageBody = data.value();
          val reader = new GenericDatumReader[GenericRecord](schema)
          var byteArrayInputStream: ByteArrayInputStream = null
          try {
            byteArrayInputStream = new ByteArrayInputStream(messageBody);
            val decoder = DecoderFactory.get.binaryDecoder(byteArrayInputStream, null)
            val genericRecord: GenericRecord = reader.read(null, decoder)
            print("Name : " + genericRecord.get("name"))
            print("Age : " + genericRecord.get("age"))
          } catch {
            case e: IOException =>
              e.printStackTrace()
          } finally {
            byteArrayInputStream.close()
          }
        }
      }
    } catch {
      case ex: WakeupException =>
        ex.printStackTrace();
    } finally {
      consumer.close(); //commit offsets and inform group coordinator
    }
  }
}
