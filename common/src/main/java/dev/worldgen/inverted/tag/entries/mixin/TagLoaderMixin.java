package dev.worldgen.inverted.tag.entries.mixin;

import dev.worldgen.inverted.tag.entries.InvertedTagEntries;
import dev.worldgen.inverted.tag.entries.tag.AdvancedTagEntry;
import dev.worldgen.inverted.tag.entries.tag.AdvancedTagFile;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

@Mixin(TagLoader.class)
public class TagLoaderMixin {

    /**
     * Don't run default tag logic, we want AdvancedTagEntry instances
     * Redirect is required so the game doesn't throw when it sees an exclamation point in a tag entry
     */
    @Redirect(
        method = "load",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/serialization/Codec;parse(Lcom/mojang/serialization/Dynamic;)Lcom/mojang/serialization/DataResult;"
        )
    )
    private DataResult<TagFile> init(Codec<TagFile> codec, Dynamic<JsonElement> dynamic) {
        return DataResult.success(new TagFile(List.of(), false));
    }

    /**
     * Once the game has run default tag logic, apply our logic to get {@link AdvancedTagEntry} instances
     */
    @Inject(
        method = "load",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/tags/TagFile;entries()Ljava/util/List;",
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void snapshot$applyAdvancedTagLogic(ResourceManager resourceManager, CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir, Map<ResourceLocation, List<TagLoader.EntryWithSource>> map, FileToIdConverter resourceFinder, Iterator<Map.Entry<ResourceLocation, List<Resource>>> var4, Map.Entry<ResourceLocation, List<Resource>> entry, ResourceLocation identifier, ResourceLocation identifier2, Iterator<List<Resource>> var8, Resource resource, Reader reader, JsonElement jsonElement, List<TagLoader.EntryWithSource> list, TagFile tagFile, String string) {
        AdvancedTagFile advancedTagFile = AdvancedTagFile.CODEC.parse(new Dynamic<>(JsonOps.INSTANCE, jsonElement)).getOrThrow(false, InvertedTagEntries.LOGGER::error);
        if (advancedTagFile.replace()) {
            list.clear();
        }
        advancedTagFile.entries().forEach(advancedEntry -> list.add(new TagLoader.EntryWithSource(advancedEntry, string)));

        advancedTagFile.removals().forEach(tagEntry -> list.add(new TagLoader.EntryWithSource(new AdvancedTagEntry((TagEntryAccessor)tagEntry, true), string)));
    }

    /**
     * Override default tag resolving logic to support removing tag entries
     */
    @Inject(
        method = "build(Lnet/minecraft/tags/TagEntry$Lookup;Ljava/util/List;)Lcom/mojang/datafixers/util/Either;",
        at = @At("HEAD"),
        cancellable = true
    )
    private <T> void snapshot$resolveAdvancedTagLogic(TagEntry.Lookup<T> valueGetter, List<TagLoader.EntryWithSource> entries, CallbackInfoReturnable<Either<Collection<TagLoader.EntryWithSource>, Collection<T>>> cir) {
        Set<T> set = new HashSet<>();
        ArrayList<TagLoader.EntryWithSource> brokenEntries = new ArrayList<>();
        for (TagLoader.EntryWithSource trackedEntry : entries) {
            // No reason this should ever be false? I think?
            if (trackedEntry.entry() instanceof AdvancedTagEntry entry) {
                if (entry.resolve(valueGetter, set::add, set::remove)) continue;
                brokenEntries.add(trackedEntry);
            }
        }
        if (!brokenEntries.isEmpty()) {
            InvertedTagEntries.LOGGER.error("Some tag entries could not be loaded: {}", brokenEntries.stream().map(TagLoader.EntryWithSource::toString).collect(Collectors.joining(", ")));
        }
        cir.setReturnValue(Either.right(set));
    }
}
