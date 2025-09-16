package org.egov.asset.calculator.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egov.asset.calculator.config.CalculatorConfig;
import org.egov.asset.calculator.kafka.broker.Producer;
import org.egov.asset.calculator.repository.AssetRepository;
import org.egov.asset.calculator.repository.DepreciationDetailRepository;
import org.egov.asset.calculator.repository.MdmsDataRepository;
import org.egov.asset.calculator.utils.AssetCalculatorUtil;
import org.egov.asset.calculator.utils.CalculatorConstants;
import org.egov.asset.calculator.utils.dto.DepreciationRateDTO;
import org.egov.asset.calculator.web.models.AuditDetails;
import org.egov.asset.calculator.web.models.DepreciationDetail;
import org.egov.asset.calculator.web.models.DepreciationReq;
import org.egov.asset.calculator.web.models.contract.Asset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ProcessDepreciationV2 {

    private final AssetRepository assetRepository;
    private final DepreciationDetailRepository depreciationDetailRepository;
    private final MdmsDataRepository mdmsDataRepository;
    private final Producer producer;
    private final AssetCalculatorUtil assetCalculatorUtil;
    private DepreciationReq depreciationReq ;

    private static int BATCH_SIZE;
    private AuditDetails auditDetails ;
    private String uuid;

    @Autowired
    public ProcessDepreciationV2(AssetRepository assetRepository,
                                 DepreciationDetailRepository depreciationDetailRepository,
                                 MdmsDataRepository mdmsDataRepository, Producer producer, 
                                 EnrichmentService enrichmentService, AssetCalculatorUtil assetCalculatorUtil,
                                 CalculatorConfig config) {
        this.assetRepository = assetRepository;
        this.depreciationDetailRepository = depreciationDetailRepository;
        this.mdmsDataRepository = mdmsDataRepository;
        this.producer = producer;
        this.assetCalculatorUtil = assetCalculatorUtil;
        depreciationReq = new DepreciationReq();
        auditDetails = new AuditDetails();

        BATCH_SIZE = config.getBatchSize();
    }

    /**
     * Calculates depreciation for assets based on legacy or non-legacy data.
     */
    @Transactional
    public String calculateDepreciation(String tenantId, String assetId, boolean legacyData, String uuid) {
        LocalDate currentDate = LocalDate.now();
        int totalAssets;
        String message;
        this.uuid = uuid;

        if(assetId != null ){
            legacyData = true;
            assetRepository.updateIsLegacyDataFlag(assetId);
        }

        // Count assets to process
        if (legacyData) {
            totalAssets = assetRepository.countLegacyAssets(tenantId, assetId);
        } else {
            totalAssets = assetRepository.countNonLegacyAssets(tenantId, assetId);
        }

        log.info("Total assets to be processed: {}", totalAssets);

        int pageIndex = 0;

        // Process assets in batches
        while (pageIndex * BATCH_SIZE < totalAssets) {
            if (pageIndex > totalAssets / BATCH_SIZE + 1) {
                log.error("Potential infinite loop detected in batch processing. Terminating.");
                break;
            }
            
            Pageable pageable = PageRequest.of(pageIndex, BATCH_SIZE);
            
            // Fetch all assets (no date filtering in query)
            List<Asset> allAssets = assetRepository.findAssetsForDepreciation(
                    tenantId, assetId, legacyData, pageable);
            
            // Filter for anniversary date in Java (only for non-legacy)
            List<Asset> assetsToProcess;
            if (!legacyData) {
                assetsToProcess = filterAssetsForAnniversary(allAssets, currentDate);
                log.info("Filtered {} assets out of {} for anniversary processing", 
                    assetsToProcess.size(), allAssets.size());
            } else {
                assetsToProcess = allAssets;
            }
            
            log.info("Processing batch {} with {} assets", pageIndex + 1, assetsToProcess.size());
            
            for (Asset asset : assetsToProcess) {
                processAssetDepreciation(asset, currentDate, legacyData);
            }

            pageIndex++;
        }
        
        message = CalculatorConstants.SUCCESS_MESSAGE;
        return message;
    }

    /**
     * Filters assets based on anniversary date matching current date
     */
    private List<Asset> filterAssetsForAnniversary(List<Asset> assets, LocalDate currentDate) {
        return assets.stream()
                .filter(asset -> isAnniversaryDate(asset, currentDate))
                .collect(Collectors.toList());
    }

    /**
     * Checks if asset's purchase date anniversary matches current date
     */
    private boolean isAnniversaryDate(Asset asset, LocalDate currentDate) {
        if (asset.getPurchaseDate() == null) {
            log.warn("Asset {} has null purchase date", asset.getId());
            return false;
        }

        try {
            // Convert epoch milliseconds to LocalDate
            LocalDate purchaseDate = Instant.ofEpochMilli(asset.getPurchaseDate())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // Check if month and day match (anniversary)
            boolean isAnniversary = purchaseDate.getMonthValue() == currentDate.getMonthValue() &&
                                   purchaseDate.getDayOfMonth() == currentDate.getDayOfMonth();

            // Don't process if it's the same year (same as original logic)
            boolean isSameYear = purchaseDate.getYear() == currentDate.getYear();

            log.debug("Asset {}: purchase={}, current={}, anniversary={}, sameYear={}", 
                asset.getId(), purchaseDate, currentDate, isAnniversary, isSameYear);

            return isAnniversary && !isSameYear;
            
        } catch (Exception e) {
            log.error("Error processing date for asset {}: {}", asset.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Processes depreciation for a single asset.
     */
    private void processAssetDepreciation(Asset asset, LocalDate currentDate, boolean legacyData) {
        int yearsElapsed;
        int remainingLife;

        if (legacyData) {
            log.info("Processing legacy Data, legacyData Flag = {}", legacyData );
            asset.setBookValue(BigDecimal.valueOf(asset.getOriginalBookValue()).doubleValue());
            
            // Convert purchase date from milliseconds to LocalDate
            LocalDate purchaseDate = LocalDate.ofEpochDay(asset.getPurchaseDate() / CalculatorConstants.SECONDS_IN_A_DAY);
            if (purchaseDate == null || asset.getLifeOfAsset() == null) {
                log.warn("Skipping asset due to missing mandatory fields: {}", asset.getId());
                return;
            }

            // Calculate asset life details
            int totalLife = Integer.parseInt(asset.getLifeOfAsset());
            LocalDate lifeEndDate = purchaseDate.plusYears(totalLife);

            if (lifeEndDate.isBefore(purchaseDate)) {
                log.warn("Invalid asset life for asset: {}, purchaseDate: {}, lifeEndDate: {}", 
                    asset.getId(), purchaseDate, lifeEndDate);
                return;
            }

            // Calculate depreciation for each year until current date
            LocalDate startDate = purchaseDate;
            LocalDate endDate = startDate.plusYears(1);
            yearsElapsed = currentDate.getYear() - purchaseDate.getYear();

            if (currentDate.isAfter(lifeEndDate)) {
                log.info("Asset life expired. Calculating depreciation only until {}", lifeEndDate);
            }

            while (endDate.isBefore(currentDate) || endDate.isEqual(currentDate)) {
                if (startDate.isAfter(lifeEndDate) || endDate.isAfter(lifeEndDate)) { 
                    break; 
                }

                DepreciationRateDTO depreciationRateDTO = fetchDepreciationRateAndMethod(
                    asset.getAssetCategory(), asset.getPurchaseDate());

                if (depreciationRateDTO == null) {
                    log.warn("Skipping depreciation due to missing rate/method for asset: {}", asset.getId());
                    break;
                }

                BigDecimal depreciationRate = depreciationRateDTO.getRate();
                BigDecimal depreciation = null;

                String depreciationMethod = depreciationRateDTO.getMethod();
                if(depreciationMethod.equals(CalculatorConstants.SLM)){
                    depreciation = calculateDepreciationValueSLM(asset, depreciationRate);
                }
                else if (CalculatorConstants.DBM.equals(depreciationMethod)) {
                    depreciation = calculateDepreciationValueDBM(asset, depreciationRate);
                } else {
                    log.warn("Unknown depreciation method: {}", depreciationMethod);
                    break;
                }

                BigDecimal currentBookValue = BigDecimal.valueOf(asset.getBookValue()).subtract(depreciation);
                asset.setBookValue(currentBookValue.doubleValue());
                saveDepreciationDetail(asset, startDate, endDate, depreciation, depreciationRate, true, depreciationMethod);

                startDate = endDate;
                endDate = startDate.plusYears(1);
            }

            asset.setIsLegacyData(false);
            assetRepository.save(asset);
        } else {
            log.info("Processing Non legacy Data, legacyData Flag = {}", legacyData);
            
            // Convert purchase date from milliseconds to LocalDate
            LocalDate purchaseDate = Instant.ofEpochMilli(asset.getPurchaseDate())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
                    
            if (purchaseDate == null) {
                log.warn("Skipping asset with null purchase date: {}", asset.getId());
                return;
            }

            LocalDate anniversaryDate = LocalDate.of(currentDate.getYear(), 
                purchaseDate.getMonth(), purchaseDate.getDayOfMonth());

            // Additional validation (this should already be filtered, but keeping for safety)
            if (!anniversaryDate.equals(currentDate) || purchaseDate.equals(currentDate)) {
                log.warn("Anniversary date / purchase date not applicable for assetId: {} Skipping processing.", 
                    asset.getId());
                return;
            }
            
            DepreciationRateDTO depreciationRateDTO = fetchDepreciationRateAndMethod(
                asset.getAssetCategory(), asset.getPurchaseDate());
            BigDecimal depreciationRate = depreciationRateDTO.getRate();
            BigDecimal depreciation = null;

            String depreciationMethod = depreciationRateDTO.getMethod();
            if (depreciationRate != null && depreciationRate.compareTo(BigDecimal.ZERO) > 0) {
                if (depreciationMethod.equals(CalculatorConstants.SLM)) {
                    depreciation = calculateDepreciationValueSLM(asset, depreciationRate);
                } else {
                    depreciation = calculateDepreciationValueDBM(asset, depreciationRate);
                }

                BigDecimal currentBookValue = BigDecimal.valueOf(asset.getBookValue()).subtract(depreciation);
                asset.setBookValue(currentBookValue.doubleValue());

                saveDepreciationDetail(asset, purchaseDate, anniversaryDate, depreciation, 
                    depreciationRate, false, depreciationMethod);
                assetRepository.save(asset);
            }
            else {
                log.info("Depreciation not processed for assetId : {} as rate is Zero", asset.getId());
            }
        }
    }

    // Rest of your methods remain the same...
    private DepreciationRateDTO fetchDepreciationRateAndMethod(String category, Long purchaseDate) {
        BigDecimal depreciationRate = null;
        String depreciationMethod = null;

        Object[] result = mdmsDataRepository.findDepreciationRateByCategoryAndPurchaseDate(
            category.trim(), purchaseDate*CalculatorConstants.MILLISECONDS_IN_A_SECOND);
        try {
            log.debug("Fetched depreciation rate for rate, method: {}", result[0]);

            if (((Object[]) result).length > 0 && ((Object[]) result)[0] instanceof Object[]) {
                Object[] innerArray = (Object[]) ((Object[]) result)[0];
                depreciationRate = (innerArray[0] instanceof BigDecimal) ? (BigDecimal) innerArray[0] : null;
                depreciationMethod = (innerArray[1] instanceof String) ? (String) innerArray[1] : null;

                if (depreciationRate == null || depreciationMethod == null) {
                    throw new IllegalArgumentException("Depreciation details are incomplete.");
                }
                log.info("Depreciation Rate is : {} and method is :{} ", depreciationRate, depreciationMethod);
                return new DepreciationRateDTO(depreciationRate, depreciationMethod);
            }
        }
        catch (Exception e) {
            log.error("Unexpected error while fetching depreciation rate: {}", e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred while processing depreciation data.", e);
        }

        log.info("Depreciation Rate is : {} and method is :{} ", depreciationRate, depreciationMethod);
        return new DepreciationRateDTO(depreciationRate, depreciationMethod);
    }

    private BigDecimal calculateDepreciationValueSLM(Asset asset, BigDecimal depreciationRate) {
        if (depreciationRate == null || BigDecimal.ZERO.compareTo(depreciationRate) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal bookValue = BigDecimal.valueOf(asset.getBookValue());
        BigDecimal minimumValue = BigDecimal.valueOf(asset.getMinimumValue());
        BigDecimal originalBookValue = BigDecimal.valueOf(asset.getOriginalBookValue());
        BigDecimal maxDepreciation = bookValue.subtract(minimumValue);

        BigDecimal calculatedDepreciation = originalBookValue.multiply(depreciationRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return calculatedDepreciation.min(maxDepreciation);
    }

    private BigDecimal calculateDepreciationValueDBM(Asset asset, BigDecimal depreciationRate) {
        if (depreciationRate == null || BigDecimal.ZERO.compareTo(depreciationRate) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal bookValue = BigDecimal.valueOf(asset.getBookValue());
        BigDecimal minimumValue = BigDecimal.valueOf(asset.getMinimumValue());
        BigDecimal maxDepreciation = bookValue.subtract(minimumValue);

        BigDecimal calculatedDepreciation = bookValue.multiply(depreciationRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return calculatedDepreciation.min(maxDepreciation);
    }

    private void saveDepreciationDetail(Asset asset, LocalDate startDate, LocalDate endDate, 
                                       BigDecimal depreciation, BigDecimal depreciationRate, 
                                       boolean legacyData, String depreciationMethod) {
        Optional<DepreciationDetail> existingDetail = depreciationDetailRepository
            .findByAssetIdAndFromDateAndToDate(asset.getId(), startDate, endDate);

        if (existingDetail.isPresent()) {
            if (legacyData) {
                auditDetails = assetCalculatorUtil.getAuditDetails(uuid, false);
                DepreciationDetail detail = existingDetail.get();
                detail.setDepreciationValue(depreciation.doubleValue());
                detail.setBookValue(asset.getBookValue());
                detail.setRate(depreciationRate.doubleValue());
                detail.setDepreciationMethod(depreciationMethod);
                detail.setOldBookValue(BigDecimal.valueOf(asset.getBookValue()).add(depreciation).doubleValue());
                detail.setUpdatedAt(auditDetails.getLastModifiedTime());
                detail.setUpdatedBy(auditDetails.getLastModifiedBy());
                depreciationReq.setDepreciation(detail);

                log.info("Pushing message to Kafka: topic={}, data={}", "update-depreciation",detail);
                producer.push("update-depreciation",depreciationReq);
            } else {
                log.warn("Duplicate depreciation record found for asset {} from {} to {}. Skipping save as legacyData is false.",
                        asset.getId(), startDate, endDate);
            }
        } else {
            auditDetails = assetCalculatorUtil.getAuditDetails(uuid, true);
            DepreciationDetail detail = new DepreciationDetail();
            detail.setAssetId(asset.getId());
            detail.setFromDate(startDate);
            detail.setToDate(endDate);
            detail.setDepreciationValue(depreciation.doubleValue());
            detail.setBookValue(asset.getBookValue());
            detail.setCreatedAt(auditDetails.getCreatedTime());
            detail.setCreatedBy(auditDetails.getCreatedBy());
            detail.setRate(depreciationRate.doubleValue());
            detail.setDepreciationMethod(depreciationMethod);
            detail.setOldBookValue(BigDecimal.valueOf(asset.getBookValue()).add(depreciation).doubleValue());

            depreciationReq.setDepreciation(detail);
            log.info("Pushing message to Kafka: Create topic={}, data={}", "save-depreciation",detail);
            producer.push("save-depreciation",depreciationReq);
        }
    }
}