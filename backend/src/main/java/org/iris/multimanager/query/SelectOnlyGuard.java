package org.iris.multimanager.query;

import jakarta.ws.rs.BadRequestException;
import java.util.regex.Pattern;

public final class SelectOnlyGuard {
    private static final Pattern COMMENTS=Pattern.compile("(?s)/\\*.*?\\*/|--[^\\r\\n]*");
    private SelectOnlyGuard() {}
    public static String validate(String sql){
        if(sql==null||sql.length()>10000)throw new BadRequestException("Query is required and limited to 10,000 characters");
        String normalized=COMMENTS.matcher(sql).replaceAll(" ").trim();
        if(normalized.endsWith(";")) normalized=normalized.substring(0,normalized.length()-1).trim();
        if(normalized.isBlank()||normalized.contains(";")||!normalized.matches("(?is)^select\\b.*"))throw new BadRequestException("Only one SELECT statement is allowed");
        if(Pattern.compile("(?is)\\b(insert|update|delete|merge|drop|alter|create|truncate|grant|revoke|call|do|into)\\b").matcher(normalized).find())throw new BadRequestException("Only read-only SELECT statements are allowed");
        return normalized;
    }
}
