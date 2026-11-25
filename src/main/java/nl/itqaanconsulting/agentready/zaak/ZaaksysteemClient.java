package nl.itqaanconsulting.agentready.zaak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.HtmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.http.HttpClient;
import java.time.LocalDate;

/**
 * SOAP client for the legacy case system (StUF-inspired, simplified). Treats the legacy system
 * as untrusted input: strict timeouts, validated case numbers, and an XML parser with DOCTYPEs
 * and external entities disabled (no XXE).
 */
@Component
public class ZaaksysteemClient {

    static final String NAMESPACE = "urn:voorbeeldstad:zaken:v1";
    private static final Logger log = LoggerFactory.getLogger(ZaaksysteemClient.class);

    private final RestClient restClient;

    public ZaaksysteemClient(RestClient.Builder builder, ZaaksysteemProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = builder.baseUrl(properties.url()).requestFactory(requestFactory).build();
    }

    public Zaak geefZaakdetails(String zaaknummer) {
        String nummer = Zaaknummer.validated(zaaknummer);
        String response;
        try {
            response = restClient.post()
                    .contentType(MediaType.TEXT_XML)
                    .header("SOAPAction", NAMESPACE + ":GeefZaakdetails")
                    .body(request(nummer))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            if (exception.getResponseBodyAsString().contains("ZaakNietGevonden")) {
                throw new ZaakNietGevondenException(nummer);
            }
            throw unavailable("HTTP " + exception.getStatusCode().value(), exception);
        } catch (RuntimeException exception) {
            throw unavailable(exception.getClass().getSimpleName(), exception);
        }
        return parse(response, nummer);
    }

    private static String request(String zaaknummer) {
        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:zkn="%s">
                  <soapenv:Body>
                    <zkn:GeefZaakdetails><zkn:zaaknummer>%s</zkn:zaaknummer></zkn:GeefZaakdetails>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(NAMESPACE, HtmlUtils.htmlEscape(zaaknummer));
    }

    private Zaak parse(String xml, String zaaknummer) {
        try {
            Element zaak = (Element) secureParser(xml).getElementsByTagNameNS(NAMESPACE, "zaak").item(0);
            if (zaak == null) {
                throw new IllegalStateException("No zaak element in response");
            }
            Element type = child(zaak, "zaaktype");
            Element status = child(zaak, "status");
            Element behandelaar = child(zaak, "behandelaar");
            Element initiator = child(zaak, "initiator");
            return new Zaak(
                    text(zaak, "zaaknummer"),
                    new Zaak.Zaaktype(type.getAttribute("code"), type.getTextContent().trim()),
                    new Zaak.Status(status.getAttribute("code"), status.getTextContent().trim(),
                            LocalDate.parse(status.getAttribute("datum"))),
                    LocalDate.parse(text(zaak, "startdatum")),
                    LocalDate.parse(text(zaak, "uiterlijkeEinddatum")),
                    new Zaak.Behandelaar(text(behandelaar, "naam"), text(behandelaar, "afdeling")),
                    new Zaak.Initiator(text(initiator, "bsn"), text(initiator, "naam"), text(initiator, "adres"),
                            text(initiator, "telefoon"), text(initiator, "email")),
                    text(zaak, "toelichting"));
        } catch (RuntimeException exception) {
            throw unavailable("Unreadable response for " + zaaknummer, exception);
        } catch (Exception exception) {
            throw unavailable("Rejected response for " + zaaknummer, exception);
        }
    }

    static Document secureParser(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static Element child(Element parent, String name) {
        Node node = parent.getElementsByTagNameNS(NAMESPACE, name).item(0);
        if (node == null) {
            throw new IllegalStateException("Missing element " + name);
        }
        return (Element) node;
    }

    private static String text(Element parent, String name) {
        return child(parent, name).getTextContent().trim();
    }

    private static ZaaksysteemNietBeschikbaarException unavailable(String detail, Exception cause) {
        log.warn("Zaaksysteem unavailable: {}", detail);
        return new ZaaksysteemNietBeschikbaarException(detail, cause);
    }
}
