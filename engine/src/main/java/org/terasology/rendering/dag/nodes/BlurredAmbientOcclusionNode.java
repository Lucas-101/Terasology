/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.dag.nodes;

import org.terasology.assets.ResourceUrn;
import org.terasology.config.Config;
import org.terasology.config.RenderingConfig;
import org.terasology.monitoring.PerformanceMonitor;
import org.terasology.registry.In;
import org.terasology.rendering.assets.material.Material;
import org.terasology.rendering.dag.ConditionDependentNode;
import org.terasology.rendering.dag.stateChanges.BindFBO;
import org.terasology.rendering.dag.stateChanges.EnableMaterial;
import org.terasology.rendering.dag.stateChanges.SetViewportToSizeOf;
import org.terasology.rendering.opengl.FBO;
import org.terasology.rendering.opengl.FBOConfig;
import static org.terasology.rendering.opengl.ScalingFactors.FULL_SCALE;
import org.terasology.rendering.opengl.fbms.DisplayResolutionDependentFBOs;
import org.terasology.rendering.world.WorldRenderer;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.terasology.rendering.opengl.OpenGLUtils.renderFullscreenQuad;

/**
 * TODO: Add diagram of this node
 */
public class BlurredAmbientOcclusionNode extends ConditionDependentNode {
    public static final ResourceUrn SSAO_BLURRED_FBO = new ResourceUrn("engine:ssaoBlurred");
    private static final ResourceUrn SSAO_FBO = new ResourceUrn("engine:ssao");
    private static final ResourceUrn SSAO_BLURRED_MATERIAL = new ResourceUrn("engine:prog.ssaoBlur");

    @In
    private DisplayResolutionDependentFBOs displayResolutionDependentFBOs;

    @In
    private WorldRenderer worldRenderer;

    @In
    private Config config;

    private Material ssaoBlurredMaterial;

    @Override
    public void initialise() {
        RenderingConfig renderingConfig = config.getRendering();
        renderingConfig.subscribe(RenderingConfig.SSAO, this);
        requiresCondition(renderingConfig::isSsao);

        addDesiredStateChange(new EnableMaterial(SSAO_BLURRED_MATERIAL.toString()));
        ssaoBlurredMaterial = getMaterial(SSAO_BLURRED_MATERIAL);

        requiresFBO(new FBOConfig(SSAO_FBO, FULL_SCALE, FBO.Type.DEFAULT), displayResolutionDependentFBOs);
        requiresFBO(new FBOConfig(SSAO_BLURRED_FBO, FULL_SCALE, FBO.Type.DEFAULT), displayResolutionDependentFBOs);
        addDesiredStateChange(new BindFBO(SSAO_BLURRED_FBO, displayResolutionDependentFBOs));
        addDesiredStateChange(new SetViewportToSizeOf(SSAO_BLURRED_FBO, displayResolutionDependentFBOs));
    }

    /**
     * If Ambient Occlusion is enabled in the render settings, this method generates and
     * stores the necessary images into their own FBOs. The stored images are eventually
     * combined with others.
     * <p>
     * For further information on Ambient Occlusion see: http://en.wikipedia.org/wiki/Ambient_occlusion
     */
    @Override
    public void process() {
        PerformanceMonitor.startActivity("rendering/blurredAmbientOcclusion");

        FBO ssaoFBO = displayResolutionDependentFBOs.get(SSAO_FBO);
        ssaoFBO.bindTexture(); // TODO: verify this is the only input

        FBO ssaoBlurredFBO = displayResolutionDependentFBOs.get(SSAO_BLURRED_FBO); // TODO: make this class a subscriber and handle changes
        ssaoBlurredMaterial.setFloat2("texelSize", 1.0f / ssaoBlurredFBO.width(), 1.0f / ssaoBlurredFBO.height(), true);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // TODO: verify this is necessary
        renderFullscreenQuad();

        PerformanceMonitor.endActivity();
    }
}
