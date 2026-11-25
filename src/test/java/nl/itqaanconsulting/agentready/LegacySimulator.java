package nl.itqaanconsulting.agentready;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/** The simulated legacy case system: WireMock serving the stubs in legacy-simulator/ (also used by the demo). */
public final class LegacySimulator {

    private static WireMockServer server;

    private LegacySimulator() {
    }

    public static synchronized String url() {
        if (server == null) {
            server = new WireMockServer(options().dynamicPort().usingFilesUnderDirectory("legacy-simulator"));
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        }
        return "http://localhost:" + server.port() + "/zaaksysteem/ZaakService";
    }
}
