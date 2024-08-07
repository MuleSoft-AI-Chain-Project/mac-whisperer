package com.mule.mulechain.helpers;
import java.util.Set;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class ModelNameProvider implements ValueProvider {

	private static final Set<Value> VALUES_FOR = ValueBuilder.getValuesFor(
	"whisper-1"
	);

	@Override
	public Set<Value> resolve() throws ValueResolvingException {
		
		return VALUES_FOR;
	}

}