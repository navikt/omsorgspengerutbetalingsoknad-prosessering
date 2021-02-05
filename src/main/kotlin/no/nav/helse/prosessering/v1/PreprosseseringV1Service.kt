package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktørId
import no.nav.helse.dokument.DokumentService
import no.nav.helse.k9format.tilKOmsorgspengerUtbetalingSøknad
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.Søknad
import org.slf4j.LoggerFactory

internal class PreprosseseringV1Service(
    private val pdfV1Generator: PdfV1Generator,
    private val dokumentService: DokumentService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(PreprosseseringV1Service::class.java)
    }

    internal suspend fun preprosseser(
        melding: MeldingV1,
        metadata: Metadata
    ): PreprossesertMeldingV1 {
        val søknadId = SoknadId(melding.søknadId)
        logger.info("Preprosseserer $søknadId")

        val correlationId = CorrelationId(metadata.correlationId)

        val søkerAktørId = AktørId(melding.søker.aktørId)

        logger.info("Søkerens AktørID = $søkerAktørId")

        logger.info("Genererer Oppsummerings-PDF av søknaden.")
        val soknadOppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdf(melding)

        logger.info("Generering av Oppsummerings-PDF OK.")

        logger.info("Mellomlagrer Oppsummerings-PDF.")
        val soknadOppsummeringPdfUrl = dokumentService.lagreSoknadsOppsummeringPdf(
            pdf = soknadOppsummeringPdf,
            correlationId = correlationId,
            aktørId = søkerAktørId
        )
        logger.info("Mellomlagring av Oppsummerings-PDF OK")

        logger.info("Mellomlagrer Oppsummerings-JSON")

        val k9FormatSøknad: Søknad = melding.k9FormatSøknad?.let {
            logger.info("Bruker k9Format fra api: {}", JsonUtils.toString(it)) // TODO: 05/02/2021 fjern før prodsetting
            it
        } ?: melding.tilKOmsorgspengerUtbetalingSøknad()

        val soknadJsonUrl = dokumentService.lagreSoknadsMelding(
            melding = k9FormatSøknad,
            aktørId = søkerAktørId,
            correlationId = correlationId
        )
        logger.info("Mellomlagrer Oppsummerings-JSON OK.")

        val komplettDokumentUrls = mutableListOf(
            listOf(
                soknadOppsummeringPdfUrl,
                soknadJsonUrl
            )
        )


        melding.vedlegg.forEach { komplettDokumentUrls.add(listOf(it)) }

        logger.info("Totalt ${komplettDokumentUrls.size} dokumentbolker.")


        val preprossesertMeldingV1 = PreprossesertMeldingV1(
            melding = melding,
            dokumentUrls = komplettDokumentUrls.toList(),
            søkerAktørId = søkerAktørId,
            k9FormatSøknad = k9FormatSøknad
        )
        melding.reportMetrics()
        preprossesertMeldingV1.reportMetrics()
        return preprossesertMeldingV1
    }

}
