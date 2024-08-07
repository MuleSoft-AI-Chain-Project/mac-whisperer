package com.mule.mulechain.helpers;
import java.util.Set;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class ModelNameProviderTTS implements ValueProvider {

	private static final Set<Value> VALUES_FOR = ValueBuilder.getValuesFor(
	"tts-1",
	"tts-1-hd"
	);

	@Override
	public Set<Value> resolve() throws ValueResolvingException {
		
		return VALUES_FOR;
	}

}