package com.jozufozu.flywheel.glsl.parse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jozufozu.flywheel.glsl.span.Span;

public class ShaderStruct {

	// https://regexr.com/61rpe
	public static final Pattern PATTERN = Pattern.compile("struct\\s+([\\w_]*)\\s*\\{(.*?)}\\s*([\\w_]*)?\\s*;\\s", Pattern.DOTALL);

	public final Span name;
	public final Span body;
	public final Span self;
	public final Span variableName;

	private final ImmutableList<StructField> fields;
	private final ImmutableMap<String, Span> fields2Types;

	public ShaderStruct(Span self, Span name, Span body, Span variableName) {
		this.self = self;
		this.name = name;
		this.body = body;
		this.variableName = variableName;
		this.fields = parseFields();
		this.fields2Types = createTypeLookup();
	}

	public Span getName() {
		return name;
	}

	public Span getBody() {
		return body;
	}

	public ImmutableList<StructField> getFields() {
		return fields;
	}

	private ImmutableMap<String, Span> createTypeLookup() {
		ImmutableMap.Builder<String, Span> lookup = ImmutableMap.builder();
		for (StructField field : fields) {
			lookup.put(field.name.get(), field.type);
		}

		return lookup.build();
	}

	private ImmutableList<StructField> parseFields() {
		Matcher matcher = StructField.fieldPattern.matcher(body);

		ImmutableList.Builder<StructField> fields = ImmutableList.builder();

		while (matcher.find()) {
			Span field = Span.fromMatcher(body, matcher);
			Span type = Span.fromMatcher(body, matcher, 1);
			Span name = Span.fromMatcher(body, matcher, 2);

			fields.add(new StructField(field, type, name));
		}

		return fields.build();
	}

	@Override
	public String toString() {
		return "struct " + name;
	}
}