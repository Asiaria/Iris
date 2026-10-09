package art.arcane.iris.client;

import art.arcane.iris.spi.protocol.IrisMessage;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IrisClientHotloadTest {
    private static final IrisMessage.DimensionStatus OVERWORLD = new IrisMessage.DimensionStatus(
            "overworld",
            "pack",
            1L,
            -64,
            320,
            true
    );

    @Test
    public void successfulCurrentPackHotloadInvalidatesWorldCaches() {
        IrisMessage.StudioHotload hotload = new IrisMessage.StudioHotload("pack", 0, false, "");

        assertTrue(IrisClient.shouldInvalidateForHotload(OVERWORLD, hotload));
    }

    @Test
    public void failedOrUnrelatedHotloadRetainsWorldCaches() {
        assertFalse(IrisClient.shouldInvalidateForHotload(
                OVERWORLD,
                new IrisMessage.StudioHotload("pack", 0, true, "failed")
        ));
        assertFalse(IrisClient.shouldInvalidateForHotload(
                OVERWORLD,
                new IrisMessage.StudioHotload("other", 0, false, "")
        ));
        assertFalse(IrisClient.shouldInvalidateForHotload(null, null));
    }
}
