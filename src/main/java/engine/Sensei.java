package engine;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static engine.Texts.*;

/**
 * The main engine class, executing the series of koans.
 */
public class Sensei {
    private final Locale locale;
    private final Printer consolePrinter;
    private Printer p = Printer.SILENT;
    private List<Koan> allKoans;

    public Sensei(Locale locale, List<List<Koan>> koanSeries) {
        this.locale = locale;
        this.consolePrinter = new ConsolePrinter(locale);
        this.allKoans = koanSeries
            .stream()
            .flatMap(kl -> kl.stream())
            .toList();
    }

    public void offerKoans() {
        for(int i = 0; i< allKoans.size(); i++) {
            if (!tryOffer(allKoans.get(i), i)) {
                return;
            }
        }

        consolePrinter.println();
        consolePrinter.println(MOUNTAINS_ARE_AGAIN_MERELY_MOUNTAINS);
        consolePrinter.println();
    }

    private boolean tryOffer(Koan koan, int successfulCount) {
        for(var test: koan.tests) {
            var success = tryOffer(test, successfulCount);
            if (!success) {
                return false;
            }
        }
        return true;
    }

    private boolean tryOffer(KoanTest test, int successfulCount) {
        // Execute silently the first time.
        // We do not want to display all the outputs of the successful koans to the student.
        p = Printer.SILENT;
        var succeeded = offer(test, successfulCount);

        if (!succeeded) {
            // If failed, execute verbosely the second time, in order to give feedback to the student.
            p = consolePrinter;
            return offer(test, successfulCount);
        }

        return true;
    }

    private boolean offer(KoanTest test, int successfulCount) {
        var koan = test.koan;
        observe(koan);
        encourage();
        
        var success = false;
        try {
            success = executeCall(test);
        } catch (IllegalAccessException iae) {
            // Special case: since the executeCall() method did not complete, the console conclusion was not displayed.
            concludeConsole(koan);
            p.println(Color.red(EXPECTED_METHOD_TO_BE_PUBLIC), koan.methodName);
        } catch (IllegalArgumentException iae) {
            // Special case: since the executeCall() method did not complete, the console conclusion was not displayed.
            concludeConsole(koan);
            // Would be a bug in the Koan instances, since we are ensuring for the method with the right parameters.
            p.println(Color.red(THE_METHOD_APPEARS_TO_PRODUCE_AN_ERROR), koan.methodName, iae.getMessage());
        } catch (InvocationTargetException ite) {
            // Special case: since the executeCall() method did not complete, the console conclusion was not displayed.
            concludeConsole(koan);
            p.println(Color.red(THE_METHOD_APPEARS_TO_PRODUCE_AN_ERROR), koan.methodName, ite.getCause().getMessage());
        } catch (NoStaticMethodException nsme) {
            // Special case: since the executeCall() method did not complete, the console conclusion was not displayed.
            concludeConsole(koan);
            p.println(Color.red(EXPECTED_METHOD_TO_BE_STATIC), koan.methodName, koan.exerciseClassName(locale).replace(".", "/"));
        } catch (NoDynamicMethodException ndme) {
            // Special case: since the executeCall() method did not complete, the console conclusion was not displayed.
            concludeConsole(koan);
            p.println(Color.red(EXPECTED_METHOD_TO_NOT_BE_STATIC), koan.methodName, koan.exerciseClassName(locale));
        } catch (NoSuchConstructorException nsce) {
            // Special case: since the executeCall() method did not complete, the console conclusion was not displayed.
            concludeConsole(koan);
            displayConstructorNotFound(koan);
        } catch (NoSuchMethodException mnfe) {
            // Special case: since the executeCall() method did not complete, the console conclusion was not displayed.
            concludeConsole(koan);
            displayMethodNotFound(koan);
        } catch (ClassNotFoundException cnfe) {
            // Special case: since the executeCall() method did not complete, the console conclusion was not displayed.
            concludeConsole(koan);
            p.println(Color.red(EXPECTED_TO_FIND_A_CLASS_IN_THE_PACKAGE), koan.exerciseClassName.get(), koan.exerciseClassPackage.get());
        } catch (InstantiationException ie) {
            // Special case: since the executeCall() method did not complete, the console conclusion was not displayed.
            concludeConsole(koan);
            // Would be a bug in the Koan instances, since we are ensuring for the method with the right parameters.
            p.println(Color.red(THE_CONSTRUCTOR_APPEARS_TO_PRODUCE_AN_ERROR), koan.exerciseClassName.get());
        }

        offerToMeditate(koan);
        showProgress(successfulCount);

        return success;
    }

