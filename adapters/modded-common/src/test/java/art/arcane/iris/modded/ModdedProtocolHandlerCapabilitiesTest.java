package art.arcane.iris.modded;

import art.arcane.iris.spi.protocol.IrisProtocol;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class ModdedProtocolHandlerCapabilitiesTest {
    @Test
    public void advertisesEveryServerFeatureThatSendsProtocolFrames() throws Exception {
        Field field = ModdedProtocolHandler.class.getDeclaredField("SERVER_CAPABILITIES");
        field.setAccessible(true);

        assertEquals(IrisProtocol.CAPABILITY_PREGEN
                | IrisProtocol.CAPABILITY_VISION
                | IrisProtocol.CAPABILITY_CURSOR
                | IrisProtocol.CAPABILITY_STUDIO, field.getLong(null));
    }
}
