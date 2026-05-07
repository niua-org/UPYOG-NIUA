// Before: used kafka-node which is abandoned and incompatible with Node 22
// Change: replaced kafka-node with kafkajs, updated producer API to kafkajs style

import { Kafka } from "kafkajs";
import logger from "../config/logger.js";
import envVariables from "../EnvironmentVariables.js";

const kafka = new Kafka({
  brokers: envVariables.KAFKA_BROKER_HOST.split(",").map(b => b.trim())});

const producer = kafka.producer();

// await producer.connect();
export const connectProducer = async () => {
  await producer.connect();
};

producer.on(producer.events.CONNECT, () => logger.info("Producer is ready"));
producer.on(producer.events.DISCONNECT, () => logger.error("Producer disconnected"));

export default producer;