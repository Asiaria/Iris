package art.arcane.iris.probe;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ClassloadProbeTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void scansNamedAndGeneratedNestedClasses() throws Exception {
        Path classesRoot = temporaryFolder.newFolder("classes").toPath();
        Path packageRoot = Files.createDirectories(classesRoot.resolve("art/arcane/iris"));
        Files.createFile(packageRoot.resolve("Outer.class"));
        Files.createFile(packageRoot.resolve("Outer$Nested.class"));
        Files.createFile(packageRoot.resolve("Outer$1.class"));
        Files.createFile(packageRoot.resolve("ignored.txt"));

        List<String> names = ClassloadProbe.scanClassNames(classesRoot);

        assertEquals(List.of(
                "art.arcane.iris.Outer",
                "art.arcane.iris.Outer$1",
                "art.arcane.iris.Outer$Nested"
        ), names);
    }

    @Test
    public void scansEmptyClassDirectory() throws Exception {
        Path classesRoot = temporaryFolder.newFolder("empty-classes").toPath();

        assertTrue(ClassloadProbe.scanClassNames(classesRoot).isEmpty());
    }

    @Test
    public void acceptsMatchingReviewedFailure() {
        ClassloadProbe.Failure failure = new ClassloadProbe.Failure(
                "java.lang.ClassNotFoundException",
                "org.bukkit.event.Listener"
        );
        ClassloadProbe.Allowance allowance = new ClassloadProbe.Allowance(
                ClassloadProbe.AllowanceCategory.BUKKIT_API,
                "org.bukkit.event.Listener"
        );

        ClassloadProbe.Review review = ClassloadProbe.review(
                Map.of("art.arcane.iris.Allowed", failure),
                Map.of("art.arcane.iris.Allowed", allowance)
        );

        assertTrue(review.passes());
    }

    @Test
    public void rejectsUnlistedFailure() {
        ClassloadProbe.Failure failure = new ClassloadProbe.Failure(
                "java.lang.ClassNotFoundException",
                "org.bukkit.event.Listener"
        );

        ClassloadProbe.Review review = ClassloadProbe.review(
                Map.of("art.arcane.iris.Unlisted", failure),
                Map.of()
        );

        assertFalse(review.passes());
        assertTrue(review.unexpected().containsKey("art.arcane.iris.Unlisted"));
    }

    @Test
    public void acceptsDifferentMissingClassInReviewedNamespace() {
        ClassloadProbe.Failure changedFailure = new ClassloadProbe.Failure(
                "java.lang.ClassNotFoundException",
                "org.bukkit.Material"
        );
        ClassloadProbe.Allowance allowance = new ClassloadProbe.Allowance(
                ClassloadProbe.AllowanceCategory.BUKKIT_API,
                "org.bukkit.event.Listener"
        );

        ClassloadProbe.Review review = ClassloadProbe.review(
                Map.of("art.arcane.iris.Changed", changedFailure),
                Map.of("art.arcane.iris.Changed", allowance)
        );

        assertTrue(review.passes());
    }

    @Test
    public void rejectsChangedFailureNamespace() {
        ClassloadProbe.Failure changedFailure = new ClassloadProbe.Failure(
                "java.lang.ClassNotFoundException",
                "io.papermc.lib.environments.Environment"
        );
        ClassloadProbe.Allowance allowance = new ClassloadProbe.Allowance(
                ClassloadProbe.AllowanceCategory.BUKKIT_API,
                "org.bukkit.event.Listener"
        );

        ClassloadProbe.Review review = ClassloadProbe.review(
                Map.of("art.arcane.iris.Changed", changedFailure),
                Map.of("art.arcane.iris.Changed", allowance)
        );

        assertFalse(review.passes());
        assertTrue(review.unexpected().containsKey("art.arcane.iris.Changed"));
    }

    @Test
    public void rejectsNonMissingClassFailure() {
        ClassloadProbe.Failure changedFailure = new ClassloadProbe.Failure(
                "java.lang.IllegalStateException",
                "org.bukkit.event.Listener"
        );
        ClassloadProbe.Allowance allowance = new ClassloadProbe.Allowance(
                ClassloadProbe.AllowanceCategory.BUKKIT_API,
                "org.bukkit.event.Listener"
        );

        assertFalse(allowance.matches(changedFailure));
    }

    @Test
    public void acceptsBukkitFailureCachedByClassInitialization() {
        ClassloadProbe.Failure failure = new ClassloadProbe.Failure(
                "java.lang.ExceptionInInitializerError",
                "Exception java.lang.NoClassDefFoundError: org/bukkit/Material [in thread \"main\"]"
        );
        ClassloadProbe.Allowance allowance = new ClassloadProbe.Allowance(
                ClassloadProbe.AllowanceCategory.BUKKIT_API,
                "org.bukkit.Material"
        );

        assertTrue(allowance.matches(failure));
    }

    @Test
    public void acceptsDifferentBukkitFailureCachedByClassInitialization() {
        ClassloadProbe.Failure failure = new ClassloadProbe.Failure(
                "java.lang.ExceptionInInitializerError",
                "Exception java.lang.NoClassDefFoundError: org/bukkit/World [in thread \"main\"]"
        );
        ClassloadProbe.Allowance allowance = new ClassloadProbe.Allowance(
                ClassloadProbe.AllowanceCategory.BUKKIT_API,
                "org.bukkit.Material"
        );

        assertTrue(allowance.matches(failure));
    }

    @Test
    public void rejectsDifferentNamespaceCachedByClassInitialization() {
        ClassloadProbe.Failure failure = new ClassloadProbe.Failure(
                "java.lang.ExceptionInInitializerError",
                "Exception java.lang.NoClassDefFoundError: io/papermc/lib/environments/Environment [in thread \"main\"]"
        );
        ClassloadProbe.Allowance allowance = new ClassloadProbe.Allowance(
                ClassloadProbe.AllowanceCategory.BUKKIT_API,
                "org.bukkit.Material"
        );

        assertFalse(allowance.matches(failure));
    }

    @Test
    public void acceptsMythicMobsApiNamespace() {
        ClassloadProbe.Failure failure = new ClassloadProbe.Failure(
                "java.lang.ClassNotFoundException",
                "io.lumine.mythic.api.skills.conditions.ILocationCondition"
        );
        ClassloadProbe.Allowance allowance = new ClassloadProbe.Allowance(
                ClassloadProbe.AllowanceCategory.MYTHICMOBS_API,
                "io.lumine.mythic.api.skills.conditions.ILocationCondition"
        );

        assertTrue(allowance.matches(failure));
    }

    @Test
    public void rejectsStaleAllowance() {
        ClassloadProbe.Allowance allowance = new ClassloadProbe.Allowance(
                ClassloadProbe.AllowanceCategory.BUKKIT_API,
                "org.bukkit.event.Listener"
        );
        TreeMap<String, ClassloadProbe.Allowance> allowlist = new TreeMap<>();
        allowlist.put("art.arcane.iris.Stale", allowance);

        ClassloadProbe.Review review = ClassloadProbe.review(Map.of(), allowlist);

        assertFalse(review.passes());
        assertTrue(review.staleAllowances().containsKey("art.arcane.iris.Stale"));
    }
}
