package mycraft.yuyears.neofavoriteitems.client.ui.binding;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NfiValueBinding<T> {
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final Supplier<T> defaultSupplier;
    private final Function<T, NfiValidationResult> validator;
    private final Runnable changeListener;

    public NfiValueBinding(
        Supplier<T> getter,
        Consumer<T> setter,
        Supplier<T> defaultSupplier,
        Function<T, NfiValidationResult> validator,
        Runnable changeListener
    ) {
        this.getter = Objects.requireNonNull(getter);
        this.setter = Objects.requireNonNull(setter);
        this.defaultSupplier = Objects.requireNonNull(defaultSupplier);
        this.validator = Objects.requireNonNull(validator);
        this.changeListener = Objects.requireNonNull(changeListener);
    }

    public static <T> NfiValueBinding<T> unchecked(
        Supplier<T> getter,
        Consumer<T> setter,
        Supplier<T> defaultSupplier,
        Runnable changeListener
    ) {
        return new NfiValueBinding<>(getter, setter, defaultSupplier, value -> NfiValidationResult.ok(), changeListener);
    }

    public T get() {
        return getter.get();
    }

    public NfiValidationResult set(T value) {
        NfiValidationResult result = validator.apply(value);
        if (result.valid() && !Objects.equals(get(), value)) {
            setter.accept(value);
            changeListener.run();
        }
        return result;
    }

    public NfiValidationResult validate(T value) {
        return validator.apply(value);
    }

    public NfiValidationResult reset() {
        return set(defaultSupplier.get());
    }

    public T defaultValue() {
        return defaultSupplier.get();
    }

    public boolean isDefault() {
        return Objects.equals(get(), defaultSupplier.get());
    }
}
