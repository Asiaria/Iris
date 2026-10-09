package art.arcane.iris.modded;

enum ModdedGenerationMode {
    PERSISTENT_CREATE(true),
    PERSISTENT_RESTORE(true),
    TRANSIENT_STUDIO(false);

    private final boolean historyRequired;

    ModdedGenerationMode(boolean historyRequired) {
        this.historyRequired = historyRequired;
    }

    boolean historyRequired() {
        return historyRequired;
    }
}
