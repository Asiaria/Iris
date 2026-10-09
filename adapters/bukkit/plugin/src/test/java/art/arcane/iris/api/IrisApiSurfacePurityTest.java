package art.arcane.iris.api;

import art.arcane.iris.api.terrain.IrisTerrainService;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.GenericArrayType;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IrisApiSurfacePurityTest {
    private static final String API_PACKAGE = "art.arcane.iris.api";
    private static final List<String> ALLOWED_PREFIXES = List.of(
            "java.",
            "javax.",
            "org.bukkit.",
            API_PACKAGE + "."
    );

    @Test
    public void everyApiTypeIsReachableWithoutIris() throws IOException, URISyntaxException, ClassNotFoundException {
        List<Class<?>> apiTypes = apiTypes();
        assertTrue("the api package must contain types", apiTypes.size() >= 11);

        Set<String> violations = new LinkedHashSet<>();
        for (Class<?> apiType : apiTypes) {
            collect(apiType.getGenericSuperclass(), violations, apiType);
            for (Type implemented : apiType.getGenericInterfaces()) {
                collect(implemented, violations, apiType);
            }
            for (Method method : apiType.getDeclaredMethods()) {
                if (!isExported(method.getModifiers())) {
                    continue;
                }
                collect(method.getGenericReturnType(), violations, apiType);
                for (Type parameter : method.getGenericParameterTypes()) {
                    collect(parameter, violations, apiType);
                }
                for (Type thrown : method.getGenericExceptionTypes()) {
                    collect(thrown, violations, apiType);
                }
            }
            for (Constructor<?> constructor : apiType.getDeclaredConstructors()) {
                if (!isExported(constructor.getModifiers())) {
                    continue;
                }
                for (Type parameter : constructor.getGenericParameterTypes()) {
                    collect(parameter, violations, apiType);
                }
            }
            for (Field field : apiType.getDeclaredFields()) {
                if (!isExported(field.getModifiers())) {
                    continue;
                }
                collect(field.getGenericType(), violations, apiType);
            }
        }

        assertEquals("Iris API types must only expose java, bukkit and Iris API types: " + violations,
                Set.of(), violations);
    }

    @Test
    public void theTerrainServiceIsPartOfTheScannedSurface() throws IOException, URISyntaxException, ClassNotFoundException {
        assertTrue(apiTypes().contains(IrisTerrainService.class));
    }

    private static boolean isExported(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    private static void collect(Type type, Set<String> violations, Class<?> owner) {
        if (type == null) {
            return;
        }

        switch (type) {
            case Class<?> raw -> {
                Class<?> component = raw;
                while (component.isArray()) {
                    component = component.getComponentType();
                }
                if (component.isPrimitive()) {
                    return;
                }
                String name = component.getName();
                if (ALLOWED_PREFIXES.stream().noneMatch(name::startsWith)) {
                    violations.add(owner.getName() + " -> " + name);
                }
            }
            case ParameterizedType parameterized -> {
                collect(parameterized.getRawType(), violations, owner);
                for (Type argument : parameterized.getActualTypeArguments()) {
                    collect(argument, violations, owner);
                }
            }
            case GenericArrayType array -> collect(array.getGenericComponentType(), violations, owner);
            case WildcardType wildcard -> {
                for (Type bound : wildcard.getUpperBounds()) {
                    collect(bound, violations, owner);
                }
                for (Type bound : wildcard.getLowerBounds()) {
                    collect(bound, violations, owner);
                }
            }
            case TypeVariable<?> variable -> {
                for (Type bound : variable.getBounds()) {
                    collect(bound, violations, owner);
                }
            }
            default -> violations.add(owner.getName() + " -> unresolvable type " + type);
        }
    }

    private static List<Class<?>> apiTypes() throws IOException, URISyntaxException, ClassNotFoundException {
        Path classesRoot = Path.of(IrisTerrainService.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        Path apiRoot = classesRoot.resolve(API_PACKAGE.replace('.', '/'));
        assertTrue("compiled api package not found at " + apiRoot, Files.isDirectory(apiRoot));

        List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.walk(apiRoot)) {
            files.filter(path -> path.getFileName().toString().endsWith(".class"))
                    .forEach(path -> names.add(classesRoot.relativize(path).toString()
                            .replace('/', '.')
                            .replace('\\', '.')
                            .replaceAll("\\.class$", "")));
        }

        names.sort(String::compareTo);
        List<Class<?>> types = new ArrayList<>(names.size());
        for (String name : names) {
            types.add(Class.forName(name, false, IrisApiSurfacePurityTest.class.getClassLoader()));
        }
        return types;
    }
}
