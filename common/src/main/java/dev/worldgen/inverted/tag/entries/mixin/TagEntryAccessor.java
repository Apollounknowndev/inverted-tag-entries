package dev.worldgen.inverted.tag.entries.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TagEntry.class)
public interface TagEntryAccessor {
    @Accessor("id")
    ResourceLocation id();

    @Accessor("tag")
    boolean tag();

    @Accessor("required")
    boolean required();
}
