import { Kafka, Partitioners } from "kafkajs";
import fs from "fs";
import { TerminalService } from "./TerminalService";
import os from "os";
import chalk from "chalk";
import { TokenService } from "./TokenService";
import { getFileAsJson } from "../utils/terminal-utils";
import { v4 as uuidv4 } from "uuid";

async function connectToKafka(local: boolean): Promise<Kafka> {
  if (local) {
    console.log("Kobler til lokal kafka cluster");
    return connecToLocalKafka();
  }
  return connectToAivenKafka();
}

async function connecToLocalKafka() {
  return new Kafka({
    clientId: "bidrag-cli",
    brokers: ["127.0.0.1:9092"],
    ssl: false,
  });
}

async function connectToAivenKafka() {
  const cluster = await TokenService.getNaisCluster();
  const path = await TerminalService.creatAivenService();
  await new Promise((resolve) => setTimeout(resolve, 1000)); // Add 1-second delay
  return new Kafka({
    clientId: "bidrag-cli",
    brokers: [
      cluster.startsWith("prod")
        ? "nav-prod-kafka-nav-prod.aivencloud.com:26484"
        : "nav-dev-kafka-nav-dev.aivencloud.com:26484",
    ],
    ssl: {
      rejectUnauthorized: false,
      ca: [fs.readFileSync(`${path}/kafka-ca.pem`, "utf-8")],
      key: fs.readFileSync(`${path}/kafka-private-key.pem`, "utf-8"),
      cert: fs.readFileSync(`${path}/kafka-certificate.crt`, "utf-8"),
    },
  });
}

export async function sendMessageToKafkaTopic(
  topicName: string,
  local: boolean = false,
  pathToMessage: string,
) {
  const client = await connectToKafka(local);
  console.log(
    `Sender melding fra sti ${pathToMessage} til topic ${chalk.greenBright(topicName)}`,
  );
  const message = await getFileAsJson(pathToMessage);
  const producer = client.producer({
    createPartitioner: Partitioners.DefaultPartitioner,
  });
  await producer.connect();
  await producer.send({
    topic: topicName,
    messages: [{ key: uuidv4(), value: JSON.stringify(message) }],
  });
  console.log(`Sendte melding til topic ${chalk.greenBright(topicName)}`);
  await producer.disconnect();
}

export async function listenToKafkaTopic(
  topicName: string,
  local: boolean = false,
  filter?: string,
  seekToOffsett?: number,
) {
  const client = await connectToKafka(local);
  console.log(`Lytter nå på topic ${chalk.greenBright(topicName)}`);
  const consumer = client.consumer({ groupId: os.hostname() });
  await consumer.connect();
  await consumer.subscribe({ topics: [topicName] });
  // Handle program termination
  const shutdown = async () => {
    console.log("Shutting down consumer...");
    await consumer.disconnect();
    process.exit(0);
  };

  process.on("SIGINT", shutdown);
  process.on("SIGTERM", shutdown);

  await consumer.run({
    eachMessage: async ({ topic, partition, message, heartbeat, pause }) => {
      const messageValue = message.value.toString();
      if (filter && messageValue.includes(filter)) {
        console.log({
          topic,
          offsett: message.offset,
          key: message.key.toString(),
          value: messageValue,
        });
      }
    },
  });
  if (seekToOffsett) {
    consumer.seek({
      offset: seekToOffsett.toString(),
      topic: topicName,
      partition: 0,
    });
  }
}

export async function listTopics(local: boolean = false) {
  const client = await connectToKafka(local);
  const admin = client.admin();

  const topics = await admin.listTopics();
  const bidragTopics = topics.filter((t) => t.startsWith("bidrag"));

  console.log(
    `Følgende topic ble funnet \n${bidragTopics.map((t) => chalk.greenBright(t)).join("\n")}`,
  );
  return;
}

export async function showTopicOffsets(
  topic: string,
  groupId?: string,
  local: boolean = false,
) {
  const client = await connectToKafka(local);
  const admin = client.admin();

  if (!groupId) {
    const offsets = await admin.fetchTopicOffsets(topic);

    console.log(`Følgende topic ble funnet \n${JSON.stringify(offsets)}`);
  } else {
    const offsets = await admin.fetchOffsets({ groupId, topics: [topic] });

    console.log(
      `Følgende topic ble funnet \n${JSON.stringify(offsets)} i gruppe ${groupId}`,
    );
  }

  await admin.disconnect();
}

export async function updateTopicOffset(
  topic: string,
  groupId?: string,
  offset: number = 0,
  partition: number = 0,
  local: boolean = false,
) {
  const client = await connectToKafka(local);
  const admin = client.admin();

  await admin.setOffsets({
    topic,
    groupId,
    partitions: [{ partition, offset: offset.toString() }],
  });

  console.log(
    `Offsett for groupId ${groupId} topic ${topic} ble satt til ${offset} i partisjon ${partition}`,
  );

  await admin.disconnect();
}
