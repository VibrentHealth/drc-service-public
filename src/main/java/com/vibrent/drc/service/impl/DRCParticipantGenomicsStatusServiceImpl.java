package com.vibrent.drc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.drc.constants.DrcConstant;
import com.vibrent.drc.domain.ParticipantGenomicStatusBatch;
import com.vibrent.drc.domain.ParticipantGenomicStatusPayload;
import com.vibrent.drc.domain.SystemProperties;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.dto.GenomicGemResponseDTO;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.enumeration.ExternalGenomicPayloadProcessingStatus;
import com.vibrent.drc.enumeration.SystemPropertiesEnum;
import com.vibrent.drc.repository.ParticipantGenomicStatusBatchRepository;
import com.vibrent.drc.repository.ParticipantGenomicStatusPayloadRepository;
import com.vibrent.drc.repository.SystemPropertiesRepository;
import com.vibrent.drc.service.DRCBackendProcessorWrapper;
import com.vibrent.drc.service.DRCParticipantGenomicsStatusService;
import com.vibrent.drc.service.DataSharingMetricsService;
import com.vibrent.drc.service.ParticipantGenomicsStatusPayloadMapper;
import com.vibrent.drc.util.ExternalApiRequestLogUtil;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.exception.DrcConnectorException;
import com.vibrenthealth.drcutils.service.DRCConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponentsBuilder;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DRCParticipantGenomicsStatusServiceImpl implements DRCParticipantGenomicsStatusService {

    @Value("${vibrent.drc.genomics.participantStatus.batchProcessingSize}")
    private int batchProcessingSize;


    private final SystemPropertiesRepository systemPropertiesRepository;
    private final DRCConfigService drcConfigService;
    private final ParticipantGenomicStatusPayloadRepository participantGenomicStatusPayloadRepository;
    private final ParticipantGenomicStatusBatchRepository participantGenomicStatusBatchRepository;
    private final ParticipantGenomicsStatusPayloadMapper participantGenomicsStatusPayloadMapper;
    private final DataSharingMetricsService dataSharingMetricsService;
    private final DRCBackendProcessorWrapper drcBackendProcessorWrapper;

    public DRCParticipantGenomicsStatusServiceImpl(SystemPropertiesRepository systemPropertiesRepository,
                                                   DRCConfigService drcConfigService, ParticipantGenomicStatusPayloadRepository participantGenomicStatusPayloadRepository,
                                                   ParticipantGenomicStatusBatchRepository participantGenomicStatusBatchRepository,
                                                   ParticipantGenomicsStatusPayloadMapper participantGenomicsStatusPayloadMapper,
                                                   DataSharingMetricsService dataSharingMetricsService,
                                                   DRCBackendProcessorWrapper drcBackendProcessorWrapper) {
        this.systemPropertiesRepository = systemPropertiesRepository;
        this.drcConfigService = drcConfigService;
        this.participantGenomicStatusPayloadRepository = participantGenomicStatusPayloadRepository;
        this.participantGenomicStatusBatchRepository = participantGenomicStatusBatchRepository;
        this.participantGenomicsStatusPayloadMapper = participantGenomicsStatusPayloadMapper;
        this.dataSharingMetricsService = dataSharingMetricsService;
        this.drcBackendProcessorWrapper = drcBackendProcessorWrapper;

        this.drcBackendProcessorWrapper.initialize(true);
    }

    @Override
    @Transactional
    public void retrieveParticipantGenomicsStatusFromDrc() throws DrcConnectorException, JsonProcessingException {

        // If drc is not initialised, then log the warn message and return
        if (!drcBackendProcessorWrapper.isInitialized()) {
            log.warn("DRC: Not calling DRC Genomic report ready status as DRC is not initialised");
            return;
        }

        // retrieve the last time stamp from the database
        SystemProperties property = systemPropertiesRepository.findByName(SystemPropertiesEnum.DRC_GENOMICS_REPORT_READY_STATUS);

        String timeStampToFetchNextStatus = property != null ? property.getValue() : null;

        String startDate = timeStampToFetchNextStatus != null ? timeStampToFetchNextStatus : DrcConstant.DEFAULT_START_DATE;

        String uriString = drcConfigService.getDrcApiBaseUrl() +
                UriComponentsBuilder.fromUriString(DrcConstant.URL_GENOMICS_PARTICIPANT_STATUS)
                        .queryParam("start_date", "{startDate}")
                        .encode()
                        .buildAndExpand(startDate)
                        .toUriString();

        log.info("DRC Service: Request URL called for Genomics Outreach API V2: {}", uriString);
        ExternalApiRequestLog externalApiRequestLog = ExternalApiRequestLogUtil.createExternalApiRequestLog(ExternalEventType.DRC_GENOMICS_RESULT_STATUS);

        // call the DRC to get Genomics report ready status
        HttpResponseWrapper responseFromDrc = drcBackendProcessorWrapper.sendRequest(uriString, null, RequestMethod.GET, null, externalApiRequestLog);
        if (responseFromDrc.getStatusCode() == HttpStatus.OK.value()) {

            GenomicGemResponseDTO genomicGemResponseDTO = JacksonUtil.getMapper().readValue(responseFromDrc.getResponseBody(), GenomicGemResponseDTO.class);

            if (genomicGemResponseDTO.getTimestamp() == null) {
                log.warn("DRC: Returned Invalid response from DRC for Genomics report ready status api call with response as {}", responseFromDrc.getResponseBody());
                return;
            }

            ParticipantGenomicStatusPayload savedEntity = saveExternalParticipantStatusPayloads(startDate, genomicGemResponseDTO.getTimestamp(), responseFromDrc.getResponseBody());
            saveExternalParticipantStatusBatches(genomicGemResponseDTO.getData(), batchProcessingSize, savedEntity);

            // save the retrieved time stamp in the database.
            saveSystemProperties(property, genomicGemResponseDTO.getTimestamp());
            dataSharingMetricsService.incrementGenomicsStatusFetchInitiatedCounter(genomicGemResponseDTO.getData().size());
            log.info("DRC : Data saved successfully for requested timestamp {}", startDate);
        }else{
            log.warn("DRC : Error response received from DRC with status code as {}", responseFromDrc.getStatusCode());
        }
    }

    private SystemProperties saveSystemProperties(SystemProperties systemProperties, String timestamp) {
        if (systemProperties == null) {
            systemProperties = new SystemProperties();
            systemProperties.setName(SystemPropertiesEnum.DRC_GENOMICS_REPORT_READY_STATUS);
        }
        systemProperties.setValue(timestamp);
        this.systemPropertiesRepository.save(systemProperties);
        return systemProperties;
    }

    private ParticipantGenomicStatusPayload saveExternalParticipantStatusPayloads(String requestedTimestamp, String nextTimestamp, String responseString) {
        ParticipantGenomicStatusPayload participantGenomicStatusPayload = new ParticipantGenomicStatusPayload();
        participantGenomicStatusPayload.setRequestedTimestamp(requestedTimestamp);
        participantGenomicStatusPayload.setNextTimestamp(nextTimestamp);
        participantGenomicStatusPayload.setRawPayload(responseString);
        participantGenomicStatusPayload.setStatus(ExternalGenomicPayloadProcessingStatus.PENDING);
        return participantGenomicStatusPayloadRepository.save(participantGenomicStatusPayload);
    }

    private void saveExternalParticipantStatusBatches(List<Map<String, Object>> entireList, int partitionSize, ParticipantGenomicStatusPayload savedEntity) throws JsonProcessingException {
        List<ParticipantGenomicStatusBatch> participantGenomicStatusBatchList = new ArrayList<>();

        //divide the entire list into chucks
        final AtomicInteger counter = new AtomicInteger();
        final Collection<List<Map<String, Object>>> subLists = entireList.stream().collect(Collectors.groupingBy
                (it -> counter.getAndIncrement() / partitionSize))
                .values();

        for(List<Map<String, Object>> collection : subLists){
            participantGenomicStatusBatchList.add(buildExternalReportBatch(collection, partitionSize, savedEntity));
        }

        if(!CollectionUtils.isEmpty(participantGenomicStatusBatchList)){
            participantGenomicStatusBatchRepository.saveAll(participantGenomicStatusBatchList);
            log.info("DRC Genomics: Saved {} batches in the database", participantGenomicStatusBatchList.size());
        }
    }

    private ParticipantGenomicStatusBatch buildExternalReportBatch(List<Map<String, Object>> participantGenomicStatusDTOList, int partitionSize, ParticipantGenomicStatusPayload savedEntity) throws JsonProcessingException {
        ParticipantGenomicStatusBatch participantGenomicStatusBatch = new ParticipantGenomicStatusBatch();
        String jsonPayload = participantGenomicsStatusPayloadMapper.mapListOfParticipantGenomicsStatusToJsonString(participantGenomicStatusDTOList);
        participantGenomicStatusBatch.setBatchPayload(jsonPayload);
        participantGenomicStatusBatch.setBatchSize(partitionSize);
        participantGenomicStatusBatch.setStatus(ExternalGenomicPayloadProcessingStatus.PENDING);
        participantGenomicStatusBatch.setParticipantGenomicStatusPayload(savedEntity);
        participantGenomicStatusBatch.setRetryCount(0);
        return participantGenomicStatusBatch;
    }

}
