package engine;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Stores all the information required to execute and assess the result of a koan.
 */
public class Koan {
    private final Localizable<Class<?>> koanClass;
    public final String methodName;
    public final Class<?>[] methodParamTypes;
    public final KoanMethodCall[] calls;
    public final boolean usesConsole;

    public class KoanMethodCall {
        public static final List<Localizable<String>> NO_STD_IN_INPUTS = List.of();

        private final Object[] params;
        public final Assertion[] assertions;
        private final List<Localizable<String>> stdInInputs;
        public final Koan koan;

        public KoanMethodCall(Koan koan, Object[] params, Assertion[] assertions, List<Localizable<String>> stdInInputs) {
            this.koan = koan;
            this.params = params;
            this.assertions = assertions;
            this.stdInInputs = stdInInputs;
        }

        public KoanMethodCall withStdInInputs(List<Localizable<String>> inputs) {
            return new KoanMethodCall(koan, params, assertions, inputs);
        }
        
        public KoanMethodCall withAssertions(Assertion[] assertions) {
            return new KoanMethodCall(koan, params, assertions, stdInInputs);
        }

        @SuppressWarnings("rawtypes")
        public Object[] parameters(Locale locale) {
            return Arrays.stream(params)
                .map(p -> p instanceof Local ? ((Local)p).get(locale) : p)
                .toArray();
        }

        public String[] stdInInputs(Locale locale) {
            return stdInInputs.stream()
                .map(p -> p.get(locale))
                .toArray(String[]::new);
        }

        public int paramCount() {
            return params.length;
        }

        @Override
        public String toString() {
            String[] params = Arrays.stream(this.params)
                .map(p -> p == null ? "null": p.toString())
                .toArray(String[]::new);
            return String.format("%s(%s)", koan.methodName, String.join(", ", params));
        }
    }

    public Koan(Localizable<Class<?>> koanClass, String methodName, Class<?>... methodParamTypes) {
        this(koanClass, methodName, methodParamTypes, new KoanMethodCall[0], false);
    }

    private Koan(Localizable<Class<?>> koanClass, String methodName, Class<?>[] methodParamTypes, KoanMethodCall[] calls, boolean usesConsole) {
        this.koanClass = Objects.requireNonNull(koanClass, "koanClass must not be null");
        this.methodName = Objects.requireNonNull(methodName, "methodName must not be null");
        this.methodParamTypes = Objects.requireNonNull(methodParamTypes, "methodParamTypes must not be null");
        this.calls = Objects.requireNonNull(calls, "calls must not be null");
        this.usesConsole = usesConsole;
    }


    public Koan whenCallingWith(Object... params) {
        var newCalls = Arrays.copyOf(calls, calls.length + 1);
        
        newCalls[newCalls.length - 1] = new KoanMethodCall(this, params, new Assertion[0], KoanMethodCall.NO_STD_IN_INPUTS);
        return new Koan(
            koanClass,
            methodName,
            methodParamTypes,
            newCalls,
            usesConsole
        );
    }

    public Koan useConsole() {
        return new Koan(
            koanClass,
            methodName,
            methodParamTypes,
            calls,
            true
        );
    }

    public Koan whenCalling() {
        return new Koan(
            koanClass,
            methodName,
            methodParamTypes,
            new KoanMethodCall[] { new KoanMethodCall(this, new Object[0], new Assertion[0], KoanMethodCall.NO_STD_IN_INPUTS) },
            usesConsole
        );
    }

    public Koan withStdInInputs(List<Localizable<String>> inputs) {
        return withUpdatedCall(call -> {
            if (call.stdInInputs.size() > 0) {
                throw new IllegalArgumentException("Current call already have StdIn inputs");
            }
            return call.withStdInInputs(inputs);
        });
    }

    public Koan withStdInInputs(String... inputs) {
        return withStdInInputs(
            Arrays.stream(inputs)
                .map(input -> (Localizable<String>)new Global<String>(input))
                .toList()
        );
    }

    public Koan then(Assertion... assertions) {
        return withUpdatedCall(call -> {
            if (call.assertions.length > 0) {
                throw new IllegalArgumentException("Current call already have assertions");
            }
            return call.withAssertions(assertions);
        });
    }

    public String className(Locale locale) {
        return koanClass.get(locale).getSimpleName();
    }

    public Method method(Locale locale) throws NoSuchMethodException {
        return koanClass.get(locale).getMethod(methodName, methodParamTypes);
    }

    private Koan withUpdatedCall(Function<KoanMethodCall, KoanMethodCall> newCall) {
        if (calls.length == 0) {
            throw new IllegalArgumentException("No current call in Koan");
        }
        var currentCall = calls[calls.length - 1];
        var newCalls = calls.clone();
        newCalls[newCalls.length - 1] = newCall.apply(currentCall);
        return new Koan(koanClass, methodName, methodParamTypes, newCalls, usesConsole);
    }
}
