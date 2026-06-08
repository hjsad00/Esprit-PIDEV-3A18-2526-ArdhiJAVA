package tn.neuron.ardhi.services.UserAndDiag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

/**
 * Service for country calling codes with auto-detection based on IP
 * geolocation.
 */
public class CountryCodeService {

    public static class CountryCode implements Comparable<CountryCode> {
        private final String name;
        private final String dialCode;
        private final String isoCode;

        public CountryCode(String name, String dialCode, String isoCode) {
            this.name = name;
            this.dialCode = dialCode;
            this.isoCode = isoCode;
        }

        public String getName() {
            return name;
        }

        public String getDialCode() {
            return dialCode;
        }

        public String getIsoCode() {
            return isoCode;
        }

        @Override
        public String toString() {
            return dialCode + " " + name;
        }

        @Override
        public int compareTo(CountryCode o) {
            return this.name.compareTo(o.name);
        }
    }

    private static final List<CountryCode> ALL_CODES = new ArrayList<>();

    static {
        // North Africa & Middle East
        ALL_CODES.add(new CountryCode("Tunisia", "+216", "TN"));
        ALL_CODES.add(new CountryCode("Algeria", "+213", "DZ"));
        ALL_CODES.add(new CountryCode("Morocco", "+212", "MA"));
        ALL_CODES.add(new CountryCode("Libya", "+218", "LY"));
        ALL_CODES.add(new CountryCode("Egypt", "+20", "EG"));
        ALL_CODES.add(new CountryCode("Saudi Arabia", "+966", "SA"));
        ALL_CODES.add(new CountryCode("UAE", "+971", "AE"));
        ALL_CODES.add(new CountryCode("Qatar", "+974", "QA"));
        ALL_CODES.add(new CountryCode("Kuwait", "+965", "KW"));
        ALL_CODES.add(new CountryCode("Bahrain", "+973", "BH"));
        ALL_CODES.add(new CountryCode("Oman", "+968", "OM"));
        ALL_CODES.add(new CountryCode("Jordan", "+962", "JO"));
        ALL_CODES.add(new CountryCode("Lebanon", "+961", "LB"));
        ALL_CODES.add(new CountryCode("Iraq", "+964", "IQ"));
        ALL_CODES.add(new CountryCode("Syria", "+963", "SY"));
        ALL_CODES.add(new CountryCode("Palestine", "+970", "PS"));
        ALL_CODES.add(new CountryCode("Yemen", "+967", "YE"));

        // Europe
        ALL_CODES.add(new CountryCode("France", "+33", "FR"));
        ALL_CODES.add(new CountryCode("Germany", "+49", "DE"));
        ALL_CODES.add(new CountryCode("United Kingdom", "+44", "GB"));
        ALL_CODES.add(new CountryCode("Italy", "+39", "IT"));
        ALL_CODES.add(new CountryCode("Spain", "+34", "ES"));
        ALL_CODES.add(new CountryCode("Belgium", "+32", "BE"));
        ALL_CODES.add(new CountryCode("Netherlands", "+31", "NL"));
        ALL_CODES.add(new CountryCode("Switzerland", "+41", "CH"));
        ALL_CODES.add(new CountryCode("Portugal", "+351", "PT"));
        ALL_CODES.add(new CountryCode("Sweden", "+46", "SE"));
        ALL_CODES.add(new CountryCode("Norway", "+47", "NO"));
        ALL_CODES.add(new CountryCode("Denmark", "+45", "DK"));
        ALL_CODES.add(new CountryCode("Austria", "+43", "AT"));
        ALL_CODES.add(new CountryCode("Poland", "+48", "PL"));
        ALL_CODES.add(new CountryCode("Greece", "+30", "GR"));
        ALL_CODES.add(new CountryCode("Turkey", "+90", "TR"));
        ALL_CODES.add(new CountryCode("Russia", "+7", "RU"));

        // Americas
        ALL_CODES.add(new CountryCode("United States", "+1", "US"));
        ALL_CODES.add(new CountryCode("Canada", "+1", "CA"));
        ALL_CODES.add(new CountryCode("Brazil", "+55", "BR"));
        ALL_CODES.add(new CountryCode("Mexico", "+52", "MX"));
        ALL_CODES.add(new CountryCode("Argentina", "+54", "AR"));

        // Sub-Saharan Africa
        ALL_CODES.add(new CountryCode("Nigeria", "+234", "NG"));
        ALL_CODES.add(new CountryCode("South Africa", "+27", "ZA"));
        ALL_CODES.add(new CountryCode("Kenya", "+254", "KE"));
        ALL_CODES.add(new CountryCode("Senegal", "+221", "SN"));
        ALL_CODES.add(new CountryCode("Cameroon", "+237", "CM"));
        ALL_CODES.add(new CountryCode("Ivory Coast", "+225", "CI"));
        ALL_CODES.add(new CountryCode("Ghana", "+233", "GH"));
        ALL_CODES.add(new CountryCode("Ethiopia", "+251", "ET"));
        ALL_CODES.add(new CountryCode("Tanzania", "+255", "TZ"));
        ALL_CODES.add(new CountryCode("Mauritania", "+222", "MR"));
        ALL_CODES.add(new CountryCode("Sudan", "+249", "SD"));
        ALL_CODES.add(new CountryCode("Mali", "+223", "ML"));
        ALL_CODES.add(new CountryCode("Niger", "+227", "NE"));
        ALL_CODES.add(new CountryCode("Chad", "+235", "TD"));

        // Asia
        ALL_CODES.add(new CountryCode("China", "+86", "CN"));
        ALL_CODES.add(new CountryCode("Japan", "+81", "JP"));
        ALL_CODES.add(new CountryCode("India", "+91", "IN"));
        ALL_CODES.add(new CountryCode("South Korea", "+82", "KR"));
        ALL_CODES.add(new CountryCode("Australia", "+61", "AU"));

        Collections.sort(ALL_CODES);
    }

    /**
     * Returns all country codes, sorted alphabetically.
     */
    public List<CountryCode> getAllCountryCodes() {
        return Collections.unmodifiableList(ALL_CODES);
    }

    /**
     * Detects the user's country code based on IP geolocation.
     * Returns the matching CountryCode or defaults to Tunisia.
     */
    public CountryCode detectCountryCode() {
        try {
            LocationService locationService = new LocationService();
            LocationService.LocationData loc = locationService.detectLocation();
            if (loc != null && loc.label != null) {
                // The label format is "City, Region, Country"
                String country = loc.label;
                if (country.contains(",")) {
                    String[] parts = country.split(",");
                    country = parts[parts.length - 1].trim();
                }
                // Search by country name (case-insensitive partial match)
                for (CountryCode cc : ALL_CODES) {
                    if (cc.getName().equalsIgnoreCase(country)) {
                        return cc;
                    }
                }
                // Try partial match
                for (CountryCode cc : ALL_CODES) {
                    if (cc.getName().toLowerCase().contains(country.toLowerCase())
                            || country.toLowerCase().contains(cc.getName().toLowerCase())) {
                        return cc;
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.error(CountryCodeService.class, "Failed to detect country code", e);
        }
        // Default to Tunisia
        return ALL_CODES.stream()
                .filter(cc -> "TN".equals(cc.getIsoCode()))
                .findFirst()
                .orElse(ALL_CODES.get(0));
    }
}
