package no.sysco.middleware.talk.akka.showcase;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.ActorMaterializer;
import akka.stream.alpakka.file.javadsl.FileTailSource;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import scala.concurrent.duration.FiniteDuration;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class ShowcaseMain {

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("showcase");
        final ActorMaterializer mat = ActorMaterializer.create(system);

        final Source<Tweet, NotUsed> singleSource = Source.single(new Tweet("some rt", "jeqo"));
        final Source<Tweet, NotUsed> seqSource =
                Source.from(
                        Arrays.asList(
                                new Tweet("some rt", "jeqo"),
                                new Tweet("other rt", "jeqo89", "java", "twitter", "kafka")));


        final FileSystem fs = FileSystems.getDefault();
        final FiniteDuration pollingInterval = FiniteDuration.create(250, TimeUnit.MILLISECONDS);

        final Source<String, NotUsed> linesSource = FileTailSource.createLines(fs.getPath("./samples/tweets.csv"), 1_000, pollingInterval);

        final Source<Tweet, NotUsed> tweetsFromFiles =
                linesSource.map(line -> {
                    String[] parts = line.split(",");
                    return new Tweet(parts[0], parts[1], Arrays.copyOfRange(parts, 2, parts.length));
                });

        final Flow<Tweet, Tweet, NotUsed> filter =
                Flow.<Tweet>create()
                        .filter(tweet -> tweet.hashtags.length > 0);

        final Sink<Tweet, CompletionStage<Done>> printSink = Sink.foreach(System.out::println);

        ProducerSettings<String, String> producerSettings =
                ProducerSettings.create(system, new StringSerializer(), new StringSerializer())
                        .withBootstrapServers("localhost:29092");
        Sink<ProducerRecord<String, String>, CompletionStage<Done>> kafkaTopicSink =
                Producer.plainSink(producerSettings);

        Sink<Tweet, NotUsed> kafkaFlow = Flow.<Tweet>create()
                .map(tweet -> new ProducerRecord<>("tweets", tweet.username, tweet.toString()))
                .to(kafkaTopicSink);

        RunnableGraph<NotUsed> runnableGraph =
//                singleSource
//                seqSource
                tweetsFromFiles
                        .via(filter)
                        .to(printSink);


        tweetsFromFiles
                .via(filter)
                .to(kafkaFlow)
                .run(mat);

//        runnableGraph.run(mat);
    }


    public static class Tweet {
        final String text;
        final String username;
        final String[] hashtags;

        public Tweet(String text, String username, String... hashtags) {
            this.text = text;
            this.username = username;
            this.hashtags = hashtags;
        }

        @Override
        public String toString() {
            return "Tweet{" +
                    "text='" + text + '\'' +
                    ", username='" + username + '\'' +
                    ", hashtags=" + Arrays.toString(hashtags) +
                    '}';
        }
    }
}
