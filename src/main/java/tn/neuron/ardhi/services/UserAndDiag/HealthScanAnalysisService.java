package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.models.UserAndDiag.*;
import tn.neuron.ardhi.utils.UserAndDiag.ImgBBService;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the full health scan pipeline:
 * 1. Upload photos → ImgBB
 * 2. Analyze each photo → GroqService
 * 3. Detect vulnerabilities → VulnerabilityDetectionService
 * 4. Generate prevention plans → GroqService
 * 5. Save everything to DB
 */
public class HealthScanAnalysisService {

    private final GroqService groqService = new GroqService();
    private final WeatherService weatherService = new WeatherService();
    private final VulnerabilityDetectionService vulnService = new VulnerabilityDetectionService();
    private final FarmHealthScanService scanService = new FarmHealthScanService();
    private final PreventionPlanService planService = new PreventionPlanService();
    private final PreventionTaskService taskService = new PreventionTaskService();
    private Connection cnx = MyDatabase.getInstance().getCnx();

    /**
     * Processes a scan: analyze photos, detect vulnerabilities, generate plans,
     * save report. This runs on a background thread.
     *
     * @param scanId      the ID of the saved scan
     * @param localPhotos map of photo type → local File (before upload)
     * @return the generated report, or null on failure
     */
    public FarmHealthReport processScan(int scanId, java.util.Map<String, File> localPhotos) {
        try {
            // 1. Mark as processing
            scanService.updateStatus(scanId, ScanStatus.PROCESSING);

            FarmHealthScan scan = scanService.getById(scanId);
            if (scan == null) {
                scanService.updateStatus(scanId, ScanStatus.FAILED);
                return null;
            }

            String cropContext = buildCropContext(scan);

            // 2. Upload photos to ImgBB & update scan
            uploadPhotos(scan, localPhotos);

            // 3. Analyze each photo via Groq Vision API
            StringBuilder allAnalysis = new StringBuilder();
            String[] photoTypes = { "crops", "soil", "edges", "insects", "spacing", "overview" };
            String[] photoUrls = {
                    scan.getPhotoCrops(), scan.getPhotoSoil(), scan.getPhotoEdges(),
                    scan.getPhotoInsects(), scan.getPhotoSpacing(), scan.getPhotoOverview()
            };

            for (int i = 0; i < photoTypes.length; i++) {
                if (photoUrls[i] != null && !photoUrls[i].isEmpty()) {
                    System.out.println("[HealthScan] Analyzing " + photoTypes[i] + "...");
                    String result = groqService.analyzeHealthPhotoFromUrl(photoUrls[i], photoTypes[i], cropContext);
                    if (result != null && !result.equals("ERREUR_ANALYSE") && !result.equals("AUCUN_RISQUE")) {
                        allAnalysis.append(result).append("\n");
                    }
                }
            }

            // 4. Get weather data for context
            double lat = scan.getLatitude() != null ? scan.getLatitude() : 36.8065;
            double lon = scan.getLongitude() != null ? scan.getLongitude() : 10.1815;
            WeatherService.WeatherData weather = weatherService.getCurrentWeather(lat, lon);

            // 5. Detect vulnerabilities
            List<Vulnerability> vulnerabilities = vulnService.detectVulnerabilities(
                    allAnalysis.toString(), weather, scan.getCropType());

            // 6. Calculate scores
            int healthScore = vulnService.calculateHealthScore(vulnerabilities);
            int bioScore = vulnService.calculateBiodiversityScore(vulnerabilities);

            // 7. Save report
            FarmHealthReport report = new FarmHealthReport(scanId, healthScore, bioScore, allAnalysis.toString());
            int reportId = saveReport(report);
            report.setId(reportId);

            // 8. Save vulnerabilities
            for (Vulnerability v : vulnerabilities) {
                v.setReportId(reportId);
                saveVulnerability(v);
            }
            report.setVulnerabilities(vulnerabilities);

            // 9. Generate and save prevention plans for significant vulnerabilities
            List<PreventionPlan> plans = new ArrayList<>();
            for (Vulnerability v : vulnerabilities) {
                if (v.getSeverity() == Severity.CRITICAL || v.getSeverity() == Severity.MEDIUM) {
                    PreventionPlan plan = generateAndSavePlan(reportId, v, scan.getCropType());
                    if (plan != null) {
                        plans.add(plan);
                    }
                }
            }
            report.setPreventionPlans(plans);

            // 10. Mark as completed
            scanService.updateStatus(scanId, ScanStatus.COMPLETED);
            report.setCropType(scan.getCropType());
            report.setGrowthStage(scan.getGrowthStage());

            System.out.println("[HealthScan] Scan #" + scanId + " completed. Score: " + healthScore +
                    "/100, " + vulnerabilities.size() + " vulnerabilities, " + plans.size() + " plans.");

            return report;

        } catch (Exception e) {
            LogUtils.error(getClass(), "Error processing health scan #" + scanId, e);
            scanService.updateStatus(scanId, ScanStatus.FAILED);
            return null;
        }
    }

