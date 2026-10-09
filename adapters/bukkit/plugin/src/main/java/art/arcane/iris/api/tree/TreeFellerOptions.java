package art.arcane.iris.api.tree;

import java.util.Objects;

public record TreeFellerOptions(
        TreeFellerAccess access,
        int durabilityPreservationChance,
        TreeFellerRunHooks runHooks
) {
    public TreeFellerOptions {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(runHooks, "runHooks");
        if (durabilityPreservationChance < 0 || durabilityPreservationChance > 100) {
            throw new IllegalArgumentException("durabilityPreservationChance must be between 0 and 100");
        }
    }

    public static TreeFellerOptions standalone() {
        return new TreeFellerOptions(TreeFellerAccess.STANDALONE, 0, TreeFellerRunHooks.NONE);
    }

    public static TreeFellerOptions integrationOverride(
            int durabilityPreservationChance,
            TreeFellerRunHooks runHooks
    ) {
        return new TreeFellerOptions(
                TreeFellerAccess.INTEGRATION_OVERRIDE,
                durabilityPreservationChance,
                runHooks
        );
    }
}
