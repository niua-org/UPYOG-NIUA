// kafka-node was abandoned and incompatible with Node 22, replaced with kafkajs
// ConsumerGroup replaced with kafkajs consumer API
// async.queue retained for sequential processing

import { Kafka } from "kafkajs";
import logger from "../config/logger.js";
import envVariables from "../EnvironmentVariables.js";
import { createNoSave } from "../index.js";
import async from "async";

export const listenConsumer = async (topics) => {
  // Create a new Kafka instance with broker(s)
  // Split by comma to support multiple brokers e.g. "host1:9092,host2:9092"
  const kafka = new Kafka({
    brokers: envVariables.KAFKA_BROKER_HOST.split(",").map(b=> b.trim()),
  });

  const consumer = kafka.consumer({ groupId: "bulk-pdf" });

  // Handle consumer crash events — logs error if Kafka connection crashes
  consumer.on(consumer.events.CRASH, (event) => {
    logger.error("Kafka consumer crash: " + JSON.stringify(event.payload));
  });

// Handle consumer connect event — logs when consumer is ready
  consumer.on(consumer.events.CONNECT, () => {
    logger.info("Consumer is ready");
  });

  await consumer.connect();

// Subscribe to each topic in the list
// fromBeginning: false means only consume new messages (not from start of topic)
  for (const topic of topics) {
    if (topic) await consumer.subscribe({ topic, fromBeginning: false });
  }

const q = async.queue(async (data) => {
  try {
 // Trigger PDF generation for the received data
    await createNoSave(data, null, () => {}, () => {});
  } catch (err) {
// Log any errors that occur during PDF generation
    logger.error("Queue worker error: " + err.message);
    logger.error(err.stack || err);
  }
}, 1);

// Start consuming messages from subscribed topics
  await consumer.run({
    eachMessage: async ({ message }) => {
      try {
 // Convert Kafka message Buffer to string and parse as JSON
       const data = JSON.parse(message.value.toString());
        await new Promise((resolve) => q.push(data, resolve));
      } catch (error) {
        logger.error("error in create request by consumer: " + error.message);
        logger.error(error.stack || error);
      }
    }
  });

// Graceful shutdown handler — ensures clean disconnect from Kafka
// before the process exits when SIGTERM or SIGINT is received  
let isShuttingDown = false;
  const shutdown = async () => {
    if (isShuttingDown) return; // Prevent multiple shutdown attempts
    isShuttingDown = true;
    logger.info("Disconnecting Kafka consumer...");
    await consumer.disconnect();
    process.exit(0);
  };
  process.on("SIGTERM", shutdown);
  process.on("SIGINT", shutdown);

};
