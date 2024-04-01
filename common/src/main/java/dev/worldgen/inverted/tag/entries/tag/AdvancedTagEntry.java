package dev.worldgen.inverted.tag.entries.tag;

import dev.worldgen.inverted.tag.entries.mixin.TagEntryAccessor;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public class AdvancedTagEntry extends TagEntry {
    public static final Codec<EntryId> TAG_ENTRY_ID = Codec.STRING.comapFlatMap(AdvancedTagEntry::parseEntryId, EntryId::asString);
    public static final Codec<AdvancedTagEntry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        TAG_ENTRY_ID.fieldOf("id").forGetter(AdvancedTagEntry::getEntryIdForCodec),
        Codec.BOOL.optionalFieldOf("required", true).forGetter(AdvancedTagEntry::entryRequired)
    ).apply(instance, AdvancedTagEntry::new));

    public static final Codec<AdvancedTagEntry> CODEC = Codec.either(TAG_ENTRY_ID, ENTRY_CODEC).xmap(either ->
        either.map(id -> new AdvancedTagEntry(id, true), Function.identity()),
        entry -> entry.entryRequired() && !entry.entryInverted() ? Either.left(entry.getEntryIdForCodec()) : Either.right(entry)
    );
    private final boolean invert;
    private final TagEntryAccessor accessor;

    public AdvancedTagEntry(TagEntryAccessor entry, boolean invert) {
        super(entry.id(), entry.tag(), entry.required());
        this.invert = invert;
        this.accessor = entry;
    }

    protected AdvancedTagEntry(EntryId id, boolean required) {
        super(id.id(), id.tag(), required);
        this.invert = id.invert();
        this.accessor = (TagEntryAccessor)this;
    }

    public EntryId getEntryIdForCodec() {
        return new EntryId(this.accessor.id(), this.accessor.tag(), this.invert);
    }

    public boolean entryRequired() {
        return this.accessor.required();
    }

    public boolean entryInverted() {
        return this.invert;
    }

    public <T> boolean resolve(Lookup<T> valueGetter, Consumer<T> addConsumer, Consumer<T> removeConsumer) {
        Consumer<T> resolveMethod = this.invert ? removeConsumer : addConsumer;
        if (this.accessor.tag()) {
            Collection<T> collection = valueGetter.tag(this.accessor.id());
            if (collection == null) {
                return (!this.accessor.required()) || this.invert;
            }

            collection.forEach(resolveMethod);
        } else {
            T object = valueGetter.element(this.accessor.id());
            if (object == null) {
                return (!this.accessor.required()) || this.invert;
            }

            resolveMethod.accept(object);
        }

        return true;
    }

    private static DataResult<EntryId> parseEntryId(String entryId) {
        boolean invert;
        boolean tag;

        if (entryId.startsWith("!")) {
            invert = true;
            entryId = entryId.substring(1);
        } else {
            invert = false;
        }

        if (entryId.startsWith("#")) {
            tag = true;
            entryId = entryId.substring(1);
        } else {
            tag = false;
        }

        return ResourceLocation.read(entryId).map(id -> new EntryId(id, tag, invert));
    }


    public record EntryId(ResourceLocation id, boolean tag, boolean invert) {
        @Override
        public String toString() {
            return this.asString();
        }

        private String asString() {
            StringBuilder prefix = new StringBuilder();
            if (this.invert) prefix.append("!");
            if (this.tag) prefix.append("#");
            return prefix.toString()+this.id;
        }
    }
}
