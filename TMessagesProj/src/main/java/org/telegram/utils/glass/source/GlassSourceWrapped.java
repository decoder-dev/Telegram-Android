package org.telegram.utils.glass.source;

import android.graphics.Canvas;

import androidx.annotation.NonNull;


public final class GlassSourceWrapped implements GlassSource {
    private GlassSource sourceInternal;
    private long sourceGenerationId;
    private long generationId;

    public GlassSource getSource() {
        return sourceInternal;
    }

    public void setSource(GlassSource sourceInternal) {
        if (this.sourceInternal != sourceInternal) {
            this.sourceInternal = sourceInternal;
            sourceGenerationId = sourceInternal == null ? 0 : sourceInternal.getGenerationId();
            generationId++;
        }
    }

    @Override
    public long getGenerationId() {
        final long sourceGeneration = sourceInternal == null ? 0 : sourceInternal.getGenerationId();
        if (sourceGenerationId != sourceGeneration) {
            sourceGenerationId = sourceGeneration;
            generationId++;
        }
        return generationId;
    }

    @Override
    public void draw(@NonNull Canvas canvas, float left, float top, float right, float bottom) {
        if (sourceInternal != null) {
            sourceInternal.draw(canvas, left, top, right, bottom);
        }
    }
}
