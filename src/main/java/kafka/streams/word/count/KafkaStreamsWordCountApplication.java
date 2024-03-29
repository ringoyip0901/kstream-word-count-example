/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka.streams.word.count;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Function;

import org.apache.hadoop.mapreduce.Reducer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Serialized;
import org.apache.kafka.streams.kstream.TimeWindows;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import nl.vpro.util.Windowed;

@SpringBootApplication
public class KafkaStreamsWordCountApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaStreamsWordCountApplication.class, args);
	}

	public static class WordCountProcessorApplication {

		public static final String INPUT_TOPIC = "input";
		public static final String OUTPUT_TOPIC = "output";
		public static final int WINDOW_SIZE_MS = 30000;

		@Bean
		public Function<KStream<Bytes, String>, KStream<Bytes, WordCount>> process() {
			return (input) -> {

				// aggregate the kstream records with KTable
				KTable<String, Long> table = input.flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
						.map((key, value) -> new KeyValue<>(value, value))
						.groupByKey(Grouped.with(Serdes.String(), Serdes.String())).count(Materialized.as("WordCounts-1"));

				// emit the results as a kstream
				KStream<Bytes, WordCount> result = table.toStream()
						.map((key, value) -> new KeyValue<>(null, new WordCount(key, value, null, null)));

				return result;
			};
			// return input -> input.flatMapValues(value ->
			// Arrays.asList(value.toLowerCase().split("\\W+")))
			// .map((key, value) -> new KeyValue<>(value,
			// value)).groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
			// .windowedBy(TimeWindows.of(Duration.ofMillis(WINDOW_SIZE_MS))).count(Materialized.as("WordCounts-1"))
			// .toStream().map((key, value) -> new KeyValue<>(null,
			// new WordCount(key.key(), value, new Date(key.window().start()), new
			// Date(key.window().end()))));
		}
	}

	static class AddNumbers {

		private long sum;

		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer("WordCounts{");
			sb.append("sum='").append(sum).append('\'');
			sb.append('}');
			return sb.toString();
		}

		AddNumbers(long sum) {
			this.sum = sum;
		}
	}

	static class WordCount {

		private String word;

		private long count;

		private Date start;

		private Date end;

		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer("WordCount{");
			sb.append("word='").append(word).append('\'');
			sb.append(", count=").append(count);
			sb.append(", start=").append(start);
			sb.append(", end=").append(end);
			sb.append('}');
			return sb.toString();
		}

		WordCount(String word, long count, Date start, Date end) {
			this.word = word;
			this.count = count;
			this.start = start;
			this.end = end;
		}

		public String getWord() {
			return word;
		}

		public void setWord(String word) {
			this.word = word;
		}

		public long getCount() {
			return count;
		}

		public void setCount(long count) {
			this.count = count;
		}

		public Date getStart() {
			return start;
		}

		public void setStart(Date start) {
			this.start = start;
		}

		public Date getEnd() {
			return end;
		}

		public void setEnd(Date end) {
			this.end = end;
		}
	}
}
