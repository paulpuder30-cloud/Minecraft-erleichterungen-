package de.erl.client.utils.render.postprocess;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import de.erl.client.mixininterface.ILevelRenderer;
import net.minecraft.world.entity.Entity;

import static de.erl.client.ErlClient.mc;

public abstract class EntityShader extends PostProcessShader {
    protected EntityShader(RenderPipeline pipeline) {
        super(pipeline);
    }

    public abstract boolean shouldDraw(Entity entity);

    @Override
    protected void preDraw() {
        ((ILevelRenderer) mc.levelRenderer).erl$pushEntityOutlineFramebuffer(framebuffer);
    }

    @Override
    protected void postDraw() {
        ((ILevelRenderer) mc.levelRenderer).erl$popEntityOutlineFramebuffer();
    }

    public void submitVertices() {
    }
}
