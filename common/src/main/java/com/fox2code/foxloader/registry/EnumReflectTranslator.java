package com.fox2code.foxloader.registry;

import com.fox2code.foxloader.loader.ModLoader;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.Function;

/**
 * Map an enum to the static field of a class.
 * <p>
 * Enum implementing {@link ReflectEnum} can define custom field names.
 */
public final class EnumReflectTranslator<E extends Enum<E>, T> implements Iterable<T> {
    private static final boolean PREFILL_CACHE = ModLoader.PREFILL_CACHE;
    private final Function<E, T> provider;
    private final EnumMap<E, T> cache;
    private final Class<E> enumType;
    private final Class<T> type;
    private final Class<?> target;
    private final T defaultElement;

    public EnumReflectTranslator(Class<E> e, Class<T> type) {
        this(e, type, type, null);
    }

    public EnumReflectTranslator(Class<E> e, Class<T> type, Class<?> target) {
        this(e, type, target, null);
    }

    public EnumReflectTranslator(Class<E> e, Class<T> type, Class<?> target, E defaultElement) {
        this.provider = this::provideElement;
        this.cache = new EnumMap<>(e);
        this.enumType = e;
        this.type = type;
        this.target = target;
        this.defaultElement = defaultElement == null ?
                null : translate(defaultElement);
        if (PREFILL_CACHE) {
            fillCache();
        }
    }

    private T provideElement(E e) {
        ReflectiveOperationException lastError = null;
        String[] reflectedNames;
        if (e instanceof ReflectEnum) {
            reflectedNames = ((ReflectEnum) e).getReflectNames();
        } else {
            reflectedNames = new String[]{e.name(),
                    e.name().toLowerCase(Locale.ROOT)};
        }
        for (String field : reflectedNames) {
            try {
                return this.type.cast(this.target.getDeclaredField(field).get(null));
            } catch (ReflectiveOperationException ex) {
                lastError = ex;
            }
        }
        if (this.defaultElement == null) {
            throw new RuntimeException("Failed to get element " +
                    e.name() + " on " + this.target.getName(), lastError);
        } else {
            return this.defaultElement;
        }
    }

    public T translate(E e) {
        return this.cache.computeIfAbsent(e, this.provider);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        final E[] elements = this.enumType.getEnumConstants();
        if (elements.length == this.cache.size()) {
            return this.cache.values().iterator();
        }
        return new Iterator<T>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < elements.length;
            }

            @Override
            public T next() {
                return translate(elements[index++]);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public <I> EnumReflectTranslator<E, ? extends I> castTypeTo(Class<I> type) {
        if (!type.isAssignableFrom(this.type)) {
            throw new ClassCastException("Cannot cast " + this.type.getName() + " to " + type.getName());
        }
        return (EnumReflectTranslator<E, ? extends I>) this;
    }

    public void fillCache() {
        final E[] elements = this.enumType.getEnumConstants();
        if (elements.length != this.cache.size()) {
            for (E e : elements) {
                this.translate(e);
            }
        }
    }

    public interface ReflectEnum {
        String[] getReflectNames();
    }
}