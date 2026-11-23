package de.erl.client.utils.render.postprocess;

import de.erl.client.renderer.MeshRenderer;
import de.erl.client.renderer.ErlRenderPipelines;
import de.erl.client.systems.modules.Modules;
import de.erl.client.systems.modules.render.StorageESP;

public class StorageOutlineShader extends PostProcessShader {
    private static StorageESP storageESP;

    public StorageOutlineShader() {
        super(ErlRenderPipelines.POST_OUTLINE);
    }

    @Override
    protected boolean shouldDraw() {
        if (storageESP == null) storageESP = Modules.get().get(StorageESP.class);
        return storageESP.isShader();
    }

    @Override
    protected void setupPass(MeshRenderer renderer) {
        renderer.uniform("OutlineData", OutlineUniforms.write(
            storageESP.outlineWidth.get(),
            storageESP.fillOpacity.get() / 255.0f,
            storageESP.shapeMode.get().ordinal(),
            storageESP.glowMultiplier.get().floatValue()
        ));
    }
}
