package com.jozufozu.flywheel.core.source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.jozufozu.flywheel.core.source.error.ErrorBuilder;
import com.jozufozu.flywheel.core.source.error.ErrorReporter;
import com.jozufozu.flywheel.core.source.span.Span;

import net.minecraft.resources.ResourceLocation;

/**
 * A reference to a source file that might not be loaded when the owning object is created.
 *
 * <p>
 *     FileResolutions are used primarily while parsing import statements. {@link FileResolution#file} is initially
 *     null, but will be populated later on, after <em>all</em> SourceFiles are loaded (assuming
 *     {@link FileResolution#fileLoc} references an actual file).
 * </p>
 */
public class FileResolution {

	private static final Map<ResourceLocation, FileResolution> ALL = new HashMap<>();
	private static final Map<ResourceLocation, FileResolution> WEAK = new HashMap<>();
	private static boolean tooLate = false;

	/**
	 * Extra info about where this resolution is required. Includes shader Spans.
	 */
	private final List<Span> neededAt = new ArrayList<>();
	private final List<BiConsumer<ErrorReporter, SourceFile>> checks = new ArrayList<>();

	private final ResourceLocation fileLoc;
	private final boolean weak;

	private FileResolution(ResourceLocation fileLoc, boolean weak) {
		this.fileLoc = fileLoc;
		this.weak = weak;
	}

	public static FileResolution get(ResourceLocation file) {
		if (!tooLate) {
			return ALL.computeIfAbsent(file, loc -> new FileResolution(loc, false));
		} else {
			// Lock the map after resolution has run.
			FileResolution fileResolution = ALL.get(file);

			// ...so crash immediately if the file isn't found.
			if (fileResolution == null) {
				throw new ShaderLoadingException("could not find source for file: " + file);
			}

			return fileResolution;
		}
	}

	/**
	 * Weak resolutions don't persist through resource reloads.<p>
	 * This should be used inside parsing code.
	 *
	 * @param file The location of the file to resolve.
	 * @return A weak resolution for the given file.
	 */
	public static FileResolution weak(ResourceLocation file) {
		FileResolution fileResolution = ALL.get(file);

		if (fileResolution != null) {
			return fileResolution;
		}
		// never too late for weak resolutions.
		return WEAK.computeIfAbsent(file, loc -> new FileResolution(loc, true));
	}

	public static void checkAll(ErrorReporter errorReporter) {
		for (FileResolution resolution : ALL.values()) {
			resolution.runChecks(errorReporter);
		}
	}

	private void reportMissing(ErrorReporter errorReporter) {
		ErrorBuilder builder = errorReporter.error(String.format("could not find source for file %s", fileLoc));
		for (Span location : neededAt) {
			builder.pointAtFile(location.getSourceFile())
					.pointAt(location);
		}
	}

	private void runChecks(ErrorReporter errorReporter) {
		//		for (var check : checks) {
		//			check.accept(errorReporter, file);
		//		}
	}

	public ResourceLocation resourceLocation() {
		return fileLoc;
	}


	public boolean isWeak() {
		return weak;
	}

	/**
	 * Store the given span so this resolution can know all the places that reference the file.
	 *
	 * <p>
	 *     Used for error reporting.
	 * </p>
	 * @param span A span where this file is referenced.
	 */
	public FileResolution addSpan(Span span) {
		neededAt.add(span);
		return this;
	}

	public FileResolution validateWith(BiConsumer<ErrorReporter, SourceFile> check) {
		checks.add(check);
		return this;
	}

	@Override
	public String toString() {
		return "FileResolution[" + fileLoc + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		FileResolution that = (FileResolution) o;

		return fileLoc.equals(that.fileLoc);
	}

	@Override
	public int hashCode() {
		// FileResolutions are interned and therefore can be hashed based on object identity.
		// Overriding this to make it explicit.
		return System.identityHashCode(this);
	}
}