    /**
     * Loads a full report by scan ID (for display after processing).
     */
    public FarmHealthReport getReportByScan(int scanId) {
        String sql = "SELECT * FROM farm_health_report WHERE scan_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, scanId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                FarmHealthReport report = new FarmHealthReport();
                report.setId(rs.getInt("id"));
                report.setScanId(rs.getInt("scan_id"));
                report.setHealthScore(rs.getInt("health_score"));
                report.setBiodiversityScore(rs.getInt("biodiversity_score"));
                report.setLlavaAnalysis(rs.getString("llava_analysis"));
                report.setGeneratedAt(rs.getTimestamp("generated_at"));

                // Load scan metadata
                FarmHealthScan scan = scanService.getById(scanId);
                if (scan != null) {
                    report.setCropType(scan.getCropType());
                    report.setGrowthStage(scan.getGrowthStage());
                }

                // Load vulnerabilities
                report.setVulnerabilities(loadVulnerabilities(report.getId()));

                // Load prevention plans
                report.setPreventionPlans(planService.getByReport(report.getId()));

                return report;
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error loading report for scan #" + scanId, e);
        }
        return null;
    }

    // ── Private helpers ──

    private String buildCropContext(FarmHealthScan scan) {
        StringBuilder ctx = new StringBuilder();
        ctx.append(scan.getCropType()).append(" au stade ").append(scan.getGrowthStage());
        if (scan.getPlantingDate() != null) {
            ctx.append(", planté le ").append(scan.getPlantingDate());
        }
        if (scan.getLatitude() != null && scan.getLongitude() != null) {
            ctx.append(". Localisation GPS : ")
                    .append(String.format("%.4f, %.4f", scan.getLatitude(), scan.getLongitude()));
        }
        if (scan.getConcerns() != null && !scan.getConcerns().isEmpty()) {
            ctx.append(". Préoccupations : ").append(scan.getConcerns());
        }
        return ctx.toString();
    }

    private void uploadPhotos(FarmHealthScan scan, java.util.Map<String, File> localPhotos) {
        if (localPhotos == null)
            return;
        for (var entry : localPhotos.entrySet()) {
            try {
                String url = ImgBBService.uploadImage(entry.getValue());
                if (url != null) {
                    switch (entry.getKey()) {
                        case "crops":
                            scan.setPhotoCrops(url);
                            break;
                        case "soil":
                            scan.setPhotoSoil(url);
                            break;
                        case "edges":
                            scan.setPhotoEdges(url);
                            break;
                        case "insects":
                            scan.setPhotoInsects(url);
                            break;
                        case "spacing":
                            scan.setPhotoSpacing(url);
                            break;
                        case "overview":
                            scan.setPhotoOverview(url);
                            break;
                    }
                }
            } catch (Exception e) {
                LogUtils.error(getClass(), "Error uploading " + entry.getKey() + " photo", e);
            }
        }
        scanService.updatePhotos(scan);
    }

    private int saveReport(FarmHealthReport report) {
        String sql = "INSERT INTO farm_health_report (scan_id, health_score, biodiversity_score, llava_analysis) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, report.getScanId());
            pst.setInt(2, report.getHealthScore());
            pst.setInt(3, report.getBiodiversityScore());
            pst.setString(4, report.getLlavaAnalysis());
            pst.executeUpdate();
            ResultSet keys = pst.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error saving report", e);
        }
        return -1;
    }

    private void saveVulnerability(Vulnerability vuln) {
        String sql = "INSERT INTO vulnerability (report_id, type, severity, threat, description, " +
                "risk_score, timeframe_days, estimated_yield_loss_percent, estimated_cost_if_occurs) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, vuln.getReportId());
            pst.setString(2, vuln.getType().name());
            pst.setString(3, vuln.getSeverity().name());
            pst.setString(4, vuln.getThreat());
            pst.setString(5, vuln.getDescription());
            pst.setFloat(6, vuln.getRiskScore());
            pst.setInt(7, vuln.getTimeframeDays());
            pst.setInt(8, vuln.getEstimatedYieldLossPercent());
            pst.setFloat(9, vuln.getEstimatedCostIfOccurs());
            pst.executeUpdate();
            ResultSet keys = pst.getGeneratedKeys();
            if (keys.next()) {
                vuln.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error saving vulnerability", e);
        }
    }

    private List<Vulnerability> loadVulnerabilities(int reportId) {
        List<Vulnerability> vulns = new ArrayList<>();
        String sql = "SELECT * FROM vulnerability WHERE report_id = ? ORDER BY risk_score DESC";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, reportId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Vulnerability v = new Vulnerability();
                v.setId(rs.getInt("id"));
                v.setReportId(rs.getInt("report_id"));
                try {
                    v.setType(VulnerabilityType.valueOf(rs.getString("type")));
                } catch (Exception e) {
                    v.setType(VulnerabilityType.DISEASE_RISK);
                }
                v.setSeverity(Severity.fromString(rs.getString("severity")));
                v.setThreat(rs.getString("threat"));
                v.setDescription(rs.getString("description"));
                v.setRiskScore(rs.getFloat("risk_score"));
                v.setTimeframeDays(rs.getInt("timeframe_days"));
                v.setEstimatedYieldLossPercent(rs.getInt("estimated_yield_loss_percent"));
                v.setEstimatedCostIfOccurs(rs.getFloat("estimated_cost_if_occurs"));
                vulns.add(v);
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error loading vulnerabilities", e);
        }
        return vulns;
    }

    private PreventionPlan generateAndSavePlan(int reportId, Vulnerability vuln, String cropType) {
        try {
            // Generate tasks via Groq
            String planResult = groqService.generateHealthPreventionPlan(
                    vuln.getThreat() + " - " + vuln.getDescription(), cropType);

            if (planResult == null || planResult.startsWith("ERREUR"))
                return null;

            // Create the plan
            PreventionPlan plan = new PreventionPlan(reportId, vuln.getId(),
                    "Prévention : " + vuln.getThreat(), vuln.getDescription());
            plan.setTimelineDays(14);
            plan.setImpactLevel(vuln.getSeverity() == Severity.CRITICAL ? "HIGH" : "MEDIUM");
            plan.setExpectedOutcome("Réduction du risque de " + vuln.getThreat());
            plan.setSteps(planResult);

            int planId = planService.save(plan);
            plan.setId(planId);

            // Parse tasks from the JOUR|DESCRIPTION format
            List<PreventionTask> tasks = parseTasks(planResult, planId);
            taskService.saveAll(tasks, planId);
            plan.setTasks(tasks);
            plan.setTotalTasks(tasks.size());

            return plan;

        } catch (Exception e) {
            LogUtils.error(getClass(), "Error generating prevention plan for " + vuln.getThreat(), e);
            return null;
        }
    }

    /**
     * Parses tasks from the Groq response in JOUR|DESCRIPTION format.
     * Same pattern as the treatment plan task parsing.
     */
    private List<PreventionTask> parseTasks(String planResult, int planId) {
        List<PreventionTask> tasks = new ArrayList<>();
        String[] lines = planResult.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;
            String[] parts = line.split("\\|", 2);
            if (parts.length == 2) {
                try {
                    int dayOffset = Integer.parseInt(parts[0].trim());
                    String description = parts[1].trim();
                    tasks.add(new PreventionTask(planId, dayOffset, description));
                } catch (NumberFormatException ignored) {
                    // Skip malformed lines
                }
            }
        }
        return tasks;
    }
}
