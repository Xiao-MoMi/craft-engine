package net.momirealms.craftengine.bedrock.geyser.title;

import net.momirealms.craftengine.bedrock.text.ContainerTitleCodec;
import net.momirealms.craftengine.bedrock.text.TitleFrame;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitleFrameTest {
    @Test
    void recognizesLayeredTitlesWithGeysersFormattingReset() {
        String reset = "\u00a7r";
        String encoded = ContainerTitleCodec.encode(ContainerTitleCodec.TITLE_MARKER, List.of(reset + "bar", reset + "pointer"));
        TitleFrame.Content plain = TitleFrame.content(encoded);
        TitleFrame.Content prefixed = TitleFrame.content(reset + encoded);
        assertTrue(prefixed.layered());
        assertEquals(2, prefixed.size());
        assertEquals(TitleFrame.encode(255, 0, 2, TitleFrame.EMPTY, plain),
                TitleFrame.encode(255, 0, 2, TitleFrame.EMPTY, prefixed));
        assertFalse(TitleFrame.content(reset + "ordinary subtitle").layered());
    }
}
