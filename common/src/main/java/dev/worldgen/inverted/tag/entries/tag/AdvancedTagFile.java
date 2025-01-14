package dev.worldgen.inverted.tag.entries.tag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.tags.TagEntry;

import java.util.List;

public record AdvancedTagFile(List<AdvancedTagEntry> entries, List<TagEntry> removals, boolean replace) {
    public static final Codec<AdvancedTagFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        AdvancedTagEntry.CODEC.listOf().fieldOf("values").forGetter(AdvancedTagFile::entries),
        TagEntry.CODEC.listOf().fieldOf("remove").orElse(List.of()).forGetter(AdvancedTagFile::removals),
        Codec.BOOL.optionalFieldOf("replace", false).forGetter(AdvancedTagFile::replace)
    ).apply(instance, AdvancedTagFile::new));
}

