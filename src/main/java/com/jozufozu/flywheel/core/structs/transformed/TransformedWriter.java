package com.jozufozu.flywheel.core.structs.transformed;

import com.jozufozu.flywheel.core.structs.ColoredLitWriter;
import com.jozufozu.flywheel.util.MatrixUtil;

public class TransformedWriter extends ColoredLitWriter<TransformedPart> {
	public static final TransformedWriter INSTANCE = new TransformedWriter();

	@Override
	public void write(final long ptr, final TransformedPart d) {
		super.write(ptr, d);
		MatrixUtil.writeUnsafe(d.model, ptr + 8);
		MatrixUtil.writeUnsafe(d.normal, ptr + 72);
	}

}