    private boolean executeCall(KoanTest test) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, InstantiationException {

        test.setupRandomForKoan();
        final var targetMethod = test.resolveTargetMethod(locale);
        final var asserted = test.koan.executeAssertions(p, targetMethod);
        if (!asserted) {
            return false;
        }

        introduceConsole(targetMethod);

        final var interceptionResult = StdStreamsInterceptor.capture(
            p == Printer.SILENT,
            targetMethod::invoke,
            test.stdInInputs(locale)
        );

        final var result = new KoanResult(
            locale,
            targetMethod,
            interceptionResult.stdOutLines,
            interceptionResult.stdInLines,
            interceptionResult.returnValue
        );

        concludeConsole(test.koan);

        return result.executeAssertions(p);
    }

    private void introduceConsole(KoanTargetMethod targetMethod) throws IllegalAccessException, ClassNotFoundException, InstantiationException, InvocationTargetException {
        if (targetMethod.koanTest.koan.usesConsole) {
            if (targetMethod.koanTest.koan.showStdInInputs) {
                p.println();
                p.println(THE_MASTER_SENSED_AN_HARMONY_BREACH_WHEN_ANSWERING, Helpers.formatSequence(targetMethod.koanTest.stdInInputs(locale), AND.get(locale)));
                p.println();
            }

            p.println();
            p.println(CONSOLE, targetMethod);
            p.println("---------");
            p.println();
        }
    }

    private void concludeConsole(Koan koan) {
        if (koan.usesConsole) {
            p.println();
            p.println("---------");
            p.println();
        }
    }

    private void encourage() {
        p.println();
        p.println(THE_MASTER_SAYS);
        p.println(Color.cyan(YOU_HAVE_NOT_REACHED_ENLIGHTMENT));
        p.println();
        p.println("---------");
        p.println();
        p.println(THE_ANSWERS_YOU_SEEK);
        p.println();
    }

    private void observe(Koan koan) {
        p.println();
        p.println(THINKING, koan.koanClassName(locale));
        p.println(Color.red(HAS_DAMAGED_YOUR_KARMA), koan.methodName);
    }

    private void offerToMeditate(Koan koan) {
        p.println();
        p.println(
            PLEASE_MEDITATE_ON, 
            koan.methodName,
            koan.koanClassName(locale)
        );
        p.println();
    }

    private void showProgress(int successfulCount) {
        if (successfulCount == 0) {
            // Let's not be discouraging...
            return;
        }

        final var bar = String.format(
            "%s%s%s",
            Color.green(repeat(".", successfulCount)),
            Color.red("X"),
            Color.cyan(repeat("_", allKoans.size() - successfulCount - 1)));
        p.println(YOUR_PROGRESS_THUS_FAR, bar, successfulCount, allKoans.size());
        p.println();
    }

    private static String repeat(String s, int times) {
        return new String(new char[times]).replace("\0", s);
    }
    
    private void displayMethodNotFound(Koan koan) {
        if (koan.methodParamTypes.length == 0) {
            p.println(
                Color.red(EXPECTED_TO_FIND_MEHOD_NO_PARAMS),
                koan.methodName,
                koan.exerciseClassName(locale).replace(".", "/")
            );
        } else if (koan.methodParamTypes.length == 1) {
            p.println(
                Color.red(EXPECTED_TO_FIND_MEHOD_ONE_PARAM),
                koan.methodName,
                koan.exerciseClassName(locale).replace(".", "/"),
                koan.methodParamTypes[0].getSimpleName()
            );
        } else {
            final var expectedParams = Arrays
                .stream(koan.methodParamTypes)
                .map(type -> "'" + type.getSimpleName() + "'")
                .toArray(String[]::new);
            p.println(
                Color.red(EXPECTED_TO_FIND_MEHOD_MANY_PARAMS),
                koan.methodName,
                koan.exerciseClassName(locale).replace(".", "/"),
                Helpers.formatSequence(expectedParams, AND.get(locale))
            );
        }
    }
    
    private void displayConstructorNotFound(Koan koan) {
        if (koan.constructorParamTypes.length == 0) {
            p.println(
                Color.red(EXPECTED_TO_FIND_CONSTRUCTOR_NO_PARAMS),
                koan.exerciseClassName(locale)
            );
        } else if (koan.constructorParamTypes.length == 1) {
            p.println(
                Color.red(EXPECTED_TO_FIND_CONSTRUCTOR_ONE_PARAM),
                koan.exerciseClassName(locale),
                koan.constructorParamTypes[0]
            );
        } else {
            final var expectedParams = Arrays
                .stream(koan.constructorParamTypes)
                .map(type -> "'" + type + "'")
                .toArray(String[]::new);
            p.println(
                Color.red(EXPECTED_TO_FIND_CONSTRUCTOR_MANY_PARAMS),
                koan.exerciseClassName(locale),
                Helpers.formatSequence(expectedParams, AND.get(locale))
            );
        }
    }
}
