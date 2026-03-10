package org.loger.penalty;

public class PlaceholderProcessor {
    private static final String PH_BUFFER = "%buffer%";
    private static final String PH_PLAYER = "%player%";
    private static final String PH_PROBABILITY = "%probability%";
    private static final String PH_VL = "%vl%";

    public String process(String template, PenaltyContext context) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        if (context == null) {
            return template;
        }
        String result = template.replace(PH_PLAYER, context.getPlayerName());
        String result2 = result.replace(PH_VL, String.valueOf(context.getViolationLevel()));
        int probPercent = (int) (context.getProbability() * 100.0d);
        return result2.replace(PH_PROBABILITY, String.valueOf(probPercent)).replace(PH_BUFFER, formatDouble(context.getBuffer()));
    }

    private String formatDouble(double value) {
        return String.format("%.2f", Double.valueOf(value));
    }

    public boolean hasPlaceholders(String template) {
        if (template == null || template.isEmpty()) {
            return false;
        }
        return template.contains(PH_PLAYER) || template.contains(PH_VL) || template.contains(PH_PROBABILITY) || template.contains(PH_BUFFER);
    }
}
