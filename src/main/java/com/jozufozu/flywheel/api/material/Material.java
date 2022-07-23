package com.jozufozu.flywheel.api.material;

import com.jozufozu.flywheel.core.source.FileResolution;
import com.jozufozu.flywheel.core.source.SourceFile;

import net.minecraft.client.renderer.RenderType;

public record Material(RenderType renderType, FileResolution vertexShader, FileResolution fragmentShader) {

	public SourceFile getVertexShader() {
		return vertexShader.getFile();
	}

	public SourceFile getFragmentShader() {
		return fragmentShader.getFile();
	}
}
