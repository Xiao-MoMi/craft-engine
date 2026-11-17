package net.momirealms.craftengine.bedrock.geyser;

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.registry.loader.ProviderRegistryLoader;
import org.geysermc.geyser.registry.provider.ProviderSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public abstract class GeyserRegistryFixture {
    private MockedStatic<GeyserApi> apiHolder;
    private MockedStatic<GeyserImpl> geyser;

    @BeforeEach
    protected void loadGeyserRegistries(@TempDir Path directory) {
        GeyserImpl implementation = mock(GeyserImpl.class, RETURNS_DEEP_STUBS);
        when(implementation.packDirectory()).thenReturn(directory);
        Map<Class<?>, ProviderSupplier> providers = new ProviderRegistryLoader().load(new HashMap<>());
        GeyserApi api = mock(GeyserApi.class);
        when(api.provider(any(), any(Object[].class))).thenAnswer(call ->
                providers.get(call.getArgument(0)).create((Object[]) call.getRawArguments()[1]));
        this.geyser = mockStatic(GeyserImpl.class);
        this.geyser.when(GeyserImpl::getInstance).thenReturn(implementation);
        this.apiHolder = mockStatic(GeyserApi.class);
        this.apiHolder.when(GeyserApi::api).thenReturn(api);
    }

    @AfterEach
    protected void unloadGeyserRegistries() {
        this.apiHolder.close();
        this.geyser.close();
    }
}
