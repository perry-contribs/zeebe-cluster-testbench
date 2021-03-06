package io.zeebe.clustertestbench.testdriver.api.serde;

import java.io.IOException;
import java.time.Duration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class DurationDeserializer extends StdDeserializer<Duration> {
	private static final long serialVersionUID = 1L;

	public DurationDeserializer() {
		this(null);
	}

	public DurationDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public Duration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return Duration.parse(p.getText());
	}

}
