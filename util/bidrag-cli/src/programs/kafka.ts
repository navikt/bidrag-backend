import { Command } from "commander";
import chalk from "chalk";
import {
  listenToKafkaTopic,
  listTopics,
  showTopicOffsets,
  updateTopicOffset,
} from "../services/KafkaService";
import { sendMessageToKafkaTopic } from "../services/KafkaService";

export default function init(program: Command) {
  const kafkaProgram = program
    .command("kafka")
    .description("Lytt eller send melding til kafka topic");
  kafkaProgram
    .command("consume")
    .description("Lytt til kafka topic")
    .option("--local", "Koble til lokal kafka")
    .option("--filter <string>", "Filtrere på at meldinger inneholder tekst")
    .option(
      "--offsett <string>",
      "Start lytting fra offsett. Ellers begynner den fra siste melding",
    )
    .argument("<string>", "Kafka topic")
    .action(async (topic: any, options: any) => {
      console.log(`Starter Kafka lytteren for topic ${chalk.red(topic)}`);
      await listenToKafkaTopic(
        topic,
        options?.local,
        options?.filter,
        options.offsett,
      );
    });

  kafkaProgram
    .command("produce")
    .description("Send melding til kafka topic")
    .option("--local", "Koble til lokal kafka")
    .option("--message <string>", "Lokasjon på melding som skal sendes (JSON)")
    .argument("<string>", "Kafka topic")
    .action(async (topic: any, options: any) => {
      await sendMessageToKafkaTopic(topic, options?.local, options.message);
    });

  kafkaProgram
    .command("list")
    .description("Vis alle tilgjengelige topics")
    .option("--local", "Koble til lokal kafka")
    .action(async (options: any) => {
      await listTopics(options?.local);
    });

  kafkaProgram
    .command("offsets")
    .description("Vis offsets for topic")
    .option("--local", "Koble til lokal kafka")
    .option("-g, --groupId <string>", "Sjekk for groupId")
    .argument("<string>", "Kafka topic")
    .action(async (topic: any, options: any) => {
      console.log(options);
      await showTopicOffsets(topic, options.groupId, options?.local);
    });

  kafkaProgram
    .command("set-offset")
    .description("Sett offsets for topic")
    .option("--local", "Koble til lokal kafka")
    .option("-g, --groupId <string>", "Sjekk for groupId")
    .option("-o, --offset <number>", "Sett offset")
    .argument("<string>", "Kafka topic")
    .action(async (topic: any, options: any) => {
      await updateTopicOffset(
        topic,
        options.groupId,
        options.offset,
        0,
        options?.local,
      );
    });
}
