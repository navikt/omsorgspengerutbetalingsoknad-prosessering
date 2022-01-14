package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.k9mellomlagring.Dokument
import no.nav.helse.k9mellomlagring.DokumentEier
import no.nav.helse.k9mellomlagring.K9MellomlagringService
import no.nav.helse.k9mellomlagring.Søknadsformat
import no.nav.helse.prosessering.Metadata
import org.slf4j.LoggerFactory
import java.net.URI

internal class PreprosesseringV1Service(
    private val pdfV1Generator: PdfV1Generator,
    private val k9MellomlagringService: K9MellomlagringService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(PreprosesseringV1Service::class.java)
    }

    internal suspend fun preprosesser(
        melding: MeldingV1,
        metadata: Metadata
    ): PreprossesertMeldingV1 {
        val correlationId = CorrelationId(metadata.correlationId)
        val dokumentEier = DokumentEier(melding.søker.fødselsnummer)

        logger.info("Genererer Oppsummerings-PDF av søknaden.")
        val oppsummeringPdf = pdfV1Generator.genererSøknadOppsummeringPdf(melding)

        logger.info("Mellomlagrer Oppsummerings-PDF.")
        val oppsummeringPdfDokumentId = k9MellomlagringService.lagreDokument(
            dokument = Dokument(
                eier = dokumentEier,
                content = oppsummeringPdf,
                contentType = "application/pdf",
                title = "Søknad om utbetaling av omsorgspenger"
            ),
            correlationId = correlationId
        ).dokumentId()

        logger.info("Mellomlagrer Oppsummerings-JSON")
        val soknadJsonDokumentId = k9MellomlagringService.lagreDokument(
            dokument = Dokument(
                eier = dokumentEier,
                content = Søknadsformat.somJson(melding.k9FormatSøknad),
                contentType = "application/json",
                title = "Søknad om utbetaling av omsorgspenger som JSON"
            ),
            correlationId = correlationId
        ).dokumentId()

        val komplettDokumentId = mutableListOf(
            listOf(
                oppsummeringPdfDokumentId,
                soknadJsonDokumentId
            )
        )

        if(melding.vedleggId.isNotEmpty()){
            logger.info("Legger til ${melding.vedleggId.size} vedleggId fra søknad")
            melding.vedleggId.forEach { komplettDokumentId.add(listOf(it)) }
        }

        logger.info("Totalt ${komplettDokumentId.size} dokumentbolker med totalt ${komplettDokumentId.flatten().size} dokumenter.")

        val preprossesertMeldingV1 = PreprossesertMeldingV1(
            melding = melding,
            dokumentId = komplettDokumentId.toList()
        )
        melding.reportMetrics()

        return preprossesertMeldingV1
    }
}

fun URI.dokumentId() = this.toString().substringAfterLast("/")